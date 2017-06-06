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
import com.github.pigeon.api.utils.executors.MutexTaskExecutor;

/**
 * 功能描述：异常处理器
 *
 * @author liuhaoyong time : 2014-1-16 下午6:39:17
 */
public class PublishExceptionHandler {

    private static final Logger      logger   = LoggerFactory.getLogger(PublishExceptionHandler.class);

    private EventRepository          eventRepository;

    /**
     * 时间发送执行器
     */
    private EventPublishExecutor     eventSendExecutor;

    private PigeonConfigProperties   publisherConfigParams;

    private StringRedisTemplate      redisTemplate;

    private ScheduledExecutorService executor = null;

    public PublishExceptionHandler(EventRepository eventRepository, EventPublishExecutor eventSendExecutor,
                                   PigeonConfigProperties publisherConfigParams, StringRedisTemplate redisTemplate) {
        this.eventRepository = eventRepository;
        this.eventSendExecutor = eventSendExecutor;
        this.publisherConfigParams = publisherConfigParams;
        this.redisTemplate = redisTemplate;
    }

    /**
     * spring的init-method方法，缓存初始化
     */
    @PostConstruct
    public void init() {
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                logger.info("异常事件重试任务启动");
                MutexTaskExecutor.execute(60 * 60, publisherConfigParams.getExceptionQueueTaskLockName(), redisTemplate,
                        false, () -> {
                            handleExceptionQueue();
                        });
            }
        }, 1, 1, TimeUnit.MINUTES);

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
            event.setContent(eventString);
            eventRepository.saveFailedLog(buildFailedEventLog(event, null));
            eventRepository.delExceptionalEvent(event);
            eventRepository.delEvent(event);
        } catch (Exception e) {
            logger.error("保存事件错误日志失败", e);
        }
    }

    /**
     * 判断是否可以被重试
     * <p/>
     * 如果最大重试次数大于当前事件的已重试次数，且接收端返回能够重试则为可重试
     *
     * @param result
     * @return
     */
    private boolean canRetryCheck(EventSubscriberConfig eventConfig, EventWrapper event, EventSendResult result) {
        return (result != null && result.isCanRetry() && eventConfig != null
                && eventConfig.getMaxRetryTimes() > event.getSentTimes());
    }

    /**
     * 事件发送异常处理 如果当前事件可以重试 ，则将事件持久化到重试队列中， 并移除原有队列中的事件， 等待后续重试
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
                Date retryTime = DateUtil.addMinutes(new Date(), Long.valueOf(intervalInMinitues).intValue());
                eventRepository.persistExceptionalEvent(event, retryTime.getTime());
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
    public void handleExceptionQueue() {
        long time = DateUtil.getCurrentTimeMillis();
        try {
            if (isRun) {
                logger.info("异常重试任务已经在运行");
                return;
            }
            isRun = true;
            long min;
            Date curDate = new Date();

            /**
             * 如果第一次执行，捞取的事件区间为： [昨天, 当前时间], 防止系统长时间停机导致大量事件漏发 
             * 如果不是第一次执行，捞取的事件区间为：[上次执行时间,当前事件]
             */
            if (lastExecuteTime <= 0l) {
                min = DateUtil.addDays(curDate, -1).getTime();
            } else {
                min = lastExecuteTime;
            }
            long max = DateUtil.getCurrentTimeMillis();
            int loopCount=0;
            while (true) {
                if(loopCount++>200)
                {
                    logger.warn("异常重试任务循环执行200次,事件仍然未发送完成,请检查程序是否异常");
                    break;
                }
                Map<String, String> resultMap = new HashMap<>();
                try {
                    //每次从redis内取出retryFetchCount条事件,防止内存溢出,循环获取,直到该区间内事件取完
                    resultMap = eventRepository.extractExceptionalEvent(min, max, 0, publisherConfigParams.getRetryFetchCount());
                    if (resultMap == null || resultMap.isEmpty()) {
                        lastExecuteTime = max;
                        break;
                    }
                } catch (Exception e) {
                    logger.error("从异常队列中获取待重试的事件发生异常", e);
                    break;
                }

                logger.info("从异常队列中获取到 [{}]条待重试记录,开始重试", resultMap.size());
                for (Map.Entry<String, String> item : resultMap.entrySet()) {
                    try {
                        eventSendExecutor.sendEvent(item.getKey(), item.getValue());
                    } catch (Throwable e) {
                        logger.error("异常事件重试发送失败", e);
                    }
                }
                
                lastExecuteTime = max;
                if (resultMap.size() < publisherConfigParams.getRetryFetchCount()) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            isRun = false;
            logger.info("异常重试任务执行结束,耗时=[{}]", (DateUtil.getCurrentTimeMillis() - time));
        }

    }

    /**
     * spring的destory方法
     */
    @PreDestroy
    public void destory() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
