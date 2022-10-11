package com.linjingc.mqttandprotocol.config;

import com.linjingc.mqttandprotocol.converter.UserConverter;
import com.linjingc.mqttandprotocol.mqtt.MqttConsumer;
import com.linjingc.mqttandprotocol.mqtt.UserMqttConsumer;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;


@Configuration
@IntegrationComponentScan
@ConditionalOnProperty(value = "spring.mqtt.enable", havingValue = "true")
@Slf4j
public class MqttConfig {
    @Value("${spring.mqtt.username}")
    private String username;

    @Value("${spring.mqtt.password}")
    private String password;

    @Value("${spring.mqtt.url}")
    private String hostUrl;

    @Value("${spring.mqtt.producerclientid}")
    private String producerClientId;
    @Value("${spring.mqtt.userproducerclientid}")
    private String userProducerClientId;

    @Value("${spring.mqtt.producertopic}")
    private String producerTopic;

    //生产者和消费者是单独连接服务器会使用到一个clientid（客户端id），如果是同一个clientid的话会出现Lost connection: 已断开连接; retrying...
    @Value("${spring.mqtt.consumerclientid}")
    private String consumerClientId;
    @Value("${spring.mqtt.userconsumerclientid}")
    private String userConsumerclientid;

    @Value("${spring.mqtt.consumertopic}")
    private String[] consumerTopic;

    @Value("${spring.mqtt.timeout}")
    private int timeout;   //连接超时

    @Value("${spring.mqtt.keepalive}")
    private int keepalive;  //连接超时


    @Autowired
    /**
     * 自定义编码器
     */
    private UserConverter userConverter;


    /**
     * MQTT客户端
     *
     * @return {@link org.springframework.integration.mqtt.core.MqttPahoClientFactory}
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        //MQTT连接器选项
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());
        mqttConnectOptions.setServerURIs(new String[]{hostUrl});
        mqttConnectOptions.setKeepAliveInterval(keepalive);
        factory.setConnectionOptions(mqttConnectOptions);
        return factory;
    }

    /*******************************生产者*******************************************/

    /**
     * MQTT信息通道（生产者）
     *
     * @return {@link MessageChannel}
     */
    @Bean(name = "mqttOutboundChannel")
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT消息处理器（生产者）
     *
     * @return
     */
    @Bean
    //出站通道名（生产者）
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(producerClientId, mqttClientFactory());
        //如果设置成true，发送消息时将不会阻塞。
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(producerTopic);
        return messageHandler;
    }

    /*******************************消费者*******************************************/

    /**
     * MQTT信息通道（消费者）
     *
     * @return {@link MessageChannel}
     */
    @Bean(name = "mqttInboundChannel")
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT消息订阅绑定（消费者）
     *
     * @return {@link org.springframework.integration.core.MessageProducer}
     */
    @Bean
    public MessageProducer inbound() {
        // 可以同时消费（订阅）多个Topic
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(consumerClientId, mqttClientFactory(), consumerTopic);
        adapter.setCompletionTimeout(timeout);
        //默认编码器
        adapter.setConverter(new DefaultPahoMessageConverter());
//        自定义编码器
//        adapter.setConverter(userConverter);

        //qos是mqtt 对消息处理的几种机制分为0,1,2
        // 其中0表示的是订阅者没收到消息不会再次发送,消息会丢失,
        // 1表示的是会尝试重试,一直到接收到消息,但这种情况可能导致订阅者收到多次重复消息,
        // 2相比多了一次去重的动作,确保订阅者收到的消息有一次
        // 当然,这三种模式下的性能肯定也不一样,qos=0是最好的,2是最差的
        adapter.setQos(1);
        // 设置订阅通道
        adapter.setOutputChannel(mqttInboundChannel());
        return adapter;
    }

    /**
     * MQTT消息处理器（消费者）
     * ServiceActivator注解表明当前方法用于处理MQTT消息，inputChannel参数指定了用于接收消息信息的channel。
     *
     * @return {@link MessageHandler}
     */
    @Bean
    //入站通道名（消费者）
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public MessageHandler handler() {
        return new MqttConsumer();
    }


    /*******************************自定义编码生产者*******************************************/

    @Bean(name = "userMqttOutboundChannel")
    public MessageChannel userMqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT消息处理器（生产者）
     *
     * @return
     */
    @Bean
    //出站通道名（生产者）
    @ServiceActivator(inputChannel = "userMqttOutboundChannel")
    public MessageHandler userMqttOutbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(userProducerClientId, mqttClientFactory());
        //如果设置成true，发送消息时将不会阻塞。
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(producerTopic);
        messageHandler.setConverter(userConverter);
        return messageHandler;
    }

    /*******************************自定义编码消费者*******************************************/

    @Bean(name = "userMqttInboundChannel")
    public MessageChannel userMqttInboundChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT消息订阅绑定（消费者）
     *
     * @return {@link org.springframework.integration.core.MessageProducer}
     */
    @Bean
    public MessageProducer userInbound() {
        // 可以同时消费（订阅）多个Topic
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(userConsumerclientid, mqttClientFactory(), "user");
        adapter.setCompletionTimeout(timeout);
        //自定义编码器
        adapter.setConverter(userConverter);
        adapter.setQos(1);
        // 设置订阅通道
        adapter.setOutputChannel(userMqttInboundChannel());
        return adapter;
    }

    @Bean
    //入站通道名（消费者）
    @ServiceActivator(inputChannel = "userMqttInboundChannel")
    public MessageHandler userHandler() {
        return new UserMqttConsumer();
    }
}
 