package com.github.pigeon.api;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.github.pigeon.api.model.DomainEvent;
import com.github.pigeon.api.model.EventSubscriberConfig;
import com.github.pigeon.api.model.EventWrapper;
import com.github.pigeon.api.repository.EventRepository;
import com.github.pigeon.api.repository.SubscriberConfigRepository;
import com.github.pigeon.api.repository.impl.PublisherConfigParams;
import com.github.pigeon.api.utils.PigeonUtils;
import com.github.pigeon.api.utils.executors.MDCThreadPoolExecutor;


/**
 * 领域事件发布服务
 *
 * @author liuhaoyong time : 2015年11月3日 上午11:01:03
 */
public class DomainEventPublisher  {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventPublisher.class);

    /**
     * 事件发送执行器
     */
    private EventPublishExecutor eventSendExecutor;
    
    /**
     * 事件仓储
     */
    public EventRepository eventRepository;

    /**
     * 事件订阅配置的工厂
     */
    private SubscriberConfigRepository subseriberConfigFactory;

    /**
     * 事件发布线程池
     */
    public MDCThreadPoolExecutor eventPublishExecutor;
    
    
    public DomainEventPublisher(EventRepository eventRepository,EventPublishExecutor eventSendExecutor,SubscriberConfigRepository subseriberConfigFactory, PublisherConfigParams publisherConfigParams, MDCThreadPoolExecutor mdcThreadPoolExecutor)
    {
        this.eventRepository = eventRepository;
        this.eventSendExecutor = eventSendExecutor;
        this.subseriberConfigFactory = subseriberConfigFactory;
        this.eventPublishExecutor=mdcThreadPoolExecutor; 
    }


    /**
     * 发布一个领域事件
     */
    
    public boolean publish(DomainEvent event) {
        return publish(event, null);
    }

    
    public boolean publish(DomainEvent event, Map<String, Object> args) {
        
        if (event == null) {
            logger.info("事件为空");
            return false;
        }
        
        long time = System.currentTimeMillis();
        try {
            eventPublishExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    doPublish(event, args);
                }
            });
            logger.info("event:{},map:{},elapsed:{}", PigeonUtils.marshall(event), args, System.currentTimeMillis()-time);
            return true;
        } catch (Exception e) {
            logger.error("事件发送异常，event:{},map:{},elapsed:{}", PigeonUtils.marshall(event), args, System.currentTimeMillis()-time, e);
        }
        return false;
    }

    /**
     * @param event
     * @param args
     * @return
     */
    private boolean doPublish(DomainEvent event, Map<String, Object> args) {
        long start = System.currentTimeMillis();
        try {

            final String eventKey = event.getEventKey();
            
            // 获得事件订阅者配置，如无事件订阅者，直接返回
            final List<EventSubscriberConfig> subscriberList = subseriberConfigFactory.getEventSubscriberConfig(event, args);
            if (CollectionUtils.isEmpty(subscriberList)) {
                logger.info("无订阅者,{}", event);
                return true;
            }

            // 循环发布事件
            for (final EventSubscriberConfig item : subscriberList) {
                EventWrapper eventWrapper = null;

                try {
                    eventWrapper = this.buildEventWrapper(event, item, eventKey);
                    if(item.needToQueue()){
                        eventRepository.saveEvent(eventWrapper); //持久化事件
                    }
                    /**
                     * 异步发送事件
                     */
                    eventSendExecutor.sendEvent(item, eventWrapper);
                } catch (Exception e) {
                    logger.error("事件发送异常,eventKey={},event:{}, configID={}", eventKey, eventWrapper, item.getId(), e);
                    continue;
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("事件发送失败 耗时:{},event={}", ( System.currentTimeMillis()- start), PigeonUtils.marshall(event), e);
            return false;
        }
    }

    /**
     * @param result
     * @param event
     * @param config
     * @return
     * @throws Exception
     */
    public final static EventWrapper buildEventWrapper(DomainEvent event, EventSubscriberConfig config,String eventKey) throws Exception {
        EventWrapper result = new EventWrapper();
        result.setConfigId(config.getId());
        result.setEvent(config.getConvertor().convert(event, config));
        result.setAppName(config.getAppName());
        result.setTargetAddress(config.getConvertor().getTargetAddress(event, config));
        result.setSentTimes(0);
        result.setEventType(event.getClass().getSimpleName());
        result.setEventKey(eventKey);
        if (result.getEvent() == null) {
            return null;
        }
        return result;
    }
    
   
    

}