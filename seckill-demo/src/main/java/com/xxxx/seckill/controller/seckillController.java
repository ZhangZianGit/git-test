package com.xxxx.seckill.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wf.captcha.ArithmeticCaptcha;
import com.xxxx.seckill.config.AccessLimit;
import com.xxxx.seckill.exception.GlobalException;
import com.xxxx.seckill.pojo.Order;
import com.xxxx.seckill.pojo.SeckillMessage;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.rabbitmq.MQSender;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IOrderService;
import com.xxxx.seckill.service.ISeckillOrderService;
import com.xxxx.seckill.utils.JsonUtil;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.RespBean;
import com.xxxx.seckill.vo.RespBeanEnum;
import javafx.scene.chart.ValueAxis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * 秒杀
 */
@Slf4j
@Controller
@RequestMapping("/seckill")
public class seckillController implements InitializingBean{

    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private ISeckillOrderService seckillOrderService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender mqSender;
    @Autowired
    private RedisScript<Long> script;

    private Map<Long,Boolean> EmptyStockMap = new HashMap<>();

    /**
     * 秒杀（版本1，没有静态化）
     * 压测：50000个线程，测试3次结果：
     * windows优化前QPS:514
     * windows优化后QPS:1977
     * linux优化前QPS:1812
     */
    @RequestMapping("/doSeckill2")
    public String doSeckill2(Model model, User user,Long goodsId){
        //判断用户，如果用户为空，跳到登录页面
           if(user==null){
               return "login";
           }
           model.addAttribute("user",user);

           //查询库存
        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
        if (goods.getStockCount()<1){
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "secKillFail";
        }

        //判断是否重复抢购
//        QueryWrapper<SeckillOrder> wrapper = new QueryWrapper<>();
//        wrapper.eq("user_id",user.getId()).eq("goods_id",goodsId);
 //       SeckillOrder seckillOrder = seckillOrderService.getOne(wrapper);

        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:"+user.getId()+":"+goodsId);
        if (seckillOrder != null) {
            model.addAttribute("errmsg",RespBeanEnum.REPEATE_ERROR.getMessage());
            return "secKillFail";
        }

        //满足条件开始抢购
       Order order = orderService.seckill(user,goods);
        model.addAttribute("order",order);
        model.addAttribute("goods",goods);
        return "orderDetail";


    }

