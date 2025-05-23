package com.xxxx.seckill.config;

import com.xxxx.seckill.pojo.User;

/**
 * ThreadLocal:解决的是每个线程绑定自己的值，可以把它看成一个盒子，盒子存放的是每个线程的私有数据，
 * 如果访问ThreadLocal的变量话，会获取到遍历的本地副本，它有两个方法，一个是set，一个是get，能够
 * 用来获取默认值，或者将值变更为当前副本的值，解决线程安全问题
 *
 * 问题：在高并发，多线程情况下，如果都是在公共线程里存用户信息的话，可能会导致用户信息的紊乱，所以需要
 * 当前用户把登录信息存在自己的线程上，只有当前线程才能看到自己的值，不要进行相互的冲突
 *
 * 存放用户登录信息
 * @author zhoubin
 * @since 1.0.0
 */
public class UserContext {

	private static ThreadLocal<User> userHolder = new ThreadLocal<User>();

	public static void setUser(User user) {
		userHolder.set(user);
	}

	public static User getUser() {
		return userHolder.get();
	}
}
