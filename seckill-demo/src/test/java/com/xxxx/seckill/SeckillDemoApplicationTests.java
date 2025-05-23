package com.xxxx.seckill;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class SeckillDemoApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedisScript<Boolean> script;


    @Test
    public void contextLoads() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //占位，如果key存在，那么无法设置成功，只有当key不存在的时候，才能设置成功
        Boolean isLock = valueOperations.setIfAbsent("k1", "v1");
        if (isLock) {
            //如果成功锁住，就执行一下代码
            valueOperations.set("name", "xxxx");
            String name = (String) valueOperations.get("name");
            System.out.println("name = " + name);
            //执行完了就删除key
            redisTemplate.delete("k1");
        } else {
            System.out.println("有线程在使用，请稍后再试");
        }
    }

    /**
     * 如果在执行过程中抛异常了，那就不会去执行”redisTemplate.delete("k1");",key就不会被删除，会发生死锁问题，这时候在
     * setIfAbsent方法中加5s的过期时间。5s后k自动是失效，虽然还会抛异常，不会执行”redisTemplate.delete("k1");"但是k
     * 会自动失效，下一次仍然能成功被别的线程使用
     */

    @Test
    public void contextLoads02() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //占位，如果key存在，那么无法设置成功，只有当key不存在的时候，才能设置成功
        //给锁添加一个过期时间，防止应用在运行过程中抛出异常导致锁无法释放
        Boolean isLock = valueOperations.setIfAbsent("k1", "v1", 5, TimeUnit.SECONDS);
        if (isLock) {
            //如果成功锁住，就执行一下代码
            valueOperations.set("name", "xxxx");
            String name = (String) valueOperations.get("name");
            System.out.println("name = " + name);
            //添加一个异常
            Integer.parseInt("xxxx");
            //执行完了就删除key
            redisTemplate.delete("k1");
        } else {
            System.out.println("有线程在使用，请稍后再试");
        }
    }

    /**
     * 上面例子，如果业务非常耗时会紊乱。举例：第一个线程首先获得锁，然后执行业务代码，但是业务代
     * 码耗时8秒，这样会在第一个线程的任务还未执行成功锁就会被释放，这时第二个线程会获取到锁开始
     * 执行，在第二个线程开执行了3秒，第一个线程也执行完了，此时第一个线程会释放锁，但是注意，他
     * 释放的第二个现成的锁，释放之后，第三个线程进来。
     *
     * 解决方案：
     * 尽量避免在获取锁之后，执行耗时操作
     * 将锁的value设置为一个随机字符串，每次释放锁的时候，都去比较随机字符串是否一致，如果一
     * 致，再去释放，否则不释放。
     * 释放锁时要去查看所得value，比较value是否正确，释放锁总共三个步骤，这三个步骤不具
     * 备原子性。
     *
     * Lua脚本(原子性的)
     * Lua脚本优势：
     * 使用方便，Redis内置了对Lua脚本的支持
     * Lua脚本可以在Rdis服务端原子的执行多个Redis命令
     * 由于网络在很大程度上会影响到Redis性能，使用Lua脚本可以让多个命令一次执行，可以有
     * 效解决网络给Redis带来的性能问题
     * 使用Lua脚本思路：
     * 提前在Redis服务端写好Lua脚本，然后在java客户端去调用脚本
     * 可以在java客户端写Lua脚本，写好之后，去执行。需要执行时，每次将脚本发送到Redis上
     * 去执行
     */
    @Test
    public void contextLoads03() {

        ValueOperations valueOperations = redisTemplate.opsForValue();
        //设置一个随机值
        String value = UUID.randomUUID().toString();
        Boolean isLock = valueOperations.setIfAbsent("k1", value, 120, TimeUnit.SECONDS);
        if (isLock){
            valueOperations.set("name","xxxx");
            String name = (String) valueOperations.get("name");
            System.out.println("name = "+name);
            System.out.println(valueOperations.get("k1"));
            Boolean result = (Boolean) redisTemplate.execute(script, Collections.singletonList("k1"), value);
            System.out.println(result);
        }else {
            System.out.println("有线程在使用，请稍后再试");
        }

    }














}