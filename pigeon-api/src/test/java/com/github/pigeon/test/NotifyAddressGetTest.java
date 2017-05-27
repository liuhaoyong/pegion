package com.github.pigeon.test;

import org.junit.Assert;
import org.junit.Test;

import com.github.pigeon.api.convertor.DefaultHttpProtocolConvertor;
import com.github.pigeon.api.model.EventSubscriberConfig;
import com.github.pigeon.test.EventPublisherTest.TestEvent;


/**
 * 
 * 
 * @author liuhaoyong 2017年5月27日 上午10:55:47
 */
public class NotifyAddressGetTest {
    
    @Test
    public void testVelocity()
    {
        DefaultHttpProtocolConvertor convertor = new DefaultHttpProtocolConvertor();
        
        TestEvent event = new TestEvent();
        event.setNotifyAddress("http://localhost:8080/event/test");
        
        EventSubscriberConfig config = new EventSubscriberConfig();
        config.setTargetAddress("${event.notifyAddress}");
        
        String targetAddress = convertor.getTargetAddress(event, config);
        System.out.println(targetAddress);
        Assert.assertEquals("http://localhost:8080/event/test", targetAddress);
        
        config.setTargetAddress("http://localhost:8080/event/test2");
        targetAddress = convertor.getTargetAddress(event, config);
        System.out.println(targetAddress);
        Assert.assertEquals("http://localhost:8080/event/test2", targetAddress);
        
        
        
    }
}
