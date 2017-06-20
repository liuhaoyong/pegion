package com.github.pigeon.api.repository.impl;

import java.net.InetAddress;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.github.pigeon.api.utils.DateUtil;
import com.github.pigeon.api.utils.PigeonUtils;

/**
 * pigeon的配置参数
 * 
 * @author liuhaoyong time : 2015年11月3日 下午7:12:28
 */
@ConfigurationProperties(prefix = "pigeon")
public class PigeonConfigProperties {

    private static final Logger logger                          = LoggerFactory.getLogger(PigeonConfigProperties.class);
    private static final String split_str                       = "_";
    public static final int     defaultMaxLocalQueueSize        = 10000;

    /**
     * 名称前缀，建议使用应用名称,redis里会用来作为前缀key
     */
    private String              namePrefix                      = "pigeonClient";

    /**
     * 订阅者配置模块名
     */
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
     * 当使用http投递消息时的连接超时时间
     */
    private int                 httpConnectTimeoutInMillisecond = 3000;

    /**
     * 当使用http投递消息时的读超时时间
     */
    private int                 httpSoTimeoutInMillisecond      = 5000;

    /**
     * accept线程池相关配置
     */
    private int                 acceptCorePoolSize              = 5;

    private int                 acceptMaxPoolSize               = 5;

    private int                 acceptQueueSize                 = 100;

    /**
     * sender线程池相关配置
     */
    private int                 sendCorePoolSize                = 10;

    private int                 sendMaxPoolSize                 = 20;

    private int                 sendQueueSize                   = defaultMaxLocalQueueSize;

    /**
     * 异常事件的重试间隔执行时间，单位分
     */
    private long                retryIntervalInMinitues         = 1;

    /**
     * 重试时每次从redis里获取的事件数量
     */
    private int                 retryFetchCount                 = 100;
    
    /**
     * zk服务器地址, ip:port,ip:port格式
     */
    private String              zkServerAddress;

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public void setSubscribeConfigModuleName(String subscribeConfigModuleName) {
        this.subscribeConfigModuleName = subscribeConfigModuleName;
    }

    public void setRedisExceptionQueue(String redisExceptionQueue) {
        this.redisExceptionQueue = redisExceptionQueue;
    }

    public void setRedisExceptionMap(String redisExceptionMap) {
        this.redisExceptionMap = redisExceptionMap;
    }

    public void setRedisNormalQueue(String redisNormalQueue) {
        this.redisNormalQueue = redisNormalQueue;
    }

    public void setRedisNormalMap(String redisNormalMap) {
        this.redisNormalMap = redisNormalMap;
    }

    public void setHttpConnectTimeoutInMillisecond(int httpConnectTimeoutInMillisecond) {
        this.httpConnectTimeoutInMillisecond = httpConnectTimeoutInMillisecond;
    }

    public void setHttpSoTimeoutInMillisecond(int httpSoTimeoutInMillisecond) {
        this.httpSoTimeoutInMillisecond = httpSoTimeoutInMillisecond;
    }

    public void setAcceptCorePoolSize(int acceptCorePoolSize) {
        this.acceptCorePoolSize = acceptCorePoolSize;
    }

    public void setAcceptMaxPoolSize(int acceptMaxPoolSize) {
        this.acceptMaxPoolSize = acceptMaxPoolSize;
    }

    public void setAcceptQueueSize(int acceptQueueSize) {
        this.acceptQueueSize = acceptQueueSize;
    }

    public void setSendCorePoolSize(int sendCorePoolSize) {
        this.sendCorePoolSize = sendCorePoolSize;
    }

    public void setSendMaxPoolSize(int sendMaxPoolSize) {
        this.sendMaxPoolSize = sendMaxPoolSize;
    }

    public void setSendQueueSize(int sendQueueSize) {
        this.sendQueueSize = sendQueueSize;
    }

    public void setRetryIntervalInMinitues(long retryIntervalInMinitues) {
        this.retryIntervalInMinitues = retryIntervalInMinitues;
    }

    public void setRetryFetchCount(int retryFetchCount) {
        this.retryFetchCount = retryFetchCount;
    }

    public int getSendQueueSize() {
        return sendQueueSize;
    }

    public int getSendCorePoolSize() {
        return sendCorePoolSize;
    }

    public int getSendMaxPoolSize() {
        return sendMaxPoolSize;
    }

    public String getSubscribeConfigModuleName() {
        return subscribeConfigModuleName;
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

    public int getHttpConnectTimeoutInMillisecond() {
        return httpConnectTimeoutInMillisecond;
    }

    public int getHttpSoTimeoutInMillisecond() {
        return httpSoTimeoutInMillisecond;
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

    public String getExceptionQueueTaskLockName() {
        return getApplicationName() + split_str + "ExceptionQueueTaskLock";
    }

    public String getRedisExceptionQueue() {
        return redisExceptionQueue;
    }

    public String getRedisExceptionMap() {
        return redisExceptionMap;
    }

    public String getRedisNormalMap() {
        return redisNormalMap;
    }

    public int getAcceptCorePoolSize() {
        return acceptCorePoolSize;
    }

    public int getAcceptMaxPoolSize() {
        return acceptMaxPoolSize;
    }

    public int getAcceptQueueSize() {
        return acceptQueueSize;
    }
    
    

    public String getZkServerAddress() {
        return zkServerAddress;
    }

    public void setZkServerAddress(String zkServerAddress) {
        this.zkServerAddress = zkServerAddress;
    }

    public String getApplicationName() {
        if (StringUtils.isBlank(namePrefix)) {
            InetAddress inetAddress = PigeonUtils.getLocalHostAddress();
            if (inetAddress != null) {
                namePrefix = inetAddress.getHostName() + "_" + inetAddress.getHostAddress();
            } else {
                namePrefix = RandomStringUtils.randomAlphanumeric(10) + DateUtil.getCurrentTimeMillis();
            }
            logger.warn("[PigeonEvent]->applicationName failed to get,so give you a random name:{}", namePrefix);
        }
        return namePrefix;
    }

}
