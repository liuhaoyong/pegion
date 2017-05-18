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
import com.github.pigeon.api.repository.impl.PublisherConfigParams;
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

    private static final Logger        logger     = LoggerFactory.getLogger(EventPublishExecutor.class);

    PublishExceptionHandler     publishExceptionHandler;

    private SubscriberConfigRepository eventSubseriberConfigFactory;

    private MDCThreadPoolExecutor       pigeonEventSendExecutor;

    private EventRepository             eventRepository;

    private PublisherConfigParams      publisherConfigParams;

    private StringRedisTemplate        redisTemplate;

    private ScheduledExecutorService   retryTimer = null;
    
    private static volatile boolean isRun = false;

    public EventPublishExecutor(SubscriberConfigRepository eventSubseriberConfigFactory,
                                EventRepository eventRepository, PublisherConfigParams publisherConfigParams,
                                StringRedisTemplate redisTemplate,
                                MDCThreadPoolExecutor mdcThreadPoolExecutor) {
        this.publisherConfigParams = publisherConfigParams;
        this.eventSubseriberConfigFactory = eventSubseriberConfigFactory;
        this.eventRepository = eventRepository;
        this.redisTemplate =        redisTemplate;
        this.pigeonEventSendExecutor = mdcThreadPoolExecutor;
           
    }

    /**
     * 定时从normal queue中获取事件
     */
    @PostConstruct
    public void init() {
        retryTimer = Executors.newScheduledThreadPool(1);
        retryTimer.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                logger.info("[PigeonEvent][EVENT_JOB_Normal] start............");
                MutexTaskExecutor
                        .newMutexTaskExecutor(publisherConfigParams.getNormalQueueTaskLockName(), redisTemplate, () -> {
                            handleEventInNormal(-5);
                        }).start(false);
            }
        }, publisherConfigParams.getRetryTimerInMinitues() * 3, publisherConfigParams.getRetryTimerInMinitues() * 4,
                TimeUnit.MINUTES);

    }



    /**
     *
     */
    public void handleEventInNormal(int beforeMinutes) {
        long time = System.currentTimeMillis();
        long min = 0;
        long max = DateUtils.addMinutes(new Date(), beforeMinutes).getTime(); //取五分钟以前的数据
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
                            "[PigeonEvent][EVENT_JOB_Normal] min:[{}],max:[{}],offset:[{}],count:[{}]-->total:[{}],iterCount:[{}]",
                            min, max, offset, count, total, iterCount);
                    resultMap = eventRepository.extractEvent(min, max, offset, count);
                } catch (Exception e) {
                    logger.error("[PigeonEvent][EVENT_JOB_Normal]从redis中获取待重试的事件发生异常", e);
                    break;
                }

                //如果获取的对象为空，跳出
                if (resultMap == null || resultMap.isEmpty()) {
                    break;
                }

                int mapSize = resultMap.size();
                final int countDownSize = Integer.min(mapSize, 10);
                logger.info("[PigeonEvent][EVENT_JOB_Normal] mapSize:[{}],countDownSize:[{}]............",
                        countDownSize, countDownSize);
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
        logger.info("[PigeonEvent][EVENT_JOB_Normal] Queue Job end[{}],isRun:{}............",
                (System.currentTimeMillis() - time), isRun);

    }

    /**
     * spring的destory方法
     */
    @PreDestroy
    public void destory() {
        if (retryTimer != null) {
            retryTimer.shutdown();
        }
    }

    /**
     * 发送事件 同步处理 主要处理从队列中拿出来时间
     *
     * @param eventKey
     * @param eventJson
     */
    public void sendEvent(final String eventKey, final String eventJson) {
        try {
            if (StringUtils.isBlank(eventJson)) {
                logger.warn("[PigeonEvent] eventJson is blank! key:{}", eventKey);
                return;
            }
            EventWrapper event = PigeonUtils.unmarshall(eventJson);
            doSendEvent(eventSubseriberConfigFactory.getEventSubscriberConfig(event.getConfigId()), event);
        } catch (Exception e) {
            logger.error("[PigeonEvent]event unmarshall error:[" + eventKey + "]", e);
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
        pigeonEventSendExecutor.execute(new Runnable() {
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
        boolean needToQueue = config.needToQueue();
        try {
            result = config.getEventSender().send(event, config);
            if (needToQueue) {
                if (result.isSuccess()) {
                    if (event.isRetry()) {
                        eventRepository.delExceptionalEvent(event);
                    } else {
                        eventRepository.delEvent(event);
                    }
                }
            }
        } catch (Exception t) {
            logger.error("[PigeonEvent]Exception to send eventKey:{}", event.getEventKey(), t);
            result = EventSendResult.getFailResult(t.getMessage(), true);
        }
        if (result == null || !result.isSuccess()) {
            logger.warn("[PigeonEvent]Failed to send event:{}. result:{}", event, result);
            if (needToQueue) { //如果不需要丢入队列就意味着不需要维护重试等机制
                publishExceptionHandler.handleException(config, event, result);
            }
        }
    }
}
