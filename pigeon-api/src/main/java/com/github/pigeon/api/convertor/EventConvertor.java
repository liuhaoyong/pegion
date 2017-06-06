package com.github.pigeon.api.convertor;

import com.github.pigeon.api.model.DomainEvent;
import com.github.pigeon.api.model.EventSubscriberConfig;

/**
 * 事件转换器
 * 用于将事件转换成订阅者需要格式的事件
 * 每个订阅者都可以单独实现其转换器
 * @author liuhaoyong
 * time : 2015年11月3日 上午10:06:09
 */
public interface EventConvertor<REQUEST_EVENT extends DomainEvent> {
    
    /**
     * 转换后的内容
     * @param event
     * @param config
     * @return
     */
    String convert(REQUEST_EVENT event, EventSubscriberConfig config) ;
    
    /**
     *  返回事件接收方的目标地址
     * @return
     */
    String getTargetAddress(DomainEvent event, EventSubscriberConfig config);
    
}