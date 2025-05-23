package com.xxxx.seckill.utils;

import org.springframework.stereotype.Component;
import org.apache.commons.codec.digest.DigestUtils;


/**
 * MD5工具类
 */
@Component
public class MD5Utils {

    public static String md5(String src){
         return DigestUtils.md5Hex(src);
    }

    private static final String salt="1a2b3c4d";

    //用户端密码加密
    public static String inputPassToFromPass(String inputPass){
        //先进行盐加密
        String str =""+ salt.charAt(0)+salt.charAt(2)+inputPass+salt.charAt(5)+salt.charAt(4);
       //第一次md加密
        return md5(str);
    }

    //服务端密码加密
    public static String formPassToDBPass(String formPass,String salt){
        //先进行盐加密
        String str =""+ salt.charAt(0)+salt.charAt(2)+formPass+salt.charAt(5)+salt.charAt(4);
        //第一次md加密
        return md5(str);
    }

    //最终要调用的方法
    public static String inputPassToDBPass(String inputPass,String salt){
        String fromPass = inputPassToFromPass(inputPass);
        String dbPass = formPassToDBPass(fromPass,salt);
        return dbPass;
    }

    public static void main(String[] args) {
        //测试第一次加密的密码：d3b1294a61a07da9b49b6e22b2cbd7f9
        System.out.println(inputPassToFromPass("123456789"));
        //第二次加密后的密码
        System.out.println(formPassToDBPass("52f898c27511518b951a737149783901","1a2b3c4d"));
        System.out.println(inputPassToDBPass("123456789","1a2b3c4d"));
    }



}
