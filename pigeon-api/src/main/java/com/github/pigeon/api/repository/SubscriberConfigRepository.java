package com.github.pigeon.api.repository;

import java.util.List;
import java.util.Map;

import com.github.pigeon.api.model.DomainEvent;
import com.github.pigeon.api.model.EventSubscriberConfig;

/**
 * 事件订阅配置仓储
 * 
 * @author liuhaoyong 2017年5月16日 下午1:52:06
 */
public interface SubscriberConfigRepository {
    
    /**
     * 返回给定事件的订阅者
     * @param event
     * @param args
     * @return
     */
    public List<EventSubscriberConfig> getEventSubscriberConfig(DomainEvent event, Map<String, Object> args) ;
    
    
    /**
     * 根据事件ID获得唯一的事件订阅者
     * @param configId
     * @return
     */
    public EventSubscriberConfig getEventSubscriberConfig(Long configId) ;
}
