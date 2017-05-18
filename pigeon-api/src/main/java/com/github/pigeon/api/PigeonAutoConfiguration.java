package com.github.pigeon.api;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestTemplate;

import com.github.diamond.client.PropertiesConfiguration;
import com.github.pigeon.api.convertor.DefaultHttpProtocolConvertor;
import com.github.pigeon.api.convertor.DefaultSpringProtocolConvertor;
import com.github.pigeon.api.enums.EventPublishProtocol;
import com.github.pigeon.api.repository.EventRepository;
import com.github.pigeon.api.repository.SubscriberConfigRepository;
import com.github.pigeon.api.repository.impl.DiamondSubseriberConfigRepository;
import com.github.pigeon.api.repository.impl.PublisherConfigParams;
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
@Configuration
@ConditionalOnMissingBean(DomainEventPublisher.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableConfigurationProperties(PublisherConfigParams.class)
public class PigeonAutoConfiguration {

    @Autowired
    private PublisherConfigParams         publisherConfigParams;

    @Autowired
    private StringRedisTemplate           stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PropertiesConfiguration       propertiesConfiguration;

    @Autowired
    private RestTemplate                  restTemplate;

    @Bean
    @ConditionalOnMissingBean(EventRepository.class)
    @ConditionalOnBean(RedisTemplate.class)
    public EventRepository eventRepository() {
        EventRepository result = new RedisEventRepository(publisherConfigParams, redisTemplate);
        return result;
    }

    @Bean
    @ConditionalOnMissingBean(SubscriberConfigRepository.class)
    public SubscriberConfigRepository subscriberConfigRepository(ApplicationContext applicationContext,
                                                                 @Qualifier("eventSenderMap") Map<EventPublishProtocol, EventSender> eventSenderMap) {
        DiamondSubseriberConfigRepository result = new DiamondSubseriberConfigRepository(applicationContext,
                propertiesConfiguration, publisherConfigParams, eventSenderMap);
        return result;

    }

    @Bean
    @ConditionalOnMissingBean(EventPublishExecutor.class)
    public EventPublishExecutor eventPublishExecutor(SubscriberConfigRepository eventSubseriberConfigFactory,
                                                     EventRepository eventRepository,
                                                     @Qualifier("normalEventSendExecutor") MDCThreadPoolExecutor mdcThreadPoolExecutor) {
        EventPublishExecutor result = new EventPublishExecutor(eventSubseriberConfigFactory, eventRepository,
                publisherConfigParams, stringRedisTemplate, mdcThreadPoolExecutor);
        return result;
    }

    @Bean
    @ConditionalOnMissingBean(PublishExceptionHandler.class)
    @ConditionalOnBean({ EventRepository.class, EventPublishExecutor.class })
    public PublishExceptionHandler publishExceptionHandler(EventRepository eventRepository,
                                                           EventPublishExecutor eventSendExecutor) {
        PublishExceptionHandler result = new PublishExceptionHandler(eventRepository, eventSendExecutor,
                publisherConfigParams, stringRedisTemplate);
        eventSendExecutor.publishExceptionHandler = result;
        return result;
    }

    /**
     * 发送正常事件的exceutor
     * 
     * @return
     */
    @Bean(name = "normalEventSendExecutor")
    public MDCThreadPoolExecutor normalEventSendExecutor() {
        MDCThreadPoolExecutor pigeonEventPublishExecutor = new MDCThreadPoolExecutor();
        pigeonEventPublishExecutor.setPname("eventPublishExecutor");
        pigeonEventPublishExecutor.setCorePoolSize(publisherConfigParams.getEventPublishcorePoolSize());
        pigeonEventPublishExecutor.setMaxPoolSize(publisherConfigParams.getGetEventPublishMaxPoolSize());
        pigeonEventPublishExecutor.setQueueCapacity(publisherConfigParams.getMaxLocalQueueSize());
        pigeonEventPublishExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return pigeonEventPublishExecutor;
    }

    /**
     * 发送正常事件的exceutor
     * 
     * @return
     */
    @Bean(name = "exceptionEventSendExecutor")
    public MDCThreadPoolExecutor exceptionEventSendExecutor() {
        MDCThreadPoolExecutor pigeonEventSendExecutor = new MDCThreadPoolExecutor();
        pigeonEventSendExecutor.setPname("pigeonEventSendExecutor");
        pigeonEventSendExecutor.setCorePoolSize(publisherConfigParams.getEventSentCorePoolSize());
        pigeonEventSendExecutor.setMaxPoolSize(publisherConfigParams.getEventSentMaxPoolSize());
        pigeonEventSendExecutor.setQueueCapacity(publisherConfigParams.getMaxLocalQueueSize());
        pigeonEventSendExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return pigeonEventSendExecutor;
    }

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    @ConditionalOnBean(value = { EventRepository.class, EventPublishExecutor.class,
            SubscriberConfigRepository.class }, name = { "normalEventSendExecutor" })
    public DomainEventPublisher domainEventPublisher(@Qualifier("exceptionEventSendExecutor") MDCThreadPoolExecutor normalEventSendExecutor,
                                                     EventRepository eventRepository,
                                                     EventPublishExecutor eventSendExecutor,
                                                     SubscriberConfigRepository subseriberConfigFactory) {
        DomainEventPublisher result = new DomainEventPublisher(eventRepository, eventSendExecutor,
                subseriberConfigFactory, publisherConfigParams, normalEventSendExecutor);
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
