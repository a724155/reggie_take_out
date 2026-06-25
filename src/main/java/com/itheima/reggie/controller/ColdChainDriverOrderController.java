package com.itheima.reggie.controller;

import com.itheima.reggie.api.request.DriverOrderPageQueryReq;
import com.itheima.reggie.api.response.ColdChainOrderPageVO;
import com.itheima.reggie.common.PageResult;
import com.itheima.reggie.common.YmmResult;
import com.itheima.reggie.service.IColdChainDriverOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Positive;

/**
 * 冷运司机端订单接口。
 *
 * 该 Controller 的职责：
 * 1. 接收 App / 前端 HTTP 请求；
 * 2. 从认证层获取当前司机身份；
 * 3. 将 URL 参数、JSON 参数转换为 Java 请求对象；
 * 4. 调用 Service；
 * 5. 使用 YmmResult 统一返回 JSON 给前端。
 *
 * 不应该在 Controller 中编写：
 * 1. SQL；
 * 2. 事务；
 * 3. 支付金额校验；
 * 4. 状态机推进；
 * 5. 幂等更新逻辑。
 */
@RestController
@RequestMapping("/cold-chain/driver/orders")
@Validated
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ColdChainDriverOrderController {

    /**
     * 冷运司机端订单服务。
     */
    private final IColdChainDriverOrderService coldChainDriverOrderService;

    /**
     * 查询当前登录司机的冷运订单分页列表。
     *
     * <p>前端请求约定：</p>
     *
     * <pre>
     * GET /cold-chain/driver/orders?pageNo=1&pageSize=20&orderStatus=10&orderNo=CC202606240001
     *
     * 请求头：
     * Authorization: Bearer xxxxx
     * </pre>
     *
     * <p>前端传参方式：</p>
     *
     * <pre>
     * pageNo、pageSize、orderStatus、orderNo 都放在 URL 查询参数中。
     * 例如：?pageNo=1&pageSize=20
     * </pre>
     *
     * <p>为什么使用 GET：</p>
     *
     * <pre>
     * 当前接口只读取订单列表，不创建、不修改、不删除数据；
     * GET 语义就是查询资源，适合分页、筛选、排序等场景。
     * </pre>
     *
     * <p>为什么使用 @ModelAttribute：</p>
     *
     * <pre>
     * 分页查询参数通常较多；
     * @ModelAttribute 会将 URL 中的 pageNo、pageSize、orderStatus 等参数，
     * 自动组装为 DriverOrderPageQueryReq 对象。
     * </pre>
     *
     * <p>为什么使用 @RequestAttribute：</p>
     *
     * <pre>
     * driverId 不允许前端直接传入；
     * 认证拦截器会先解析 token，再执行：
     * request.setAttribute("currentDriverId", driverId);
     *
     * Controller 从请求属性中取得当前司机 ID，
     * 可以避免司机通过篡改 driverId 查询其他司机订单。
     * </pre>
     *
     * @param driverId 当前登录司机 ID，由认证层注入
     * @param request 分页查询条件，由 URL 查询参数自动绑定
     * @return 当前司机的冷运订单分页结果
     */
    @GetMapping
    public YmmResult<PageResult<ColdChainOrderPageVO>> queryDriverOrderPage(
            @RequestAttribute("currentDriverId")
            @Positive(message = "当前司机ID必须大于0")
            Long driverId,

            @Valid
            @ModelAttribute
            DriverOrderPageQueryReq request) {

        // driverId 来自认证层，不来自前端，避免发生水平越权。
        // request 中的 pageNo、pageSize、orderStatus 等来自 URL 查询参数。
        PageResult<ColdChainOrderPageVO> pageResult =
                coldChainDriverOrderService.queryDriverOrderPage(driverId, request);

        // Spring 会自动将 YmmResult<PageResult<ColdChainOrderPageVO>> 转成 JSON 返回给前端。
        return YmmResult.success(pageResult);
    }


}
