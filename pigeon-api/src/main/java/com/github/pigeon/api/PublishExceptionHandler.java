package com.github.pigeon.api;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.github.pigeon.api.model.EventSendResult;
import com.github.pigeon.api.model.EventSubscriberConfig;
import com.github.pigeon.api.model.EventWrapper;
import com.github.pigeon.api.model.FailedEventLog;
import com.github.pigeon.api.repository.EventRepository;
import com.github.pigeon.api.repository.impl.PigeonConfigProperties;
import com.github.pigeon.api.utils.DateUtil;
import com.github.pigeon.api.utils.PigeonUtils;
import com.github.pigeon.api.utils.executors.CountDownExecutor;
import com.github.pigeon.api.utils.executors.MutexTaskExecutor;

/**
 * 功能描述：异常处理器
 *
 * @author liuhaoyong time : 2014-1-16 下午6:39:17
 */
public class PublishExceptionHandler {

    private static final Logger      logger     = LoggerFactory.getLogger(PublishExceptionHandler.class);

    private EventRepository          eventRepository;

    /**
     * 时间发送执行器
     */
    private EventPublishExecutor     eventSendExecutor;

    private PigeonConfigProperties    publisherConfigParams;

    private StringRedisTemplate      enhancedCodisClient;

    private ScheduledExecutorService retryTimer = null;

    public PublishExceptionHandler(EventRepository eventRepository, EventPublishExecutor eventSendExecutor,
                                   PigeonConfigProperties publisherConfigParams, StringRedisTemplate redisTemplate) {
        this.eventRepository = eventRepository;
        this.eventSendExecutor = eventSendExecutor;
        this.publisherConfigParams = publisherConfigParams;
        this.enhancedCodisClient = redisTemplate;
        this.init();
    }

