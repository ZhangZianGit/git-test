package com.xxxx.seckill.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 消息发送者
 */
@Service
@Slf4j
public class MQSender {
    @Autowired
    private RabbitTemplate  rabbitTemplate;

//    //fanout模式
//    public void send(Object msg){
//        //发消息之前，打印消息
//        log.info("发送消息"+msg);
//        //发消息，往queue队列里发，发的是msg消息
//       // rabbitTemplate.convertAndSend("queue",msg);
//
//        //发消息，将消息发送到交换机
//        rabbitTemplate.convertAndSend("fanoutExchange","",msg);
//    }
//
//    //direct模式
//    //发送red消息
//    public void send01(Object msg){
//        log.info("发送red消息："+msg);
//        rabbitTemplate.convertAndSend("directExchange","queue.red",msg);
//    }
//
//    //发送green消息
//    public void send02(Object msg){
//        log.info("发送green消息："+msg);
//        rabbitTemplate.convertAndSend("directExchange","queue.green",msg);
//    }

    /**
     * 发送秒杀信息
     * @param message
     */
    public void sendSeckillMessage(String message){
        log.info("发送消息："+message);
        rabbitTemplate.convertAndSend("seckillExchange","seckill.message",message);
    }

}
