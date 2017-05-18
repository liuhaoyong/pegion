/*
 * Sdo.com Inc.
 * Copyright (c) 2010 All Rights Reserved.
 */
package com.github.pigeon.api.model;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * 失败的事件日志记录
 * 
 * @author liuhaoyong time : 2015年11月3日 下午2:14:29
 */
public class FailedEventLog{

    /**
     * 订阅者配置ID
     */
    private long   configId;

    /**
     * 目标地址
     */
    private String targetAddress;

    /**
     * 已重试的次数
     */
    private int    retriedTimes;

    /**
     * 事件对象
     */
    private String eventContent;
    
    /**
     * 事件key
     */
    private String eventKey;

    /**
     * 事件类型
     */
    private String eventType;
    
    /**
     * 事件发送结果
     */
    private String sendResult;

    public String getSendResult() {
        return sendResult;
    }

    public void setSendResult(String sendResult) {
        this.sendResult = sendResult;
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

    public int getRetriedTimes() {
        return retriedTimes;
    }

    public void setRetriedTimes(int retriedTimes) {
        this.retriedTimes = retriedTimes;
    }

    public String getEventContent() {
        return eventContent;
    }

    public void setEventContent(String eventContent) {
        this.eventContent = eventContent;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
    
}