    /**
     * spring的init-method方法，缓存初始化
     */
    @PostConstruct
    public void init() {
        retryTimer = Executors.newScheduledThreadPool(1);
        retryTimer.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                logger.info("[PigeonEvent][EVENT_JOB_Exception] start............");
                MutexTaskExecutor.newMutexTaskExecutor(publisherConfigParams.getRetryQueueTaskLockName(),
                        enhancedCodisClient, () -> {
                            handleRetryQueue();
                        }).start(false);
            }
        }, publisherConfigParams.getRetryTimerInMinitues() * 5, publisherConfigParams.getRetryTimerInMinitues() * 5,
                TimeUnit.MINUTES);

    }

    /**
     * 如果事件在json数据转化的时候就失败的处理
     *
     * @param eventKey
     * @param eventString
     */
    public void handleException(String eventKey, String eventString) {
        try {
            EventWrapper event = new EventWrapper();
            event.setEventKey(eventKey);
            event.setEvent(eventString);
            eventRepository.saveFailedLog(buildFailedEventLog(event, null));
            eventRepository.delExceptionalEvent(event);
            eventRepository.delEvent(event);
        } catch (Exception e) {
            logger.error("保存事件错误日志失败", e);
        }
    }

    /**
     * 判断时间是否可以被重试
     * <p/>
     * 如果最大重试次数大于当前事件的已重试次数，且能够重试的，则将事件再放入redis中，并移除原有队列中的事件删除， 等待后续重试
     * 否则移动到mysql数据库中, 等待报警后人工处理
     *
     * @param result
     * @return
     */
    private boolean canRetryCheck(EventSubscriberConfig eventConfig, EventWrapper event, EventSendResult result) {
        return (result != null && result.isCanRetry() && eventConfig != null
                && eventConfig.getMaxRetryTimes() > event.getSentTimes());
    }

    /**
     * 事件发送异常处理
     *
     * @param eventConfig
     * @param event
     * @param result
     */
    public void handleException(EventSubscriberConfig eventConfig, EventWrapper event, EventSendResult result) {
        try {
            if (canRetryCheck(eventConfig, event, result)) {
                event.setSentTimes(event.getSentTimes() + 1);
                long intervalInMinitues = event.getSentTimes() * publisherConfigParams.getRetryIntervalInMinitues();
                Date retryTime = DateUtil.addMinutes(new Date(),
                        Long.valueOf(intervalInMinitues).intValue());
                eventRepository.saveExceptionalEvent(event, retryTime.getTime());
                logger.info("[PigeonEvent][canRetry]event:{}", event);
            } else {
                //保存到服务器端
                eventRepository.saveFailedLog(buildFailedEventLog(event, result));
                eventRepository.delExceptionalEvent(event);
            }
            //异常处理是，总是要删除normal队列中的event,可能会重复操作,不过对功能没有影响
            eventRepository.delEvent(event);
        } catch (Throwable e) {
            logger.error("[PigeonEvent]Failed to handle eventSend Exception. event:{}", event, e);
        }
    }

    /**
     * @param eventContent
     * @param result
     * @return
     */
    private FailedEventLog buildFailedEventLog(EventWrapper eventContent, EventSendResult result) {
        FailedEventLog eventLog = new FailedEventLog();
        eventLog.setConfigId(eventContent.getConfigId());
        eventLog.setEventContent(StringUtils.left(PigeonUtils.marshall(eventContent), 4000));
        eventLog.setEventKey(eventContent.getEventKey());
        eventLog.setEventType(eventContent.getEventType());
        eventLog.setRetriedTimes(eventContent.getSentTimes());
        eventLog.setTargetAddress(eventContent.getTargetAddress());
        eventLog.setSendResult(StringUtils.left((result != null ? PigeonUtils.marshall(result) : ""), 400));
        return eventLog;
    }

    /**
     * 上一次执行时间
     */
    private static volatile long    lastExecuteTime = 0l;
    private static volatile boolean isRun           = false;

    /**
     *
     */
    public void handleRetryQueue() {
        long time = DateUtil.getCurrentTimeMillis();
        logger.info("[PigeonEvent][EVENT_JOB_Exception]Exception Queue Job Start, isRun:{}............", isRun);
        try {
            if (isRun) {
                logger.info("[PigeonEvent][EVENT_JOB_Exception] is running,so stop,isRun:{}............", isRun);
                return;
            }
            isRun = true;
            long min;
            Date curDate = new Date();

            /**
             * 如果第一次执行，捞取的事件区间为： [昨天, 上次执行时间+RetryTimerInMinitues],
             * 防止系统长时间停机导致大量事件漏发 如果不是第一次执行，捞取的事件区间为： [上次执行时间,
             * 上次执行时间+RetryTimerInMinitues]
             */
            if (lastExecuteTime <= 0l) {
                min = DateUtil.addDays(curDate, -1).getTime();
            } else {
                min = lastExecuteTime;
            }
            int count = publisherConfigParams.getRetryFetchCount();
            long max = DateUtil.getCurrentTimeMillis();
            int offset = 0;
            // 循环获取该区间内的待重试事件，每次获取eventFatchCount条，直到获取不到
            int iterCount = 0;// 循环次数
            try {
                logger.info("[PigeonEvent][EVENT_JOB_Exception] queueTotalSize:{}............",
                        eventRepository.getExceptionEventCount());
            } catch (Exception e) {
                logger.error("[PigeonEvent][EVENT_JOB_Exception]" + e.getMessage(), e);
            }
            while (true) {
                if (iterCount >= 200) {// 一次任务最多循环200次
                    break;
                }
                iterCount++;

                Map<String, String> resultMap = new HashMap<>();
                try {
                    long total = eventRepository.getExceptionEventCount(min, max);
                    logger.info(
                            "[PigeonEvent][EVENT_JOB_Exception] min:[{}],max:[{}],offset:[{}],count:[{}]-->total:[{}],iterCount:[{}]",
                            min, max, offset, count, total, iterCount);
                    resultMap = eventRepository.extractExceptionalEvent(min, max, offset, count);
                } catch (Exception e) {
                    logger.error("[PigeonEvent][EVENT_JOB_Exception]从redis中获取待重试的事件发生异常", e);
                    break;
                }

                lastExecuteTime = max;

                // 如果获取的对象为空，跳出
                if (resultMap == null || resultMap.isEmpty()) {
                    break;
                }

                int mapSize = resultMap.size();
                final int countDownSize = Integer.min(mapSize, 10);
                logger.info("[PigeonEvent][EVENT_JOB_Exception] mapSize:[{}],countDownSize:[{}]............", mapSize,
                        countDownSize);
                CountDownExecutor countDownExecutor = CountDownExecutor.newCountDown(countDownSize,
                        "pigeon-retryQueueTask");
                int num = 0;
                for (Map.Entry<String, String> item : resultMap.entrySet()) {
                    try {
                        countDownExecutor.addRunner(() -> {
                            logger.info(
                                    "[PigeonEvent][EVENT_JOB_Exception]TaskEventRetrySend,eventkey:{},eventValue:{}",
                                    item.getKey(), item.getValue());
                            eventSendExecutor.sendEvent(item.getKey(), item.getValue());
                            logger.info(
                                    "[PigeonEvent][EVENT_JOB_Exception]TaskEventRetrySend finished,eventkey:{},eventValue:{}",
                                    item.getKey(), item.getValue());
                        });
                        num++;
                        if (num == countDownSize) {
                            countDownExecutor.start();
                            num = 0;
                        }
                    } catch (Exception e) {
                        logger.error("[PigeonEvent][EVENT_JOB_Exception]数据内容解析或者SendEvent出错", e);
                    }

                }

                if (resultMap.size() < publisherConfigParams.getRetryFetchCount()) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            isRun = false;
        }
        logger.info("[PigeonEvent][EVENT_JOB_Exception]Exception Queue Job end[{}],isRun:{}............",
                (DateUtil.getCurrentTimeMillis() - time), isRun);
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
}
