package com.xxxx.seckill.utils;

import java.util.UUID;

/**
 * UUID工具类
 * UUID:全名叫做 Universally Unique Identifier，也就是通用唯一标识符的意思。
 * 有时候，也叫做全局唯一标识符, UUID 是全局唯一的，重复 UUID 的概率接近零，可以忽略不计
 * 用于以下地方：
 * 1.随机生成的文件名；
 * 2.Java Web 应用程序的 sessionID；
 * 3.数据库表的主键；
 * 4.事务 ID（UUID 生成算法非常高效，每台计算机每秒高达 1000 万次）。
 */
public class UUIDUtil {

    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}