package com.xxxx.seckill.controller;


import com.xxxx.seckill.service.IUserService;
import com.xxxx.seckill.vo.LoginVo;
import com.xxxx.seckill.vo.RespBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;


@Controller //页面跳转，RestController 是返回对象
@RequestMapping("/login")
@Slf4j //输出日志
public class LoginController {

    @Autowired
    private IUserService userService;
    /**
     * 跳转登录页面
     * @return
     */
    @RequestMapping("/toLogin")
    public String loLogin(){
        return "login";
    }

    /**
     * 登录功能
     * @param loginVo
     * @return
     */
    @RequestMapping("/doLogin")
    @ResponseBody //返回对象
    public RespBean doLogin(@Valid LoginVo loginVo, HttpServletRequest request, HttpServletResponse response){

        return userService.doLogin(loginVo,request,response);
    }
}
