package com.xxxx.seckill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zzr
 * @since 2021-12-30
 */
public interface ISeckillOrderService extends IService<SeckillOrder> {

    /**
     * 获取秒杀结果；goodsId:成功，-1 秒杀失败，0：排队中
     * @param user
     * @param goodsId
     * @return
     */
    Long getResult(User user, Long goodsId);
}
