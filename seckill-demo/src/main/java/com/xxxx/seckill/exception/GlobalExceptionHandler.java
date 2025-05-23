package com.xxxx.seckill.exception;

import com.xxxx.seckill.vo.RespBean;
import com.xxxx.seckill.vo.RespBeanEnum;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;



/**
 * 全局异常处理
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public RespBean ExceptionHandler(Exception e){
        if (e instanceof GlobalException){
            GlobalException e1 = (GlobalException) e;
            return RespBean.error(e1.getRespBeanEnum());
        }else if(e instanceof BindException){
            BindException e1 = (BindException) e;
            RespBean respBean = RespBean.error(RespBeanEnum.BIND_ERROR);
            respBean.setMessage("参数校验异常："+e1.getBindingResult().getAllErrors().get(0));
            return respBean;
        }
        return RespBean.error(RespBeanEnum.ERROR);
    }
}
