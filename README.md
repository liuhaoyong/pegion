## 介绍
pegion是一个类似zermq的消息中间件, 发送端和消费端点对点通讯, 但pegion会确保消息的可靠性。 

## 特性
1. 可靠事件: 事件发布者直跟消费者点对点通讯，消费者明确返回成功后才认为成功，否则pegion自动发起重试。 
2. 可扩展: 无单点服务器组件，pegion部署在发布者所在的客户端进程里，消息的发送处理能力可线性扩展。
3. 性能：pegion收到事件在持久化后立即返回，目前默认采用redis, 性能等于redis性能，毫秒级，且支持异步和非持久化模式。
4. pegion是一个spring boot start组件，spring boot项目增加jar依赖即可直接使用，默认情况下无需任何配置。
5. 事件订阅者可动态配置，支持已多种协议订阅事件，http, spring。
6. 领域事件，特别适合领域驱动设计的系统，pegion的初衷就是为领域驱动设计的系统和设计的，所以你可以直接将领域事件发给pegion， pegion具备消息convertor和分发多消费者等能力，且支持同进程内的事件收发（spring协议）。

## 使用指南
1. 下载项目， maven构建后，在事件发布端系统中maven引入
3. 发送端代码
```
package com.tuhu.payment.engine.test.event;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.pigeon.api.DomainEventPublisher;
import com.github.pigeon.api.model.DomainEvent;
import com.tuhu.payment.engine.test.common.BaseTest

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
```

2. 安装superdiamond，配置事件订阅者，增加事件定于者模块名，并在该模块下配置一个相关的订阅者
如一个合法的值为
```
{"protocol":"HTTP","appName":"PAYMENT-ENGINE","masRetryTimes":3,
"targetAddress":"http://localhost:8080/event/test",
"eventType":"TestEvent",
"id":1,"convertor":"defaultHttpProtocolConvertor"}
```

## 更多
1. PigeonConfigProperties中定义了pigeon可配置的所有参数，详细了解见代码
2. 该项目实际上是一个spring boot start项目，如在spirng boot系统中可依赖进来就可以使用，其他系统需要略作修改

