package com.github.pigeon.test;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.github.pigeon.api.model.EventSendResult;
import com.github.pigeon.test.BaseTest.InitConfiguration;

/**
 * 
 * 
 * @author liuhaoyong 2017年5月17日 下午3:54:22
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes={WebClientAutoConfiguration.class,InitConfiguration.class,RedisAutoConfiguration.class,PigeonAutoConfiguration.class})
public class BaseTest {
    
    
    

    @Configuration
    public static class InitConfiguration {
        
        
        @Autowired
        private RestTemplateBuilder builder;

        @Value("${http.client.connect.timeout:10000}")
        private int connectTimeout = 10 * 1000;

        @Value("${http.client.read.timeout:60000}")
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
                public EventSendResult handleEvent(String event) {
                    System.out.print("事件处理成功:" +event);
                    return EventSendResult.getSuccessResult();
                }
            };
            return result;
        }
        
        
        
            
    }
    
}
