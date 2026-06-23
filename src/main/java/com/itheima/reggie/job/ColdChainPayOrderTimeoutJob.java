package com.itheima.reggie.job;

import com.itheima.reggie.entity.ColdChainPayOrderDO;
import com.itheima.reggie.service.IColdChainPayOrderService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ColdChainPayOrderTimeoutJob {

    /**
     * 单批扫描数量
     */
    private static final Integer BATCH_SIZE = 100;

    /**
     * 冷运支付单服务
     */
    private final IColdChainPayOrderService coldChainPayOrderService;

    /**
     * 扫描超时未支付的冷运支付单
     *
     * XXL-JOB Handler 名称：
     * coldChainPayOrderTimeoutHandler
     */
    @XxlJob("coldChainPayOrderTimeoutHandler")
    public void execute() {
        LocalDateTime currentTime = LocalDateTime.now();

        /**
         * 游标式扫描起始 ID。
         */
        Long lastPayOrderId = 0L;

        while (true) {
            List<ColdChainPayOrderDO> payOrderList = coldChainPayOrderService.queryExpiredWaitPayOrderList(
                            lastPayOrderId,
                            BATCH_SIZE,
                            currentTime);

            if (CollectionUtils.isEmpty(payOrderList)) {
                break;
            }

            for (ColdChainPayOrderDO payOrderDO : payOrderList) {
                if (Objects.isNull(payOrderDO) || Objects.isNull(payOrderDO.getId())) {
                    continue;
                }

                /**
                 * 无论本次处理是否成功，
                 * 都推进扫描游标，防止死循环。
                 */
                lastPayOrderId = payOrderDO.getId();

                /**
                 * 真实支付系统中，这一步不能省：
                 *
                 * 先查询支付渠道最终状态。
                 *
                 * 如果支付渠道已成功，但回调延迟或丢失，
                 * 不能直接把本地支付单关闭。
                 *
                 * 此处为了突出 MyBatis-Plus 状态更新逻辑，
                 * 假设渠道查询结果确认仍为未支付。
                 */
                boolean closeSuccess = coldChainPayOrderService.closePayOrderIfWaitingAndExpired(
                                payOrderDO.getId(),
                                currentTime);

                if (!closeSuccess) {
                    /**
                     * 影响行数为 0 的常见原因：
                     *
                     * 1. 支付回调已经先更新成已支付；
                     * 2. 另一台机器的 JOB 已经关闭；
                     * 3. 人工客服已经处理；
                     * 4. 数据状态不符合预期。
                     */
                    continue;
                }

                /**
                 * 真正生产环境中，关闭成功后应该：
                 *
                 * 1. 在同一个数据库事务中写入 Outbox 事件；
                 * 2. Outbox 发布器发送 RocketMQ；
                 * 3. 消费者释放 cargoId 对应的货源锁；
                 * 4. 消费者必须继续做幂等处理。
                 *
                 * 不建议这里直接同步调用 Redis 解锁或直接发 MQ，
                 * 否则可能出现：
                 *
                 * 数据库已经关闭支付单，
                 * 但 MQ 发送失败，
                 * 导致货源锁未释放。
                 */
                log.info(
                        "冷运支付单超时关闭成功，payOrderId={}, cargoId={}, driverId={}",
                        payOrderDO.getId(),
                        payOrderDO.getCargoId(),
                        payOrderDO.getDriverId()
                );
            }

            /**
             * 本批数量不足 BATCH_SIZE，
             * 说明本轮超时数据已扫描完成。
             */
            if (payOrderList.size() < BATCH_SIZE) {
                break;
            }
        }
    }
}
