package com.github.pigeon.api.model;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * 时间的包装
 * 
 * @author liuhaoyong 2017年5月16日 上午10:11:26
 */
public class EventWrapper extends CommonEvent{

    private static final long serialVersionUID = -6297104699061739039L;

    /**
     * 目标地址
     */
    private String targetAddress;

    /**
     * 应用名
     */
    private String appName;

    /**
     * 订阅者配置ID
     */
    private long configId;

    /**
     * 已发送的次数
     */
    private int sentTimes;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件对象
     */
    private Object event;

    /**
     * 事件发送结果
     */
    private String sendResult;

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

    public Object getEvent() {
        return event;
    }

    public void setEvent(Object event) {
        this.event = event;
    }

    /**
     * @return
     */
    public String genUniformEventKey() {
        if (StringUtils.isBlank(eventType)) {
            return getEventKey();
        }
        return this.eventType + "||" + getEventKey();
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
