package com.github.pigeon.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.ValidationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.github.pigeon.api.convertor.EventConvertor;
import com.github.pigeon.api.model.DomainEvent;
import com.github.pigeon.api.model.EventSubscriberConfig;
import com.github.pigeon.api.model.EventWrapper;
import com.github.pigeon.api.repository.EventRepository;
import com.github.pigeon.api.repository.SubscriberConfigRepository;
import com.github.pigeon.api.repository.impl.PigeonConfigProperties;
import com.github.pigeon.api.utils.VelocityUtil;
import com.github.pigeon.api.utils.executors.MDCThreadPoolExecutor;

/**
 * 领域事件发布服务
 *
 * @author liuhaoyong time : 2015年11月3日 上午11:01:03
 */
public class DomainEventPublisher {

    private static final Logger        logger = LoggerFactory.getLogger(DomainEventPublisher.class);

    /**
     * 事件发送执行器
     */
    private EventPublishExecutor       eventSendExecutor;

    /**
     * 事件仓储
     */
    public EventRepository             eventRepository;

    /**
     * 事件订阅配置的工厂
     */
    private SubscriberConfigRepository subseriberConfigRepository;

    /**
     * 接收事件的线程池
     */
    public MDCThreadPoolExecutor       acceptThreadPool;

    public DomainEventPublisher(EventRepository eventRepository, EventPublishExecutor eventSendExecutor,
                                SubscriberConfigRepository subseriberConfigFactory,
                                PigeonConfigProperties publisherConfigParams, MDCThreadPoolExecutor acceptThreadPool) {
        this.eventRepository = eventRepository;
        this.eventSendExecutor = eventSendExecutor;
        this.subseriberConfigRepository = subseriberConfigFactory;
        this.acceptThreadPool = acceptThreadPool;
    }

    /**
     * 发布领域事件
     * 
     * @param event
     * @return
     */
    public boolean publish(DomainEvent event) {
        return publish(event, null);
    }

    /**
     * 发布领域事件，可附加参数
     * 
     * @param event
     * @param args
     * @return
     */
    public boolean publish(DomainEvent event, Map<String, Object> args) {

        if (event == null) {
            logger.info("事件为空");
            return false;
        }

        long time = System.currentTimeMillis();
        try {
            acceptThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    doPublish(event, args);
                }
            });
            logger.info("事件发布成功, event={}, map={},elapsed={}", event, args, System.currentTimeMillis() - time);
            return true;
        } catch (Exception e) {
            logger.error("事件发布异常, event={},map={},elapsed={}", event, args, System.currentTimeMillis() - time, e);
        }
        return false;
    }

    private boolean doPublish(DomainEvent event, Map<String, Object> args) {
        long start = System.currentTimeMillis();
        try {

            final String eventKey = event.getEventKey();

            // 获得事件订阅者配置，如无事件订阅者，丢弃消息
            final List<EventSubscriberConfig> subscriberList = subseriberConfigRepository
                    .getEventSubscriberConfig(event, args);
            if (CollectionUtils.isEmpty(subscriberList)) {
                logger.info("无订阅者,eventKey={}", eventKey);
                return true;
            }

            // 循环发布事件
            for (final EventSubscriberConfig item : subscriberList) {

                try {
                    EventWrapper eventWrapper = this.buildEventWrapper(event, item, eventKey);
                    if (item.isPersist()) {
                        eventRepository.persistEvent(eventWrapper);
                    }

                    eventSendExecutor.sendEvent(item, eventWrapper);
                } catch (Exception e) {
                    logger.error("事件发送异常,eventKey={},event:{}, configID={}", eventKey, event, item.getId(), e);
                    continue;
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("事件发送失败 耗时:{},event={}", (System.currentTimeMillis() - start), event, e);
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
    public EventWrapper buildEventWrapper(DomainEvent event, EventSubscriberConfig config, String eventKey) {
        EventConvertor convertor = config.getConvertor();
        String eventContent = convertor.convert(event, config);
        String targetAddress = getTargetAddress(convertor,event,config);
        if (eventContent == null || StringUtils.isBlank(targetAddress)) {
            logger.error(
                    "转换后的事件内容或者目标地址为空, 忽略发送, 请检查时间订阅者配置是否正确， eventContent={},targetAddress={}, subscriberConfig={}",
                    eventContent, targetAddress, config);
            throw new RuntimeException("转换后的事件内容或者目标地址为空，忽略发送");
        }

        EventWrapper result = new EventWrapper();
        result.setConfigId(config.getId());
        result.setContent(eventContent);
        result.setTargetAddress(targetAddress);
        result.setSentTimes(0);
        result.setEventType(event.getClass().getSimpleName());
        result.setEventKey(eventKey);
        if (result.getContent() == null) {
            return null;
        }
        return result;
    }

    /**
     * 获取目标地址
     * <p>1. 先通过convertor里的实现取通知地址
     * <p>2. 当订阅配置里targetAddres为地址取值表达式时，执行该表达式取值
     * <p>3. 不是取值表达式，则认为配置的就是一个原始地址，直接返回
     * @param event
     * @param config
     * @return
     */
    private String getTargetAddress(EventConvertor convertor, DomainEvent event, EventSubscriberConfig config) {
        
        String result = convertor.getTargetAddress(event, config);
        if(StringUtils.isNotBlank(result))
        {
            return result;
        }
        
        String targetAddress = config.getTargetAddress();
        if (StringUtils.startsWith(targetAddress, "${")) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("event", event);
            try {
                targetAddress = VelocityUtil.getString(targetAddress, map);
            } catch (ValidationException e) {
                logger.error("获取通知地址异常:eventKey={}", event.getEventKey(), e);
                return null;
            }
        }
        return targetAddress;
    }
}
