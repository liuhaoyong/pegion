package com.github.pigeon.api.model;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * 订阅者配置数据对象
 * 
 * @author liuhaoyong 2017年5月16日 下午4:49:43
 */
public class SubscribeConfigDo{
    
    private long id;
    
    private String eventType;

    private String filterExpression;

    private String protocol;

    private String targetAddress;

    private String convertor;

    private String memo;

    private int maxRetryTimes;
    
    private boolean isPersist=true;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
    

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getFilterExpression() {
        return filterExpression;
    }

    public void setFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(String targetAddress) {
        this.targetAddress = targetAddress;
    }

    public String getConvertor() {
        return convertor;
    }

    public void setConvertor(String convertor) {
        this.convertor = convertor;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public int getMaxRetryTimes() {
        return maxRetryTimes;
    }

    public void setMaxRetryTimes(int masRetryTimes) {
        this.maxRetryTimes = masRetryTimes;
    }


    public boolean isPersist() {
        return isPersist;
    }

    public void setPersist(boolean isPersist) {
        this.isPersist = isPersist;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}