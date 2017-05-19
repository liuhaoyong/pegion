package com.github.pigeon.test;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.pigeon.api.DomainEventPublisher;
import com.github.pigeon.api.model.DomainEvent;


/**
 * 
 * 
 * @author liuhaoyong 2017年5月17日 下午7:12:30
 */
public class EventPublisherTest  extends BaseTest{
    
    @Autowired
    private DomainEventPublisher eventPublisher;
    
    
    @Test
    public void testPublishEvent() {
        TestEvent event = new TestEvent();
        event.setId("1234");
        event.setName("sdff");
        event.setPwd("sdfdsf");
        boolean isSuccess = eventPublisher.publish(event);
        Assert.assertEquals(true, isSuccess);
    }
    
    
    static class TestEvent implements DomainEvent
    {
        private String id;
        
        private String name;
        
        private String pwd;

        @Override
        public String getEventKey() {
            // TODO Auto-generated method stub
            return "12345678";
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
