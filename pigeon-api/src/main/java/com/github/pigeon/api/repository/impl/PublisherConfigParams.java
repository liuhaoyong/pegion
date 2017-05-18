package com.github.pigeon.api.repository.impl;

import java.net.InetAddress;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.github.pigeon.api.utils.DateUtil;
import com.github.pigeon.api.utils.PigeonUtils;

/**
 * 事件发布器配置参数
 * 
 * @author liuhaoyong time : 2015年11月3日 下午7:12:28
 */
@ConfigurationProperties("pigeon")
public class PublisherConfigParams {

    private static final Logger logger                          = LoggerFactory.getLogger(PublisherConfigParams.class);
    private static final String split_str                       = "_";
    public static final int     defaultMaxLocalQueueSize        = 10000;

    private String              applicationName = "pigeonClient";

    private String              subscribeConfigModuleName       = "event.subscriber.config";
    /**
     * redis异常事件队列
     */
    private String              redisExceptionQueue             = "EXCEPTION_EVENT_QUEUE";

    /**
     * redis异常事件map
     */
    private String              redisExceptionMap               = "EXCEPTION_EVENT_MAP";

    /**
     * redis事件发送的队列
     */
    private String              redisNormalQueue                = "NORMAL_EVENT_QUEUE";

    /**
     * redis事件数据
     */
    private String              redisNormalMap                  = "NORMAL_EVENT_MAP";

    /**
     * 本地队列最大长度
     */
    private int                 maxLocalQueueSize               = defaultMaxLocalQueueSize;

    /**
     * 失败重试抓取数据的间隔执行时间
     */
    private long                retryIntervalInMinitues         = 1;

    /**
     * 重试时每次从redis里获取的事件数量
     */
    private int                 retryFetchCount                 = 50;

    /**
     * 正常抓取事件间隔时间，默认为1分钟
     */
    private long                retryTimerInMinitues            = 1;

    /**
     * 当使用http投递消息时的连接超时时间
     */
    private int                 httpConnectTimeoutInMillisecond = 3000;

    /**
     * 当使用http投递消息时的读超时时间
     */
    private int                 httpSoTimeoutInMillisecond      = 5000;

    private int                 eventSentCorePoolSize           = 5;

    private int                 eventSentMaxPoolSize            = 10;

    private int                 eventPublishcorePoolSize        = 5;

    private int                 getEventPublishMaxPoolSize      = 10;

    @PostConstruct
    private void init() {
        logger.info("[PigeonEvent] applicationName:{}", applicationName);
        if (StringUtils.isBlank(applicationName)) {
            throw new NullPointerException("[PigeonEvent] applicationName is blank....");
        }

    }

    public String getSubscribeConfigModuleName() {
        return subscribeConfigModuleName;
    }

    public int getMaxLocalQueueSize() {
        return maxLocalQueueSize;
    }

    public static int getDefaultmaxlocalqueuesize() {
        return defaultMaxLocalQueueSize;
    }

    public long getRetryIntervalInMinitues() {
        return retryIntervalInMinitues;
    }

    public int getRetryFetchCount() {
        return retryFetchCount;
    }

    public long getRetryTimerInMinitues() {
        return retryTimerInMinitues;
    }

    public int getHttpConnectTimeoutInMillisecond() {
        return httpConnectTimeoutInMillisecond;
    }

    public int getHttpSoTimeoutInMillisecond() {
        return httpSoTimeoutInMillisecond;
    }

    public int getEventSentCorePoolSize() {
        return eventSentCorePoolSize;
    }

    public int getEventSentMaxPoolSize() {
        return eventSentMaxPoolSize;
    }

    public int getEventPublishcorePoolSize() {
        return eventPublishcorePoolSize;
    }

    public int getGetEventPublishMaxPoolSize() {
        return getEventPublishMaxPoolSize;
    }

    public String getRedisRetryQueue() {
        return getApplicationName() + split_str + redisExceptionQueue;
    }

    public String getRedisNormalQueue() {
        return getApplicationName() + split_str + redisNormalQueue;
    }

    public String getRedisRetryHashMap() {
        return getApplicationName() + split_str + redisExceptionMap;
    }

    public String getRedisNormalHashMap() {
        return getApplicationName() + split_str + redisNormalMap;
    }

    public String getNormalQueueTaskLockName() {
        return getApplicationName() + split_str + "NormalQueueTaskLock";
    }

    public String getRetryQueueTaskLockName() {
        return getApplicationName() + split_str + "RetryQueueTaskLock";
    }

    public String getApplicationName() {
        if (StringUtils.isBlank(applicationName)) {
            InetAddress inetAddress = PigeonUtils.getLocalHostAddress();
            if (inetAddress != null) {
                applicationName = inetAddress.getHostName() + "_" + inetAddress.getHostAddress();
            } else {
                applicationName = RandomStringUtils.randomAlphanumeric(10) + DateUtil.getCurrentTimeMillis();
            }
            logger.warn("[PigeonEvent]->applicationName failed to get,so give you a random name:{}", applicationName);
        }
        return applicationName;
    }

}
