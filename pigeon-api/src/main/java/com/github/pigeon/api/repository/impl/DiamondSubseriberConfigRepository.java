package com.github.pigeon.api.repository.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.github.diamond.client.PropertiesConfiguration;
import com.github.diamond.client.event.ConfigurationEvent;
import com.github.diamond.client.event.ConfigurationListener;
import com.github.pigeon.api.convertor.EventConvertor;
import com.github.pigeon.api.enums.EventPublishProtocol;
import com.github.pigeon.api.model.DomainEvent;
import com.github.pigeon.api.model.EventSubscriberConfig;
import com.github.pigeon.api.model.SubscribeConfigDo;
import com.github.pigeon.api.repository.SubscriberConfigRepository;
import com.github.pigeon.api.sender.EventSender;
import com.github.pigeon.api.utils.PigeonUtils;

/**
 * 基于supper diamond配置中心实现的订阅者配置工厂
 * 
 * @author liuhaoyong 2017年5月16日 下午4:30:17
 */
public class DiamondSubseriberConfigRepository implements SubscriberConfigRepository {

    protected static final Logger                           logger                         = LoggerFactory
            .getLogger(DiamondSubseriberConfigRepository.class);

    /**
     * 事件订阅者配置缓存
     */
    private static Map<String, List<EventSubscriberConfig>> cachedEventSubcriberConfigMap  = new ConcurrentHashMap<>();
    private static Map<Long, EventSubscriberConfig>         cachedEventSubcriberConfigMap2 = new ConcurrentHashMap<>();

    private ApplicationContext                              applicationContext;

    private PropertiesConfiguration                         propertiesConfiguration;

    private PigeonConfigProperties                           publisherConfigParams;

    /**
     * 事件发送器map
     */
    private Map<EventPublishProtocol, EventSender>                        eventSenderMap;

    public DiamondSubseriberConfigRepository(ApplicationContext applicationContext,
                                             PropertiesConfiguration propertiesConfiguration,
                                             PigeonConfigProperties publisherConfigParams,
                                             Map<EventPublishProtocol, EventSender> eventSenderMap) {
        this.applicationContext = applicationContext;
        this.propertiesConfiguration = propertiesConfiguration;
        this.publisherConfigParams = publisherConfigParams;
        this.eventSenderMap = eventSenderMap;
    }

    /**
     * spring的init-method方法，缓存初始化
     */
    @PostConstruct
    public void init() {
        
        refresh();
        
        propertiesConfiguration.addConfigurationListener(new ConfigurationListener() {
            
            @Override
            public void configurationChanged(ConfigurationEvent event) {
                if(StringUtils.startsWith(event.getPropertyName(),publisherConfigParams.getSubscribeConfigModuleName() + "."))
                {
                    refresh();
                }
                
            }
        });
    }

    /**
     * 获得事件订阅者配置列表
     * 
     * @param event
     * @param args
     * @return
     */
    public List<EventSubscriberConfig> getEventSubscriberConfig(final DomainEvent event, Map<String, Object> args) {
        final String eventType = getEventType(event.getClass().getSimpleName());
        List<EventSubscriberConfig> subscribeConfigList = cachedEventSubcriberConfigMap.get(eventType);
        if (subscribeConfigList == null || subscribeConfigList.isEmpty()) {
            return null;
        }

        final Map<String, Object> velocityContext = new HashMap<>();
        if (null != args && !args.isEmpty()) {
            velocityContext.putAll(args);
        }
        velocityContext.put(eventType, event);

        final List<EventSubscriberConfig> result = new ArrayList<>();

        for (EventSubscriberConfig item : subscribeConfigList) {
            if (StringUtils.isBlank(item.filterExpression)) {
                result.add(item);
                continue;
            }

            if (item.isMatch(velocityContext)) {
                result.add(item);
            }
        }
        return result;

    }