    /**
     * 秒杀（版本2，静态化）
     * 相较于版本一：将秒杀订单放入redis，优化了重复判断的数据库操作
     */
    @RequestMapping(value = "/doSeckill3",method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSeckill3(Model model, User user,Long goodsId){
        //判断用户，如果用户为空，跳到登录页面
        if(user==null){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        //查询库存
        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
        if (goods.getStockCount()<1){
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        //如果库存够，则通过订单id和用户id在redis查询是否存在该订单
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:"+user.getId()+":"+goodsId);
        if (seckillOrder != null) {
            return RespBean.error(RespBeanEnum.REPEATE_ERROR);
        }

        //满足条件开始抢购
        Order order = orderService.seckill(user,goods);
        return RespBean.success(order);

    }

    /**
     * 秒杀（版本3，静态化+Redis预减库存）
     * 相较于版本二，Redis预减库存的优点：
     * 1.优化查询库存的数据库操作
     * 2.优化生成订单的数据库操作
     * 3.优化生成秒杀订单的数据库操作
     *
     *     windows优化前QPS:514
     *     windows优化后QPS:1977（将用户订单存入redis）
     *     windows优化后QPS:7709（使用RabbitMQ异步下单）
     */
    @RequestMapping(value = "/{path}/doSeckill",method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSeckill(@PathVariable String path, User user, Long goodsId){
        //判断用户，如果用户为空，跳到登录页面
        if(user==null){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        //通过Redis操作
        ValueOperations valueOperations = redisTemplate.opsForValue();
       Boolean check =  orderService.checkPath(user,goodsId,path);
       if (!check){
           return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
       }

        //判断是否重复抢购
        //判断该秒杀订单是否已存在
        SeckillOrder seckillOrder =
                (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);

        //如果已经存在，则返回错误信息，提示重复抢购
        if(seckillOrder !=null){
            return RespBean.error(RespBeanEnum.REPEATE_ERROR);
        }

        //内存标记，减少Redis的访问
        if (EmptyStockMap.get(goodsId)){
            //如果EmptyStockMap是true，则代表该商品库存为空
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        //预减库存
        //1.先把库存加载到redis（afterPropertiesSet方法）
        //2.在redis中预减库存操作（获取到递减之后的库存），也可以使用Redis分布式锁
        //在redis中预减库存
        Long stock = valueOperations.decrement("seckillGoods:" + goodsId);
        //或者用Redis分布式锁
       // Long stock = (Long) redisTemplate.execute(script, Collections.singletonList("seckillGoods:" + goodsId), Collections.EMPTY_LIST);

        if (stock<0){
            EmptyStockMap.put(goodsId,true);
            valueOperations.increment("seckillGoods:" + goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        //3.下单(user和商品由RabbitMQ提供)
        //创建一个秒杀信息对象
        SeckillMessage seckillMessage = new SeckillMessage(user,goodsId);
        //用RabbitMQ发送消息
        //将seckillMessage由对象转换成json字符串
        mqSender.sendSeckillMessage(JsonUtil.object2JsonStr(seckillMessage));
        //返回0，代表排队中
        return RespBean.success(0);
    }


    /**
     *  获取秒杀结果
     * @param user
     * @param goodsId:如果goodsId还有：成功，
     *               -1：秒杀失败
     *               0：排队中
     * @return
     */
    @RequestMapping(value = "/result",method = RequestMethod.GET)
    @ResponseBody
    public RespBean getResult(User user,Long goodsId){
        if (user == null){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        Long orderId = seckillOrderService.getResult(user,goodsId);
        return RespBean.success(orderId);
    }

    /**
     * 获取秒杀地址
     * @param user
     * @param goodsId
     * @return
     */
    //5s内访问超过5次，就会提示访问过于频繁
    @AccessLimit(second=5,maxCount=5,needLogin=true)
    @RequestMapping(value ="/path",method = RequestMethod.GET)
    @ResponseBody
    public RespBean getPath(User user, Long goodsId, String captcha, HttpServletRequest request){
        if (user==null){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        /**
         * 限流防刷，用计数器
         */
//        ValueOperations valueOperations = redisTemplate.opsForValue();
//        String uri = request.getRequestURI();
//        //限制访问次数，60s内访问5次，设验证码为0，方便测试
//        //captcha = "0";
//        Integer count = (Integer) valueOperations.get(uri + ":" + user.getId());
//        if (count==null){
//            //如果count为空，说明是第一输入验证码
//            valueOperations.set(uri + ":" + user.getId(),1,60,TimeUnit.SECONDS);
//        }else if (count<5){
//            //如果不是第一次就递增
//            valueOperations.increment(uri + ":" + user.getId());
//        }else{
//            //如果大于5，返回错误信息提示
//            return RespBean.error(RespBeanEnum.ACCESS_LIMIT_REAHCED);
//        }


        //验证码校验
        Boolean check = orderService.checkCaptcha(user,goodsId,captcha);
        if (!check){
            return RespBean.error(RespBeanEnum.ERROR_CAPTCHA);
        }
        String str = orderService.createPath(user,goodsId);
        return RespBean.success(str);
    }

    /**
     * 验证码
     */

    @RequestMapping(value = "/captcha",method = RequestMethod.GET)
    public void verifyCode(User user, Long goodsId, HttpServletResponse response){
        if (user==null||goodsId<0){
            throw new GlobalException(RespBeanEnum.REQUEST_ILLEGAL);
        }
        //设置请求头为输出图片的类型
        response.setContentType("image/jpg");
        response.setHeader("Pargam","No-cache");
        response.setHeader("Cache-Control","no-cache");
        response.setDateHeader("Expires",0); //失效时间，永不失效

        //生成验证码，将结果放入redis
        // 算术类型
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32,3);
        redisTemplate.opsForValue().set("captcha:"+user.getId()+":"+goodsId,captcha.text(),300, TimeUnit.SECONDS);
        try {
            captcha.out(response.getOutputStream());  // 输出验证码
        } catch (IOException e) {
           log.error("验证码生成失败",e.getMessage());
        }

    }

    /**
     * 初始化，在项目启动的时候把商品库存数量加载到redis
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> list = goodsService.findGoodsVo();
        if (CollectionUtils.isEmpty(list)){
            return;
        }
        list.forEach(goodsVo ->{
            redisTemplate.opsForValue().set("seckillGoods:"+goodsVo.getId(),goodsVo.getStockCount());
            //有库存，false
            EmptyStockMap.put(goodsVo.getId(),false);
                }

        );

    }
}
