package com.github.pigeon.test;

import java.util.Date;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSON;
import com.github.pigeon.api.DomainEventPublisher;
import com.github.pigeon.api.EventPublishExecutor;
import com.github.pigeon.api.PublishExceptionHandler;
import com.github.pigeon.api.model.DomainEvent;
import com.github.pigeon.api.model.EventSendResult;
import com.github.pigeon.api.model.EventSubscriberConfig;
import com.github.pigeon.api.model.EventWrapper;
import com.github.pigeon.api.repository.impl.DiamondSubseriberConfigRepository;
import com.github.pigeon.api.repository.impl.PigeonConfigProperties;
import com.github.pigeon.api.repository.impl.RedisEventRepository;
import com.github.pigeon.api.repository.impl.RedisEventRepository.OprTypeEnum;
import com.github.pigeon.api.utils.DateUtil;


/**
 * 
 * 
 * @author liuhaoyong 2017年5月17日 下午7:12:30
 */
public class EventPublisherTest  extends BaseTest{
    
    @Autowired
    private DomainEventPublisher eventPublisher;
    
    @Autowired
    private PublishExceptionHandler publishExceptionHandler;
    
    @Autowired
    private DiamondSubseriberConfigRepository diamondSubseriberConfigRepository;
    
    @Autowired
    private EventPublishExecutor eventPublishExecutor;
    
    @Autowired
    private RedisEventRepository          eventRepository; 
    
    @Autowired
    private PigeonConfigProperties pigeonConfigProperties;
    
    @Test
    public void testPublishEvent() {
        TestEvent event = new TestEvent();
        event.setId("1234");
        event.setName("sdff");
        event.setPwd("sdfdsf");
        event.setNotifyAddress("http://localhost:8080/event/test");
        boolean isSuccess = eventPublisher.publish(event);
        Assert.assertEquals(true, isSuccess);
    }
    
    @Test
    public void testExtractExceptionEvent() throws Exception
    {
        EventSubscriberConfig config = diamondSubseriberConfigRepository.getEventSubscriberConfig(2L);
        String jsonStr = "{\"@type\":\"com.github.pigeon.api.model.EventWrapper\",\"configId\":2,\"content\":{\"id\":\"TH13600740\",\"name\":453,\"pwd\":\"SUCCESS\",\"notifyAddress\":\"http://172.16.20.234:7777/merchant_system/notify\"},\"eventKey\":\"453\",\"eventType\":\"PaymentInstructionFinishedEvent\",\"retry\":true,\"sentTimes\":1,\"targetAddress\":\"testSpringListener\"}";
        EventWrapper event = JSON.parseObject(jsonStr, EventWrapper.class);
        
        publishExceptionHandler.handleException(config, event, EventSendResult.getFailResult("aa", true));
        TreeMap<String,String> map = eventRepository.extractExceptionalEvent(new Date().getTime(), DateUtil.addMinutes(new Date(),5 ).getTime(), 0, 10);
        
        String josnStr = map.get(event.genUniformEventKey());
        EventWrapper event2 = JSON.parseObject(josnStr, EventWrapper.class);
        Assert.assertEquals(event.genUniformEventKey(), event2.genUniformEventKey());
        Assert.assertEquals(event.getConfigId(), event2.getConfigId());
        Assert.assertEquals(event.getContent(), event2.getContent());
        Assert.assertEquals(event.getEventType(), event2.getEventType());
        Assert.assertEquals(event.getSendResult(), event2.getSendResult());
        Assert.assertEquals(event.getTargetAddress(), event2.getTargetAddress());
        Assert.assertEquals(event.getSentTimes(), event2.getSentTimes());
        eventRepository.delExceptionalEvent(event);
    }
    

    
    @Test
    public void testHandleExceptionEvent() throws Exception
    {
        String jsonStr = "{\"@type\":\"com.github.pigeon.api.model.EventWrapper\",\"configId\":2,\"content\":{\"id\":\"TH13600740\",\"name\":453,\"pwd\":\"SUCCESS\",\"notifyAddress\":\"http://172.16.20.234:7777/merchant_system/notify\"},\"eventKey\":\"453\",\"eventType\":\"PaymentInstructionFinishedEvent\",\"retry\":true,\"sentTimes\":1,\"targetAddress\":\"testSpringListener\"}";
        EventWrapper event = JSON.parseObject(jsonStr, EventWrapper.class);
        eventRepository.delExceptionalEvent(event);
        eventRepository.persistExceptionalEvent(event, new Date().getTime());
        publishExceptionHandler.handleExceptionQueue();
        TreeMap<String,String> map = eventRepository.extractExceptionalEvent(DateUtil.addSeconds(new Date(),-60).getTime(), new Date().getTime(), 0, 10);
        Assert.assertNull(map);
    }
    
