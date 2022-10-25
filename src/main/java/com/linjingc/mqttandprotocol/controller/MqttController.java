package com.linjingc.mqttandprotocol.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.linjingc.mqttandprotocol.converter.UserProto;
import com.linjingc.mqttandprotocol.mqtt.UserMqttProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MqttController {


    @Autowired
    private UserMqttProducer userMqttProducer;


    @RequestMapping("/user")
    public String send1() throws InvalidProtocolBufferException {
        UserProto.User.Builder user = UserProto.User.newBuilder();
        user.setId(100).setCode("10086").setName("小明").build();
        //序列化
        UserProto.User build = user.build();
        userMqttProducer.sendToMqtt("user", build);
        return "send message : " + JsonFormat.printer().print(build);
    }


}
