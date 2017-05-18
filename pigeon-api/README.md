
# 一、Change log
## dev-kafka-20161226
1. Intercepter 优化
2. 支持kafka协议

## dev-optimize-20161208
1. 程序优化，提高性能，较大的版本更新
2. 修改了若干bug

## dev-20160920
1. 支持根据系统名称来区分时间分发
	- 通过字段app_name来区分各个系统订阅的时间
	- dubbo协议的时间app_name理论上不起作用，spring协议默认都是系统自身发送自身接受
	- convertor的话则是跟着发送方的app_name,所以理论上目前不支持非dubbo协议自定义Convertor，否则会悲剧


------------------------------------------------------------------------

# 二、使用指南

各业务线通知功能对失败重试、消息发送没有统一的代码实现，各自实现，代码冗余。

## 注意 本系统保证消息至少收到一个消息,订阅方业务一定要自己保证幂等

我们的目标：


一期
-   保持之前的架构实现
-	统一的失败重试实现
-	消息发送可靠性，保证消息定义的规范下一定送到
-   支持强类型协议如:dubbo协议、以及当前jvm内调用

二期
-   支持非强类型协议如: kafka协议
-   消息encode、decode

# 使用方法 #
## 发送方和接受方的共同依赖,依赖简化

1. 依赖

		<dependency>
 			<groupId>com.wacai.payment</groupId>
 			<artifactId>pigeon-api</artifactId>
  			<version>XXXX</version>
		</dependency>

		同事需要引入配置文件
		classpath:spring/spring-pigeon-config.xml

## 发布事件
1. 配置

		#定义你自己的name,必填
		application.name＝your-application
		# 事件发送动作的 线程池配置,选填，默认core为5 max为10
		pigeon.eventSent.maxPoolSize=2
        pigeon.eventSent.corePoolSize=1
		# 事件发送动作的 线程池配置,选填，默认core为5 max为10
        pigeon.eventPublish.maxPoolSize=4
        pigeon.eventPublish.corePoolSize=3

2. 调用接口  (此方法不会抛exception,需要关注返回值）

		@Autowired
    	@Qualifier("eventPublisher")
    	private EventPublisher eventPublisher;

		boolean result  = eventPublisher.publish(new YourEvent(11l));  //YourEvent implements Event


## 订阅事件
1. 配置及使用

### Spring协议
		1. 在表wac_pay.subscribe_config里面配置订阅方信息
		   targetAddress字段应配置为 targetAddress

		2. 根据不同的协议实现不同的接口
		    ISpringBizEventListener handleEvent(Event event)

		3. 在你的实现类里面处理相应逻辑

### Dubbo协议
		1. 在表wac_pay.subscribe_config里面配置订阅方信息
		   DUBBO协议跟其它协议有点不同，targetAddress字段应配置为 your-group:your-targetAddress

		2. 根据不同的协议实现不同的接口
			注意：返回值EventSendResult里的canRetry需要业务自己定义返回给本系统
			EventSendResult IDubboBizEventExListener handleEvent(T event) //T可以为自定义的类 只要字段名一致即可
			EventSendResult IDubboBizEventListener  handleEvent(Map<String, String> params) //兼容老接口
			注意:
			   实现类必须显示标明bean的名字 如 @Service("testService)
			   此处的 testServcie 要跟wac_pay.subscribe_config里字段targetAddress的值对应

		3. 在 application.properties里面加入以下值
			pigeon.group.name=your-application-group #一般用application的名字

		4. copy以下语句到你的配置文件里面
			<dubbo:service interface="com.wacai.payment.pigeon.api.subscriber.IEventListener" ref="ieventListener" group="${pigeon.group.name}"></dubbo:service>
		5. 在你的实现类里面处理相应逻辑



# 设计 #