    @Test
    public void testExtractNormalEvent() throws Exception
    {
        String jsonStr = "{\"@type\":\"com.github.pigeon.api.model.EventWrapper\",\"configId\":2,\"content\":{\"id\":\"TH13600740\",\"name\":453,\"pwd\":\"SUCCESS\",\"notifyAddress\":\"http://172.16.20.234:7777/merchant_system/notify\"},\"eventKey\":\"453\",\"eventType\":\"PaymentInstructionFinishedEvent\",\"retry\":true,\"sentTimes\":1,\"targetAddress\":\"testSpringListener\"}";
        EventWrapper event = JSON.parseObject(jsonStr, EventWrapper.class);
        eventRepository.persistEvent(event);

        TreeMap<String,String> map = eventRepository.extractEvent(new Date().getTime()-1000*6, new Date().getTime(), 0, 10);
        
        String josnStr = map.get(event.genUniformEventKey());
        EventWrapper event2 = JSON.parseObject(josnStr, EventWrapper.class);
        Assert.assertEquals(event.genUniformEventKey(), event2.genUniformEventKey());
        Assert.assertEquals(event.getConfigId(), event2.getConfigId());
        Assert.assertEquals(event.getContent(), event2.getContent());
        Assert.assertEquals(event.getEventType(), event2.getEventType());
        Assert.assertEquals(event.getSendResult(), event2.getSendResult());
        Assert.assertEquals(event.getTargetAddress(), event2.getTargetAddress());
        Assert.assertEquals(event.getSentTimes(), event2.getSentTimes());
        eventRepository.delEvent(event);
    }
    
    
    @Test
    public void testHandleNormalEvent() throws Exception
    {
        TestEvent content = new TestEvent();
        content.setId("sdfsdf");
        content.setName("sdfdsf");
        content.setNotifyAddress("sdfsdf");
        content.setPwd("dfdfds");
        EventSubscriberConfig config = diamondSubseriberConfigRepository.getEventSubscriberConfig(2L);
        EventWrapper event = eventPublisher.buildEventWrapper(content, config, content.getEventKey());
        eventRepository.delEvent(event);
        eventRepository.doCommond(event, DateUtil.addMinutes(new Date(), -20).getTime(), OprTypeEnum.persistEvent);
        eventPublishExecutor.recover();
        TreeMap<String,String> map = eventRepository.extractEvent(DateUtil.addMinutes(new Date(), -25).getTime(), new Date().getTime(), 0, 10);
        Assert.assertNull(map);
    }
    
    
    @Test
    public void testHandleNormalEventForLoop() throws Exception
    {
        TestEvent content = new TestEvent();
        content.setId("sdfsdf");
        content.setName("sdfdsf");
        content.setNotifyAddress("sdfsdf");
        content.setPwd("dfdfds");
        content.setEventKey(content.getId());
        EventSubscriberConfig config = diamondSubseriberConfigRepository.getEventSubscriberConfig(2L);
        EventWrapper event = eventPublisher.buildEventWrapper(content, config, content.getEventKey());
        eventRepository.delEvent(event);
        eventRepository.doCommond(event, DateUtil.addMinutes(new Date(), -20).getTime(), OprTypeEnum.persistEvent);
        
        content = new TestEvent();
        content.setId("sdfsdf2");
        content.setName("sdfdsf");
        content.setNotifyAddress("sdfsdf");
        content.setPwd("dfdfds");
        content.setEventKey(content.getId());
        event = eventPublisher.buildEventWrapper(content, config, content.getEventKey());
        eventRepository.delEvent(event);
        eventRepository.doCommond(event, DateUtil.addMinutes(new Date(), -20).getTime(), OprTypeEnum.persistEvent);
        
        
        eventPublishExecutor.recover();
        TreeMap<String,String> map = eventRepository.extractEvent(DateUtil.addMinutes(new Date(), -25).getTime(), new Date().getTime(), 0, 10);
        Assert.assertNull(map);
    }
    
    @Test
    public void testHandleExeceptionEventForLoop() throws Exception
    {
        pigeonConfigProperties.setRetryFetchCount(1);
        TestEvent content = new TestEvent();
        content.setId("sdfsdf");
        content.setName("sdfdsf");
        content.setNotifyAddress("sdfsdf");
        content.setPwd("dfdfds");
        content.setEventKey(content.getId());
        EventSubscriberConfig config = diamondSubseriberConfigRepository.getEventSubscriberConfig(2L);
        EventWrapper event = eventPublisher.buildEventWrapper(content, config, content.getEventKey());
        event.setSentTimes(1);
        eventRepository.delExceptionalEvent(event);
        eventRepository.doCommond(event, DateUtil.addMinutes(new Date(), -20).getTime(), OprTypeEnum.persistExceptionEvent);
        
        content = new TestEvent();
        content.setId("sdfsdf2");
        content.setName("sdfdsf");
        content.setNotifyAddress("sdfsdf");
        content.setPwd("dfdfds");
        content.setEventKey(content.getId());
        event = eventPublisher.buildEventWrapper(content, config, content.getEventKey());
        event.setSentTimes(1);
        eventRepository.delExceptionalEvent(event);
        eventRepository.doCommond(event, DateUtil.addMinutes(new Date(), -20).getTime(), OprTypeEnum.persistExceptionEvent);
        
        publishExceptionHandler.handleExceptionQueue();
        TreeMap<String,String> map = eventRepository.extractExceptionalEvent(DateUtil.addMinutes(new Date(), -25).getTime(), new Date().getTime(), 0, 10);
        Assert.assertNull(map);
    }
    
    
    
    
    
    
    
    public static class TestEvent implements DomainEvent
    {
        private String id;
        
        private String name;
        
        private String pwd;
        
        private String notifyAddress;
        
        
        private String eventKey; 
        

        public String getNotifyAddress() {
            return notifyAddress;
        }

        public void setNotifyAddress(String notifyAddress) {
            this.notifyAddress = notifyAddress;
        }

        @Override
        public String getEventKey() {
            return eventKey;
        }

        public void setEventKey(String eventKey) {
            this.eventKey = eventKey;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPwd() {
            return pwd;
        }

        public void setPwd(String pwd) {
            this.pwd = pwd;
        }
        
        
    }
    
}
