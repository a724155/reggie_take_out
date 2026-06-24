package com.itheima.reggie.service;

import com.itheima.reggie.api.request.DriverPayOrderPageQueryReq;
import com.itheima.reggie.api.response.DriverPayOrderPageVO;
import com.itheima.reggie.api.response.PageResult;
import com.itheima.reggie.entity.ColdChainPayOrderDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 冷运定金支付单服务接口
 */
public interface IColdChainPayOrderService {

    /**
     * 查询司机自己的支付单分页列表
     *
     * @param driverId 当前登录司机ID
     * @param request  分页查询条件
     * @return 支付单分页数据
     */
    PageResult<DriverPayOrderPageVO> queryDriverPayOrderPage(Long driverId, DriverPayOrderPageQueryReq request);

    /**
     * 分批查询超时但仍处于待支付状态的支付单
     * <p>
     * 该方法供 XXL-JOB 调用。
     *
     * @param lastPayOrderId 上一批处理到的支付单ID
     * @param limit          单次最大查询数量
     * @param currentTime    当前时间
     * @return 超时支付单列表
     */
    List<ColdChainPayOrderDO> queryExpiredWaitPayOrderList(Long lastPayOrderId, Integer limit, LocalDateTime currentTime);

    /**
     * 超时关闭支付单
     * <p>
     * 只有待支付且已经超时的支付单才允许关闭。
     *
     * @param payOrderId  支付单ID
     * @param currentTime 当前时间
     * @return true：本次成功关闭；false：订单已被其他流程处理或不满足关闭条件
     */
    boolean closePayOrderIfWaitingAndExpired(Long payOrderId, LocalDateTime currentTime);

    /**
     * 支付渠道确认成功后，将支付单推进为已支付
     *
     * @param payOrderId 支付单ID
     * @param paidTime   支付成功时间
     * @return true：状态推进成功；false：支付单已不是待支付状态
     */
    boolean markPayOrderPaidIfWaiting(Long payOrderId, LocalDateTime paidTime);
}
