package com.xxxx.seckill.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 测试
 */
@Controller
@RequestMapping("/demo")
public class DemoController {

    @RequestMapping("hello")
    public String hello(Model model){
        model.addAttribute("name","张子然");
        return "hello";
    }

}
