package com.github.pigeon.api;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.github.pigeon.api.model.EventSendResult;
import com.github.pigeon.api.model.EventSubscriberConfig;
import com.github.pigeon.api.model.EventWrapper;
import com.github.pigeon.api.repository.EventRepository;
import com.github.pigeon.api.repository.SubscriberConfigRepository;
import com.github.pigeon.api.repository.impl.PigeonConfigProperties;
import com.github.pigeon.api.utils.PigeonUtils;
import com.github.pigeon.api.utils.executors.CountDownExecutor;
import com.github.pigeon.api.utils.executors.MDCThreadPoolExecutor;
import com.github.pigeon.api.utils.executors.MutexTaskExecutor;

/**
 * 事件发布执行器
 *
 * @author liuhaoyong time : 2015年11月3日 下午5:22:08
 */
public class EventPublishExecutor {

    private static final Logger        logger             = LoggerFactory.getLogger(EventPublishExecutor.class);

    PublishExceptionHandler            publishExceptionHandler;

    private SubscriberConfigRepository eventSubseriberConfigFactory;

    private MDCThreadPoolExecutor      sendExecutor;

    private EventRepository            eventRepository;

    private PigeonConfigProperties     publisherConfigParams;

    private StringRedisTemplate        redisTemplate;

    private ScheduledExecutorService   processingExecutor = null;

    private static volatile boolean    isRun              = false;

    public EventPublishExecutor(SubscriberConfigRepository eventSubseriberConfigFactory,
                                EventRepository eventRepository, PigeonConfigProperties publisherConfigParams,
                                StringRedisTemplate redisTemplate, MDCThreadPoolExecutor sendExecutor) {
        this.publisherConfigParams = publisherConfigParams;
        this.eventSubseriberConfigFactory = eventSubseriberConfigFactory;
        this.eventRepository = eventRepository;
        this.redisTemplate = redisTemplate;
        this.sendExecutor = sendExecutor;

    }

    /**
     * 定时从normal queue中获取事件
     */
    @PostConstruct
    public void init() {
        processingExecutor = Executors.newScheduledThreadPool(1);
        processingExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                logger.info("任务启动，处理长期在发送队列中的事件");
                MutexTaskExecutor
                        .newMutexTaskExecutor(publisherConfigParams.getNormalQueueTaskLockName(), redisTemplate, () -> {
                            recover();
                        }).start(false);
            }
        }, 2, 5, TimeUnit.MINUTES);

    }

    /**
     * 发送事件 同步处理 主要处理从队列中拿出来时间
     *
     * @param eventKey
     * @param eventJson
     */
    public void sendEvent(String eventKey, String eventJson) {
        try {
            if (StringUtils.isBlank(eventJson)) {
                logger.warn("eventJson is blank, key:{}", eventKey);
                return;
            }
            EventWrapper event = PigeonUtils.unmarshall(eventJson);
            doSendEvent(eventSubseriberConfigFactory.getEventSubscriberConfig(event.getConfigId()), event);
        } catch (Exception e) {
            logger.error("event unmarshall error,event={}", eventKey, e);
            publishExceptionHandler.handleException(eventKey, eventJson);
        }
    }

    /**
     * 发送事件 异步处理 主要处理正常的publish
     *
     * @param config
     * @param event
     */
    public void sendEvent(final EventSubscriberConfig config, final EventWrapper event) {
        sendExecutor.execute(new Runnable() {
            public void run() {
                doSendEvent(config, event);
            }
        });
    }

    /**
     * @param config
     * @param event
     */
    private void doSendEvent(final EventSubscriberConfig config, final EventWrapper event) {
        EventSendResult result = null;
        boolean isPersist = config.isPersist();
        try {
            result = config.getEventSender().send(event, config);
            if (isPersist) {
                if (result.isSuccess()) {
                    if (event.isRetry()) {
                        eventRepository.delExceptionalEvent(event);
                    } else {
                        eventRepository.delEvent(event);
                    }
                }
            }
        } catch (Exception t) {
            logger.error("Exception to send eventKey:{}", event.getEventKey(), t);
            result = EventSendResult.getFailResult(t.getMessage(), true);
        }

        if (result == null || !result.isSuccess()) {
            logger.info("Failed to send event:{}. result:{}", event, result);
            if (isPersist) { //如果不需要丢入队列就意味着不需要维护重试等机制
                publishExceptionHandler.handleException(config, event, result);
            }
        }
    }

    /**
     * 针对长期在持久化队列里的任务，执行恢复重试， 防止系统重启等原因导致的队列中的任务没有被处理掉
     */
    public void recover() {
        long time = System.currentTimeMillis();
        long min = 0;
        long max = DateUtils.addMinutes(new Date(), -5).getTime(); //取五分钟以前的数据
        int count = publisherConfigParams.getRetryFetchCount();
        int offset = 0;
        int iterCount = 0;//循环次数
        try {
            if (isRun) {
                logger.info("事件发送任务已经在运行");
                return;
            }
            isRun = true;
            while (true) {
                if (iterCount >= 200) {//一次任务最多循环200次
                    break;
                }
                iterCount++;

                Map<String, String> resultMap;
                try {
                    long total = eventRepository.getEventCount(min, max);
                    logger.info(
                            "长期在处理中的任务信息， min:[{}],max:[{}],offset:[{}],count:[{}]-->total:[{}],iterCount:[{}]",
                            min, max, offset, count, total, iterCount);
                    resultMap = eventRepository.extractEvent(min, max, offset, count);
                } catch (Exception e) {
                    logger.error("从正常队列中获取待重试的事件发生异常", e);
                    break;
                }

                //如果获取的对象为空，跳出
                if (resultMap == null || resultMap.isEmpty()) {
                    break;
                }

                int mapSize = resultMap.size();
                final int countDownSize = Integer.min(mapSize, 10);
                CountDownExecutor countDownExecutor = CountDownExecutor.newCountDown(countDownSize,
                        "pigeon-normalQueueTask");
                int num = 0;
                for (Map.Entry<String, String> item : resultMap.entrySet()) {
                    try {
                        for (int i = 0; i < countDownSize; i++) {
                            countDownExecutor.addRunner(() -> {
                                sendEvent(item.getKey(), item.getValue());
                            });
                        }
                        num++;
                        if (num == countDownSize) {
                            countDownExecutor.start();
                            num = 0;
                        }
                    } catch (Exception e) {
                        logger.error("[PigeonEvent][EVENT_JOB_Normal]数据内容解析或者SendEvent出错,event:" + item, e);
                    }
                }

                if (resultMap.size() < count) {
                    break;
                }
            }

        } finally {
            isRun = false;
        }
    }

    /**
     * spring的destory方法
     */
    @PreDestroy
    public void destory() {
        if (processingExecutor != null) {
            processingExecutor.shutdown();
        }
    }

}
