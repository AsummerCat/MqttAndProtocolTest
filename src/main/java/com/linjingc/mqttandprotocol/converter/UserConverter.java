package com.linjingc.mqttandprotocol.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;

@Component
@Slf4j
public class UserConverter implements MqttMessageConverter {
    private int defaultQos = 0;
    private boolean defaultRetain = false;

    //消费者->入站消息解码
    @Override
    public AbstractIntegrationMessageBuilder<UserProto.User> toMessageBuilder(String topic, MqttMessage mqttMessage) {
        UserProto.User protocol = null;
        try {
            //反序列化
            protocol = UserProto.User.parseFrom(mqttMessage.getPayload());
        } catch (IOException e) {
            if (e instanceof JsonProcessingException) {
                System.out.println();
                log.error("Converter only support json string");
            }
        }
        assert protocol != null;
        MessageBuilder<UserProto.User> messageBuilder = MessageBuilder
                .withPayload(protocol);
        //使用withPayload初始化的消息缺少头信息，将原消息头信息填充进去
        messageBuilder.setHeader(MqttHeaders.ID, mqttMessage.getId())
                .setHeader(MqttHeaders.RECEIVED_QOS, mqttMessage.getQos())
                .setHeader(MqttHeaders.DUPLICATE, mqttMessage.isDuplicate())
                .setHeader(MqttHeaders.RECEIVED_RETAINED, mqttMessage.isRetained());
        if (topic != null) {
            messageBuilder.setHeader(MqttHeaders.TOPIC, topic);
        }
        return messageBuilder;
    }

    //生产者->出站消息编码
    @Override
    public Object fromMessage(Message<?> message, Class<?> targetClass) {
        MqttMessage mqttMessage = new MqttMessage();
        UserProto.User user = (UserProto.User) message.getPayload();
        //转换成字节数组
        byte[] msg = user.toByteArray();
        mqttMessage.setPayload(msg);

        //这里的 mqtt_qos ,和 mqtt_retained 由 MqttHeaders 此类得出不可以随便取，如需其他属性自行查找
        Integer qos = (Integer) message.getHeaders().get("mqtt_qos");
        mqttMessage.setQos(qos == null ? defaultQos : qos);
        Boolean retained = (Boolean) message.getHeaders().get("mqtt_retained");
        mqttMessage.setRetained(retained == null ? defaultRetain : retained);
        return mqttMessage;
    }

    //此方法直接拿默认编码器的来用的，照抄即可
    @Override
    public Message<?> toMessage(Object payload, MessageHeaders headers) {
        Assert.isInstanceOf(MqttMessage.class, payload,
                () -> "This converter can only convert an 'MqttMessage'; received: " + payload.getClass().getName());
        return this.toMessage(null, (MqttMessage) payload);
    }

    public void setDefaultQos(int defaultQos) {
        this.defaultQos = defaultQos;
    }

    public void setDefaultRetain(boolean defaultRetain) {
        this.defaultRetain = defaultRetain;
    }
}
