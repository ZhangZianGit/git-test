package com.xxxx.seckill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.vo.LoginVo;
import com.xxxx.seckill.vo.RespBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zzr
 * @since 2021-12-26
 */
public interface IUserService extends IService<User> {
    /**
     * 登录功能
     * @param loginVo
     * @param request
     * @param response
     * @return
     */
    RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response);

    /**
     * 根据cookie获取用户
     */
    User getUserByCookie(String userTicket,HttpServletRequest request, HttpServletResponse response);

    /**
     * 用户更新密码
     */
    RespBean updatePassword(String userTicket,String password,HttpServletRequest request, HttpServletResponse response);
}
