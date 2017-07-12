## 介绍
pegion是一个类似zeromq的消息中间件, 发送端和消费端点对点通讯, 但pegion会确保消息的可靠性。 

## 特性
1. 可靠事件: 事件发布者直跟消费者点对点通讯，消费者明确返回成功后才认为成功，否则pegion自动发起重试。 
2. 可扩展: 无单点服务器组件，pegion部署在发布者所在的客户端进程里，消息的发送处理能力可线性扩展。
3. 性能：pegion收到事件在持久化后立即返回，目前默认采用redis, 性能等于redis性能，毫秒级，且支持异步和非持久化模式。
4. pegion是一个spring boot start组件，spring boot项目增加jar依赖即可直接使用，默认情况下无需任何配置。
5. 事件订阅者可动态配置，支持以多种协议订阅事件，http, spring。
6. 领域事件，pegion的初衷就是为采用领域驱动设计的系统而设计的，你可以直接将领域事件发给pegion， pegion具备消息convert和分发到多消费者的能力，且支持同进程内的事件收发（spring协议）。

## 使用指南
1. 下载项目， maven构建后，在事件发布端系统中maven引入
3. 发送端代码
```
package com.github.pigeon.test;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.github.diamond.client.PropertiesConfiguration;
import com.github.pigeon.api.PigeonAutoConfiguration;
import com.github.pigeon.api.listeners.SpringEventListener;
import com.github.pigeon.api.model.DomainEvent;
import com.github.pigeon.api.model.EventSendResult;
import com.github.pigeon.test.BaseTest.InitConfiguration;

/**
 * 
 * 
 * @author liuhaoyong 2017年5月17日 下午3:54:22
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes={WebClientAutoConfiguration.class,PropertyPlaceholderAutoConfiguration.class,InitConfiguration.class,RedisAutoConfiguration.class,PigeonAutoConfiguration.class})
public class BaseTest {
    
    
    

    @Configuration
    public static class InitConfiguration {
        
        
        @Autowired
        private RestTemplateBuilder builder;

        @Value("${http.client.connect.timeout}")
        private int connectTimeout = 10 * 1000;

        @Value("${http.client.read.timeout}")
        private int readTimeout = 60 * 1000;


        /**
         * 构建一个rest模板对象，后续可以使用该对象直接访问远端的rest服务
         * 
         * @return
         */
        @Bean
        public RestTemplate initRestTemplate() {
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectTimeout)
                    .setConnectionRequestTimeout(connectTimeout).setSocketTimeout(readTimeout).build();
            CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
            return builder.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient)).build();
        }
        

        /**
         * 初始化配置中心服务
         * 
         * @return
         */
        @Bean
        public PropertiesConfiguration initDiamond() {
            
            PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration("172.16.20.234", 8283,
                    "payment-engine", "development");
            return propertiesConfiguration;
        }
        
        @Bean(name="testSpringListener")
        public SpringEventListener springEventListener()
        {
            SpringEventListener result = new SpringEventListener() {
                
                @Override
                public EventSendResult handleEvent(DomainEvent event) {
                    System.out.print("事件发送成功"+ToStringBuilder.reflectionToString(event));
                    return EventSendResult.getSuccessResult();
                }
            };
            return result;
        }
        
            
    }
    
}
```

```
public class EventPublisherTest  extends BaseTest{
    
    @Autowired
    private DomainEventPublisher eventPublisher;
    
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
}
```

2. 安装superdiamond，配置事件订阅者，增加事件订阅者配置的模块名，并在该模块下配置相关的订阅者
如一个合法的订阅者配置示例如下：
```
{"convertor":"defaultSpringProtocolConvertor","protocol":"spring","appName":"Test","maxRetryTimes":3,"targetAddress":"testSpringListener","eventType":"TestEvent","id":2}
```

## 更多
1. PigeonConfigProperties中定义了pigeon可配置的所有参数，详细了解见代码
2. 该项目实际上是一个spring boot start项目，如在spirng boot系统中可依赖进来就可以使用，其他系统需要略作修改

