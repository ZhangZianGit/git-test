package com.xxxx.seckill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxxx.seckill.exception.GlobalException;
import com.xxxx.seckill.mapper.UserMapper;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IUserService;
import com.xxxx.seckill.utils.CookieUtil;
import com.xxxx.seckill.utils.MD5Utils;
import com.xxxx.seckill.utils.UUIDUtil;
import com.xxxx.seckill.vo.LoginVo;
import com.xxxx.seckill.vo.RespBean;
import com.xxxx.seckill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zzr
 * @since 2021-12-26
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 登录功能
     * @param loginVo
     * @param request
     * @param response
     * @return
     */
    @Override
    public RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response) {
        String mobile = loginVo.getMobile();
        String password = loginVo.getPassword();
        //判断是否为空
//        if (StringUtils.isEmpty(mobile) || StringUtils.isEmpty(password)){
//            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
//        }
//
//        //如果电话校验不成功
//        if (!(ValidatorUtil.isMobile(mobile))){
//             return  RespBean.error(RespBeanEnum.MOBILE_ERROR);
//        }

        //1.若输入的手机号或密码不为空
        //2.如电话格式正确
        //3.继续在数据库中查询,根据ID查询用户
        User user = userMapper.selectById(mobile);
        //4.若用户不存在，则返回错误信息
        if (null==user){
            // return RespBean.error(RespBeanEnum.LOGIN_ERROR);
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }
        //5.若user不为空，就去校验密码是否正确，
        if (!MD5Utils.formPassToDBPass(password,user.getSalt()).equals(user.getPassword())){
            //如果密码错误，返回错误信息
            //return  RespBean.error(RespBeanEnum.LOGIN_ERROR);
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }
        //6.生成cookie
        String ticket = UUIDUtil.uuid();
        //7.将cookie和对应的用户信息存储在redis中
        redisTemplate.opsForValue().set("user:"+ticket,user);
        //使用session来存储cookie和user
       // request.getSession().setAttribute(ticket,user);

        //8.设置Cookie的值 不设置生效时间默认浏览器关闭即失效,也不编码
        CookieUtil.setCookie(request,response,"userTicket",ticket);

        //9.返回成功信息
        return RespBean.success(ticket);
    }

    @Override
    public User getUserByCookie(String userTicket,HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isEmpty(userTicket)){
            return null;
        }

        User user = (User) redisTemplate.opsForValue().get("user:" + userTicket);

        if (user!=null){
            CookieUtil.setCookie(request,response,"userTicket",userTicket);
        }
        return user;
    }

    /**
     * 用户更新密码
     * 问题：用户密码存储在redis中，没有设置过期时间，永不失效，如果用户修改了密码，用户拿到的还是旧密码
     * 如何进行处理保证redis中的信息和数据库中的信息的一致性？
     * 解决：每次对数据库进行操作的时候，直接把redis中的数据清空，当用户登录时，会先从数据库获取数据，
     * 获得最新的用户登录的信息，最后会把更新的用户信息重新存入redis中
     */
    @Override
    public RespBean updatePassword(String userTicket, String password,HttpServletRequest request, HttpServletResponse response) {
        User user = getUserByCookie(userTicket, request, response);
        //判断用户是否为空
        if (user==null){
            throw new GlobalException(RespBeanEnum.MOBILE_NOT_EXIST);
        }
        //设置密码
        user.setPassword(MD5Utils.formPassToDBPass(password,user.getSalt()));
        //在数据库中进行更新
        int result = userMapper.updateById(user);
        //判断是否操作成功
        if (1==result){
            //如果操作成功，删除redis中对应的用户
            redisTemplate.delete("user:"+userTicket);
            return RespBean.success();
        }else{
            return RespBean.error(RespBeanEnum.PASSWORD_UPDATE_FAIL);
        }

    }


}
