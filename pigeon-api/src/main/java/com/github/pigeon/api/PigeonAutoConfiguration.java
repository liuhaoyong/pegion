package com.github.pigeon.api;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestTemplate;

import com.github.diamond.client.PropertiesConfiguration;
import com.github.pigeon.api.convertor.DefaultHttpProtocolConvertor;
import com.github.pigeon.api.convertor.DefaultSpringProtocolConvertor;
import com.github.pigeon.api.enums.EventPublishProtocol;
import com.github.pigeon.api.repository.EventRepository;
import com.github.pigeon.api.repository.SubscriberConfigRepository;
import com.github.pigeon.api.repository.impl.DiamondSubseriberConfigRepository;
import com.github.pigeon.api.repository.impl.PigeonConfigProperties;
import com.github.pigeon.api.repository.impl.RedisEventRepository;
import com.github.pigeon.api.sender.EventSender;
import com.github.pigeon.api.sender.HttpSender;
import com.github.pigeon.api.sender.SpringSender;
import com.github.pigeon.api.utils.executors.MDCThreadPoolExecutor;

/**
 * pigeon自动配置工厂
 * 
 * @author liuhaoyong 2017年5月17日 上午11:40:58
 */
@Order(100)
@Configuration
@ConditionalOnMissingBean(DomainEventPublisher.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableConfigurationProperties(PigeonConfigProperties.class)
public class PigeonAutoConfiguration {

    @Autowired
    private PigeonConfigProperties  pigeonConfigProperties;

    @Autowired
    private StringRedisTemplate     stringRedisTemplate;

    @Autowired
    private PropertiesConfiguration propertiesConfiguration;

    @Autowired
    private RestTemplate            restTemplate;

    @Autowired
    private CuratorFramework        zkClient;

    @Bean
    @ConditionalOnMissingBean(EventRepository.class)
    @ConditionalOnBean(StringRedisTemplate.class)
    public EventRepository eventRepository() {
        EventRepository result = new RedisEventRepository(pigeonConfigProperties, stringRedisTemplate);
        return result;
    }

    @Bean
    @ConditionalOnMissingBean(SubscriberConfigRepository.class)
    public SubscriberConfigRepository subscriberConfigRepository(ApplicationContext applicationContext,
                                                                 @Qualifier("eventSenderMap") Map<EventPublishProtocol, EventSender> eventSenderMap) {
        DiamondSubseriberConfigRepository result = new DiamondSubseriberConfigRepository(applicationContext,
                propertiesConfiguration, pigeonConfigProperties, eventSenderMap);
        return result;

    }

    @Bean
    @ConditionalOnMissingBean(EventPublishExecutor.class)
    public EventPublishExecutor eventPublishExecutor(SubscriberConfigRepository eventSubseriberConfigFactory,
                                                     EventRepository eventRepository,
                                                     @Qualifier("sendExecutor") MDCThreadPoolExecutor sendExecutor) {
        EventPublishExecutor result = new EventPublishExecutor(eventSubseriberConfigFactory, eventRepository,
                pigeonConfigProperties, stringRedisTemplate, sendExecutor, zkClient);
        return result;
    }

    @Bean
    @ConditionalOnMissingBean(PublishExceptionHandler.class)
    @ConditionalOnBean({ EventRepository.class, EventPublishExecutor.class, CuratorFramework.class })
    public PublishExceptionHandler publishExceptionHandler(EventRepository eventRepository,
                                                           EventPublishExecutor eventSendExecutor) {
        PublishExceptionHandler result = new PublishExceptionHandler(eventRepository, eventSendExecutor,
                pigeonConfigProperties, stringRedisTemplate, zkClient);
        eventSendExecutor.publishExceptionHandler = result;
        return result;
    }

    /**
     * 发送事件的exceutor
     * 
     * @return
     */
    @Bean(name = "sendExecutor")
    public MDCThreadPoolExecutor normalEventSendExecutor() {
        MDCThreadPoolExecutor pigeonEventPublishExecutor = new MDCThreadPoolExecutor();
        pigeonEventPublishExecutor.setPname("sendExecutor");
        pigeonEventPublishExecutor.setCorePoolSize(pigeonConfigProperties.getSendCorePoolSize());
        pigeonEventPublishExecutor.setMaxPoolSize(pigeonConfigProperties.getSendMaxPoolSize());
        pigeonEventPublishExecutor.setQueueCapacity(pigeonConfigProperties.getSendQueueSize());
        pigeonEventPublishExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return pigeonEventPublishExecutor;
    }

    /**
     * 接收事件的exceutor
     * 
     * @return
     */
    @Bean(name = "acceptExecutor")
    public MDCThreadPoolExecutor exceptionEventSendExecutor() {
        MDCThreadPoolExecutor pigeonEventSendExecutor = new MDCThreadPoolExecutor();
        pigeonEventSendExecutor.setPname("acceptExecutor");
        pigeonEventSendExecutor.setCorePoolSize(pigeonConfigProperties.getAcceptCorePoolSize());
        pigeonEventSendExecutor.setMaxPoolSize(pigeonConfigProperties.getAcceptMaxPoolSize());
        pigeonEventSendExecutor.setQueueCapacity(pigeonConfigProperties.getAcceptQueueSize());
        pigeonEventSendExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return pigeonEventSendExecutor;
    }

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    @ConditionalOnBean(value = { EventRepository.class, EventPublishExecutor.class, SubscriberConfigRepository.class })
    public DomainEventPublisher domainEventPublisher(@Qualifier("acceptExecutor") MDCThreadPoolExecutor acceptExecutor,
                                                     EventRepository eventRepository,
                                                     EventPublishExecutor eventSendExecutor,
                                                     SubscriberConfigRepository subseriberConfigFactory) {
        DomainEventPublisher result = new DomainEventPublisher(eventRepository, eventSendExecutor,
                subseriberConfigFactory, pigeonConfigProperties, acceptExecutor);
        return result;
    }

    @Bean(name = "defaultHttpProtocolConvertor")
    @ConditionalOnMissingBean(DefaultHttpProtocolConvertor.class)
    public DefaultHttpProtocolConvertor httpProtoclConvertor() {
        return new DefaultHttpProtocolConvertor();
    }

    @Bean(name = "defaultSpringProtocolConvertor")
    @ConditionalOnMissingBean(DefaultSpringProtocolConvertor.class)
    public DefaultSpringProtocolConvertor springProtoclConvertor() {
        return new DefaultSpringProtocolConvertor();
    }

    @Bean(name = "eventSenderMap")
    @ConditionalOnMissingBean(name = "eventSenderMap")
    public Map<EventPublishProtocol, EventSender> eventSenderMap(ApplicationContext applicationContext) {
        Map<EventPublishProtocol, EventSender> map = new HashMap<EventPublishProtocol, EventSender>();
        map.put(EventPublishProtocol.HTTP, new HttpSender(restTemplate));
        map.put(EventPublishProtocol.SPRING, new SpringSender(applicationContext));
        return map;
    }

}
