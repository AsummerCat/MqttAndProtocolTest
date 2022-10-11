package com.linjingc.mqttandprotocol.controller;

import com.alibaba.fastjson.JSON;
import com.linjingc.mqttandprotocol.mqtt.MqttProducer;
import com.linjingc.mqttandprotocol.converter.User;
import com.linjingc.mqttandprotocol.mqtt.UserMqttProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MqttController {

    @Autowired
    private MqttProducer mqttProducer;
    @Autowired
    private UserMqttProducer userMqttProducer;

    @RequestMapping("/send/{topic}/{message}")
    public String send(@PathVariable String topic, @PathVariable String message) {
        mqttProducer.sendToMqtt(topic, message);
        return "send message : " + message;
    }


    @RequestMapping("/user")
    public String send1() {
        User user = new User();
        user.setUsername("小明");
        user.setPassword("密码");
        userMqttProducer.sendToMqtt("user", user);
        return "send message : " + JSON.toJSONString(user);
    }


}