    /**
     * 获取事件的类型
     * 
     * @param className
     * @return
     */
    private String getEventType(final String className) {
        if (StringUtils.contains(className, "@")) {
            return StringUtils.substringBefore(className, "@");
        }
        return className;
    }

    /**
     * 根据事件ID获得唯一的事件订阅配置
     * 
     * @param configId
     * @return
     */
    public EventSubscriberConfig getEventSubscriberConfig(final Long configId) {
        return (cachedEventSubcriberConfigMap2 != null) ? cachedEventSubcriberConfigMap2.get(configId) : null;
    }

    /**
     *
     */
    public void refresh() {
        //装载所有的事件订阅配置
        List<SubscribeConfigDo> subscibeList = loadCurrentAppEventConfig();

        if (subscibeList == null || subscibeList.isEmpty()) {
            logger.warn("[PigeonEvent][EVENT_JOB_config]subscribe config is null");
            return;
        }

        Map<String, List<EventSubscriberConfig>> newConfigMap = new ConcurrentHashMap<>();
        Map<Long, EventSubscriberConfig> newConfigMap2 = new ConcurrentHashMap<>();
        for (SubscribeConfigDo item : subscibeList) {
            EventSubscriberConfig config = createEventSubscriberConfig(item);
            if (config == null) {
                continue;
            }
            if (newConfigMap.containsKey(item.getEventType())) {
                newConfigMap.get(item.getEventType()).add(config);
            } else {
                List<EventSubscriberConfig> list = new ArrayList<>();
                list.add(config);
                newConfigMap.put(StringUtils.trim(item.getEventType()), list);
            }
            newConfigMap2.put(item.getId(), config);
        }

        cachedEventSubcriberConfigMap = newConfigMap;
        cachedEventSubcriberConfigMap2 = newConfigMap2;
    }

    /**
     * 读取本服务的订阅配置信息,（app_name为自身appliction_name的所有spring协议订阅 && 所有dubbo协议订阅）
     * 
     * @return
     */
    private List<SubscribeConfigDo> loadCurrentAppEventConfig() {

        List<String> listStr = propertiesConfiguration
                .getListByModule(publisherConfigParams.getSubscribeConfigModuleName());
        if (CollectionUtils.isEmpty(listStr)) {
            return null;
        }

        List<SubscribeConfigDo> results = new ArrayList<SubscribeConfigDo>();

        for (String configDo : listStr) {
            results.add(JSON.parseObject(configDo, SubscribeConfigDo.class));
        }
        return results;
    }

    /**
     * 封装订阅的config信息
     * 
     * @param item
     * @return
     */
    private EventSubscriberConfig createEventSubscriberConfig(SubscribeConfigDo item) {
        EventSubscriberConfig config = new EventSubscriberConfig();
        config.setId(item.getId());
        try {
            
            //设置convertor
            Object obj = PigeonUtils.getBean(applicationContext, item.getConvertor());
            if (obj == null) {
                logger.error("-->[PigeonEvent][EVENT_JOB_config]event builder config invalid, id={},beanName={}",
                        item.getId(), item.getConvertor());
                return null;
            }
            config.setConvertor((EventConvertor<?>) obj);
            
            //设置sender
            config.setProtocol(EventPublishProtocol.valueOf(StringUtils.upperCase(item.getProtocol())));
            EventSender sender = this.eventSenderMap.get(config.getProtocol());
            if (sender == null) {
                logger.error("-->[PigeonEvent][EVENT_JOB_config]event protocol config invalid, id={},protocol={}",
                        item.getId(), item.getProtocol());
                return null;
            }
            config.setEventSender(sender);
            
            //设置其他参数
            config.setFilterExpression(item.getFilterExpression());
            config.setMaxRetryTimes(item.getMasRetryTimes());
            config.setTargetAddress(item.getTargetAddress());
            config.setEventType(item.getEventType());
            return config;
        } catch (Exception e) {
            logger.error("event subscriber config invalid, {}", item, e);
            return null;
        }
    }

}
