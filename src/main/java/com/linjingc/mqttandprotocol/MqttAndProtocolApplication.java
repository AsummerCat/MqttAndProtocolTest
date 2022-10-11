package com.linjingc.mqttandprotocol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class MqttAndProtocolApplication {

    public static void main(String[] args) {
        log.info("MQTT连接启动....");
        SpringApplication.run(MqttAndProtocolApplication.class, args);
    }

}
