package com.linjingc.mqttandprotocol.mqtt;

import com.linjingc.mqttandprotocol.converter.User;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 生产者发送消息
 */

//@MessagingGateway是一个用于提供消息网关代理整合的注解，参数defaultRequestChannel指定发送消息绑定的channel。
@MessagingGateway(defaultRequestChannel = "userMqttOutboundChannel")
@Component
@ConditionalOnProperty(value = "spring.mqtt.enable", havingValue = "true")
public interface UserMqttProducer {
    //自定义编码数据
    void sendToMqtt(User user);

    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, User user);

    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, @Header(MqttHeaders.QOS) Integer Qos, User user);
}