package com.github.pigeon.api.model;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * 时间的包装
 * 
 * @author liuhaoyong 2017年5月16日 上午10:11:26
 */
public class EventWrapper {

    /**
     * 目标地址
     */
    private String            targetAddress;

    /**
     * 订阅者配置ID
     */
    private long              configId;

    /**
     * 已发送的次数
     */
    private int               sentTimes;

    /**
     * 事件类型
     */
    private String            eventType;

    /**
     * 发送给消费者的真实内容,convertor转换后
     */
    private String            content;

    /**
     * 事件发送结果
     */
    private String            sendResult;
    
    /**
     * 事件业务唯一标识
     */
    private String            eventKey;

    public boolean isRetry() {
        return sentTimes > 0;
    }

    public String getSendResult() {
        return sendResult;
    }

    public void setSendResult(String sendResult) {
        this.sendResult = sendResult;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public int getSentTimes() {
        return sentTimes;
    }

    public void setSentTimes(int sentTimes) {
        this.sentTimes = sentTimes;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(String targetAddress) {
        this.targetAddress = targetAddress;
    }

    public long getConfigId() {
        return configId;
    }

    public void setConfigId(long configId) {
        this.configId = configId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 用来唯一标识一个待发送的消息 事件订阅者ID+事件类型+事件业务key
     * 
     * @return
     */
    public String genUniformEventKey() {
        return this.configId + "||" + this.eventType + "||" + this.eventKey;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
