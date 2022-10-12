package com.linjingc.mqttandprotocol.config;

import com.linjingc.mqttandprotocol.converter.UserConverter;
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
 