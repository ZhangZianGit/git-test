package com.xxxx.seckill.controller;

import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IUserService;
import com.xxxx.seckill.vo.DetailVo;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import sun.dc.pr.PRError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 跳转到商品页面
 */
@Controller
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private IUserService userService;
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver; //Thymeleaf中可以做手动渲染

    /**
     * 跳转商品列表（版本一）
     */
//    @RequestMapping("/toList")
//    public String toList(HttpServletRequest request, HttpServletResponse response, Model model, @CookieValue("userTicket") String ticket){
//      //如果cookie为空，跳转登录页面
//       if (StringUtils.isEmpty(ticket)){
//           return "login";
//       }
//       //如果cooike不为空，则去session获取用户信息
//       // User user = (User) session.getAttribute(ticket);
//
//        //根据ticket获取存在于redis中的user信息
//        User user = userService.getUserByCookie(ticket, request, response);
//        if (null==user){
//           return "login";
//       }
//
//       //如果都没问题,将User对象传到前端页面
//        model.addAttribute("user",user);
//
//       //跳转到商品页面
//        return "goodsList";
//    }


    /**
     * 跳转商品列表（版本二：加了UserArgumentResolver，统一获取当前用户）
     */
//    @RequestMapping("/toList")
//    public String toList( Model model, User user){
//        /**
//         * 压测：50000个线程，测试3次结果：
//         * windows优化前QPS:1224
//         * linux优化前QPS:1320
//         */
//
//        //如果都没问题,将User对象传到前端页面
//        model.addAttribute("user",user);
//        model.addAttribute("goodsList", goodsService.findGoodsVo());
//
//        //跳转到商品页面
//        return "goodsList";
//    }

    /**
     * 跳转商品列表（版本三：页面缓存优化）
     *
     * 先从redis中读取缓存，如果发现了页面，直接返回给浏览器，
     * 如果没有，则手动渲染模板，将它存在redis中，并且把结果
     * 输出到浏览器端
     *
     *          压测：50000个线程，测试3次结果：
     *          windows优化前QPS:1224
     *          windows优化后QPS:8181
     *
     *
     * produces的作用是指定返回值类型和返回值编码
     */
    @RequestMapping(value = "/toList",produces = "text/html;charset=utf-8")
    @ResponseBody
    public String toList( Model model, User user,HttpServletRequest request, HttpServletResponse response){
        //Redis中获取页面，如果不为空，直接返回页面
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsList");
        if (!StringUtils.isEmpty(html)){
            return html;
        }
        //如果都没问题,将User对象传到前端页面
        model.addAttribute("user",user);
        model.addAttribute("goodsList", goodsService.findGoodsVo());

        //如果为空，手动渲染，并存入redis，并且返回
        WebContext context = new WebContext(request,response,request.getServletContext(),request.getLocale(),model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsList", context);
        if (!StringUtils.isEmpty(html)){
            //将页面加入redis缓存，并且设置过期时间为1分钟
            valueOperations.set("goodsList",html,60, TimeUnit.SECONDS);
        }
        return html;
    }

     /**
     * 跳转商品详情（版本一）
     */
//    @RequestMapping("/toDetail/{goodsId}")
//    public String toDetail(Model model,User user,@PathVariable Long goodsId){
//        model.addAttribute("user",user);
//        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
//        ////获取秒杀开始时间和秒杀结束时间
//        Date startDate = goodsVo.getStartDate();
//        Date endDate= goodsVo.getEndDate();
//        //获取当前时间
//        Date nowDate = new Date();
//        //秒杀状态
//        int seckillStatus = 0;
//        //秒杀倒计时
//        int remainSeconds = 0;
//        //秒杀还没开始
//        if(nowDate.before(startDate)){
//            remainSeconds = (int)((startDate.getTime() - nowDate.getTime())/1000);
//
//        }else if (nowDate.after(endDate)){
//            //秒杀结束
//            seckillStatus = 2;
//            remainSeconds = -1;
//        }else{
//            //秒杀中
//            seckillStatus = 1;
//            remainSeconds = 0;
//        }
//
//        model.addAttribute("remainSeconds",remainSeconds);
//        model.addAttribute("seckillStatus",seckillStatus);
//        model.addAttribute("goods",goodsVo);
//        return "goodsDetail";
//
//    }

//    /**
//     *  跳转商品详情（版本二）
//     *  URL缓存，针对不同商品id进入不同的页面进行缓存
//     */
//    @RequestMapping(value = "/toDetail/{goodsId}",produces = "text/html;charset=utf-8")
//    @ResponseBody
//    public String toDetail(Model model,User user,@PathVariable Long goodsId,HttpServletRequest request, HttpServletResponse response) {
//        ValueOperations valueOperations = redisTemplate.opsForValue();
//        String html = (String) valueOperations.get("goodsDetail:" + goodsId);
//        if (!StringUtils.isEmpty(html)) {
//            return html;
//        }
//
//        model.addAttribute("user", user);
//        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
//        ////获取秒杀开始时间和秒杀结束时间
//        Date startDate = goodsVo.getStartDate();
//        Date endDate = goodsVo.getEndDate();
//        //获取当前时间
//        Date nowDate = new Date();
//        //秒杀状态
//        int seckillStatus = 0;
//        //秒杀倒计时
//        int remainSeconds = 0;
//        //秒杀还没开始
//        if (nowDate.before(startDate)) {
//            remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
//
//        } else if (nowDate.after(endDate)) {
//            //秒杀结束
//            seckillStatus = 2;
//            remainSeconds = -1;
//        } else {
//            //秒杀中
//            seckillStatus = 1;
//            remainSeconds = 0;
//        }
//
//        model.addAttribute("remainSeconds", remainSeconds);
//        model.addAttribute("seckillStatus", seckillStatus);
//        model.addAttribute("goods", goodsVo);
//
//        WebContext context = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
//        html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail", context);
//        if (!StringUtils.isEmpty(html)) {
//            //将页面加入redis缓存，并且设置过期时间为1分钟
//            valueOperations.set("goodsDetail:商品" + goodsId, html, 60, TimeUnit.SECONDS);
//        }
//        return html;
//    }
    /**
     *  商品详情页面静态化
     */
    @RequestMapping(value = "/toDetail/{goodsId}")
    @ResponseBody
    public RespBean toDetail(HttpServletRequest request,HttpServletResponse response,Model model, User user, @PathVariable Long goodsId){

        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        ////获取秒杀开始时间和秒杀结束时间
        Date startDate = goodsVo.getStartDate();
        Date endDate= goodsVo.getEndDate();
        //获取当前时间
        Date nowDate = new Date();
        //秒杀状态
        int seckillStatus = 0;
        //秒杀倒计时
        int remainSeconds = 0;
        //秒杀还没开始
        if(nowDate.before(startDate)){
            remainSeconds = (int)((startDate.getTime() - nowDate.getTime())/1000);

        }else if (nowDate.after(endDate)){
            //秒杀结束
            seckillStatus = 2;
            remainSeconds = -1;
        }else{
            //秒杀中
            seckillStatus = 1;
            remainSeconds = 0;
        }

        DetailVo detailVo = new DetailVo();
        detailVo.setUser(user);
        detailVo.setGoodsVo(goodsVo);
        detailVo.setSeckillStatus(seckillStatus);
        detailVo.setRemainSeconds(remainSeconds);
        return RespBean.success(detailVo);

    }

}
