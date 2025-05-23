package com.xxxx.seckill.rabbitmq;

import com.xxxx.seckill.pojo.Order;
import com.xxxx.seckill.pojo.SeckillMessage;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IOrderService;
import com.xxxx.seckill.utils.JsonUtil;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.RespBean;
import com.xxxx.seckill.vo.RespBeanEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 消息消费者
 */
@Service
@Slf4j
public class MQReceiver {
//
//    @RabbitListener(queues = "queue")
//    public void receive(Object msg){
//        log.info("接受消息："+msg);
//    }
//
//    @RabbitListener(queues = "queue_fanout01")
//    public void receive01(Object msg){
//        log.info("QUEUE01接受消息："+msg);
//    }
//
//    @RabbitListener(queues = "queue_fanout02")
//    public void receive02(Object msg){
//        log.info("QUEUE02接受消息："+msg);
//    }
//
//    @RabbitListener(queues = "queue_direct01")
//    public void receive03(Object msg){
//        log.info("QUEUE01接受消息："+msg);
//    }
//
//    @RabbitListener(queues = "queue_direct02")
//    public void receive04(Object msg){
//        log.info("QUEUE02接受消息："+msg);
//    }

    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IOrderService orderService;
    /**
     * 接受到消息后，进行下单的操作
     * @param message
     */
    @RabbitListener(queues = "seckillQueue")
    public void receive(String message){
        log.info("接受消息："+message);
        //将message由json字符串转换成对象,拿到秒杀消息
        SeckillMessage seckillMessage = JsonUtil.jsonStr2Object(message, SeckillMessage.class);
        //获取到user和goodId
        Long goodId = seckillMessage.getGoodId();
        User user = seckillMessage.getUser();
        //根据goodId获取商品对象，然后去判断库存
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodId);
        //判断库存,若库存不够，直接返回
        if (goodsVo.getStockCount()<1){
            return;
        }
        //判断是否重复抢购
        //判断该秒杀订单是否已存在
        SeckillOrder seckillOrder =
                (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodId);

        //如果已经存在，则直接返回
        if(seckillOrder !=null){
            return;
        }

        //开始秒杀下单
         orderService.seckill(user, goodsVo);


    }
}
