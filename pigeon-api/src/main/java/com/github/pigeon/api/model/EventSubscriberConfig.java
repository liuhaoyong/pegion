package com.github.pigeon.api.model;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pigeon.api.convertor.EventConvertor;
import com.github.pigeon.api.enums.EventPublishProtocol;
import com.github.pigeon.api.sender.EventSender;
import com.github.pigeon.api.utils.VelocityUtil;

/**
 * 事件订阅者配置
 *
 * @author liuhaoyong
 *         time : 2015年11月2日 下午7:53:25
 */
public class EventSubscriberConfig {

    protected static final Logger logger = LoggerFactory.getLogger(EventSubscriberConfig.class);

    private String eventType;

    //唯一标识
    private long id;

    /**
     * 事件过滤表达式
     */
    public String filterExpression;

    /**
     * 事件发送器
     */
    private EventSender eventSender;

    /**
     * 最大重试次数
     */
    private int maxRetryTimes;

    /**
     * 事件发布协议
     */
    private EventPublishProtocol protocol;

    /**
     * 目标地址
     */
    private String targetAddress;

    /**
     * 通知内容生成器
     */
    private EventConvertor<?> convertor;
    
    /**
     * 是否持久化消息
     */
    private boolean isPersist = true;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFilterExpression() {
        return filterExpression;
    }

    public void setFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }


    public boolean isPersist(){
        return isPersist;
    }
    
    

    public void setPersist(boolean isPersist) {
        this.isPersist = isPersist;
    }

    /**
     * 判断当前事件是否匹配订阅者
     *
     * @return
     */
    public boolean isMatch(Map<String, Object> eventContext) {

        if (StringUtils.isBlank(filterExpression))
            return true;
        
        if(StringUtils.equalsIgnoreCase(StringUtils.trim(filterExpression), "true")) {
            return true;
        }

        if (StringUtils.equalsIgnoreCase(StringUtils.trim(filterExpression), "false")) {
            return false;
        }

        //velocity解析
        String velocityExpr = null;
        try{
            velocityExpr = StringUtils.replace(StringUtils.containsIgnoreCase(this.filterExpression, "${") ? this.filterExpression : "${" + this.filterExpression + "}", ";", "");
            return VelocityUtil.isTrue(velocityExpr,eventContext);
        }catch (Exception e){
            logger.error("[PigeonEvent]velocityError,expr=" + velocityExpr + ",paramMap=" + eventContext,e );
        }
        return false;
    }

    public EventSender getEventSender() {
        return eventSender;
    }

    public void setEventSender(EventSender eventSender) {
        this.eventSender = eventSender;
    }

    public int getMaxRetryTimes() {
        return maxRetryTimes;
    }

    public void setMaxRetryTimes(int maxRetryTimes) {
        this.maxRetryTimes = maxRetryTimes;
    }

    public EventPublishProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(EventPublishProtocol protocol) {
        this.protocol = protocol;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(String targetAddress) {
        this.targetAddress = targetAddress;
    }

    public EventConvertor<?> getConvertor() {
        return convertor;
    }

    public void setConvertor(EventConvertor<?> contentBuilder) {
        this.convertor = contentBuilder;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}