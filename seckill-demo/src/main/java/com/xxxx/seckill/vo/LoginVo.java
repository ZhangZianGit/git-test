package com.xxxx.seckill.vo;

import com.xxxx.seckill.validator.IsMobile;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

/**
 * 登录参数
 */

@Data
public class LoginVo {

    @NotNull //不为空
    @IsMobile //自定义注解,默认required为true
    private String mobile;

    @NotNull
    @Length(min=32) //长度最小为32位
    private String password;



}
