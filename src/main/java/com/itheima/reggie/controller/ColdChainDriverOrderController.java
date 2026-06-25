package com.itheima.reggie.controller;

import com.itheima.reggie.api.request.DriverCancelOrderReq;
import com.itheima.reggie.api.request.DriverOrderPageQueryReq;
import com.itheima.reggie.api.request.DriverPaySubmitReq;
import com.itheima.reggie.api.response.ColdChainOrderDetailVO;
import com.itheima.reggie.api.response.ColdChainOrderPageVO;
import com.itheima.reggie.api.response.DriverPayOrderVO;
import com.itheima.reggie.api.response.PaySubmitVO;
import com.itheima.reggie.common.PageResult;
import com.itheima.reggie.common.YmmResult;
import com.itheima.reggie.service.IColdChainDriverOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;

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
    public YmmResult<PageResult<ColdChainOrderPageVO>> queryDriverOrderPage(@RequestAttribute("currentDriverId") @Positive(message = "当前司机ID必须大于0") Long driverId, @Valid @ModelAttribute DriverOrderPageQueryReq request) {

        // driverId 来自认证层，不来自前端，避免发生水平越权。
        // request 中的 pageNo、pageSize、orderStatus 等来自 URL 查询参数。
        PageResult<ColdChainOrderPageVO> pageResult = coldChainDriverOrderService.queryDriverOrderPage(driverId, request);

        // Spring 会自动将 YmmResult<PageResult<ColdChainOrderPageVO>> 转成 JSON 返回给前端。
        return YmmResult.success(pageResult);
    }

    /**
     * 查询当前登录司机可见的冷运订单详情。
     *
     * <p>前端请求约定：</p>
     *
     * <pre>
     * GET /cold-chain/driver/orders/888
     *
     * 请求头：
     * Authorization: Bearer xxxxx
     * </pre>
     *
     * <p>为什么使用 GET：</p>
     *
     * <pre>
     * 当前接口只查询订单详情，不会修改订单状态；
     * 因此使用 GET，而不是 POST。
     * </pre>
     *
     * <p>为什么使用 @PathVariable：</p>
     *
     * <pre>
     * orderId 是当前要查询的订单资源唯一标识；
     * URL /orders/888 中的 888 就是 orderId；
     * 这种“资源 ID”适合使用路径参数表达。
     * </pre>
     *
     * @param driverId 当前登录司机 ID，由认证层注入，用于数据权限校验
     * @param orderId 冷运订单 ID，从 URL 路径中获取
     * @return 冷运订单详情
     */
    @GetMapping("/{orderId}")
    public YmmResult<ColdChainOrderDetailVO> queryDriverOrderDetail(@RequestAttribute("currentDriverId") @Positive(message = "当前司机ID必须大于0") Long driverId,
                                                                    @PathVariable @Positive(message = "订单ID必须大于0") Long orderId) {

        // Service 内部应校验：当前订单是否属于当前司机，或者当前司机是否有查看权限。
        ColdChainOrderDetailVO orderDetailVO = coldChainDriverOrderService.queryDriverOrderDetail(driverId, orderId);

        return YmmResult.success(orderDetailVO);
    }

    /**
     * 查询当前司机指定订单下的支付单列表。
     *
     * <p>前端请求约定：</p>
     *
     * <pre>
     * GET /cold-chain/driver/orders/888/pay-orders?includeClosed=false
     *
     * 请求头：
     * Authorization: Bearer xxxxx
     * </pre>
     *
     * <p>参数来源：</p>
     *
     * <pre>
     * orderId：
     * 来自 /orders/888 中的路径参数；
     *
     * includeClosed：
     * 来自 ?includeClosed=false 的简单 URL 查询参数。
     * </pre>
     *
     * <p>为什么使用 @RequestParam：</p>
     *
     * <pre>
     * includeClosed 只有一个简单的布尔筛选条件；
     * 不需要额外定义一个查询对象；
     * 因此直接使用 @RequestParam 更清晰。
     * </pre>
     *
     * @param driverId 当前登录司机 ID，由认证层注入
     * @param orderId 冷运订单 ID
     * @param includeClosed 是否包含已关闭支付单，默认不包含
     * @return 支付单列表
     */
    @GetMapping("/{orderId}/pay-orders")
    public YmmResult<List<DriverPayOrderVO>> queryDriverPayOrderList(@RequestAttribute("currentDriverId") @Positive(message = "当前司机ID必须大于0") Long driverId,

            @PathVariable @Positive(message = "订单ID必须大于0") Long orderId,

            @RequestParam(value = "includeClosed", required = false, defaultValue = "false") Boolean includeClosed) {

        List<DriverPayOrderVO> payOrderList = coldChainDriverOrderService.queryDriverPayOrderList(driverId, orderId, includeClosed);

        return YmmResult.success(payOrderList);
    }

    /**
     * 当前司机为指定订单提交支付，并获取收银台拉起信息。
     *
     * <p>前端请求约定：</p>
     *
     * <pre>
     * POST /cold-chain/driver/orders/888/pay
     *
     * 请求头：
     * Authorization: Bearer xxxxx
     * Idempotency-Key: 0cdb7a70-8f6a-4ccf-9cc6-2c785b13d999
     * Content-Type: application/json
     *
     * 请求体：
     * {
     *   "payChannel": "ALIPAY",
     *   "couponId": 10001
     * }
     * </pre>
     *
     * <p>为什么使用 POST，而不是 GET：</p>
     *
     * <pre>
     * 提交支付会产生业务副作用：
     * 1. 可能创建支付单；
     * 2. 可能锁货；
     * 3. 可能生成收银台订单；
     * 4. 可能写数据库、Redis。
     *
     * GET 应当只用于读取数据；
     * 任何会修改业务状态、创建资源、触发支付链路的操作，都不应该使用 GET。
     * </pre>
     *
     * <p>为什么使用 @RequestBody：</p>
     *
     * <pre>
     * payChannel、couponId 属于一次支付提交的业务参数；
     * 前端通过 JSON 请求体传递；
     * Spring 使用 @RequestBody 将 JSON 自动转换为 DriverPaySubmitReq。
     * </pre>
     *
     * <p>为什么使用 @RequestHeader("Idempotency-Key")：</p>
     *
     * <pre>
     * 司机可能重复点击“立即支付”，也可能因为网络重试重复发请求；
     * Idempotency-Key 用于让后端识别“这是同一次支付提交”；
     * 具体幂等处理逻辑必须在 Service 或更下层完成。
     * </pre>
     *
     * @param driverId 当前登录司机 ID，由认证层注入
     * @param orderId 冷运订单 ID，从 URL 路径中获取
     * @param idempotencyKey 幂等键，从 HTTP 请求头中获取
     * @param request 支付提交参数，从 JSON 请求体中获取
     * @return 支付单号及收银台拉起信息
     */
    @PostMapping("/{orderId}/pay")
    public YmmResult<PaySubmitVO> submitDriverPay(@RequestAttribute("currentDriverId") @Positive(message = "当前司机ID必须大于0") Long driverId,

            @PathVariable @Positive(message = "订单ID必须大于0") Long orderId,

            @RequestHeader("Idempotency-Key") String idempotencyKey,

            @Valid @RequestBody DriverPaySubmitReq request) {

        // Controller 只负责收参、身份传递和返回；
        // 创建支付单、锁货、金额校验、幂等控制等逻辑由 Service 负责。
        PaySubmitVO paySubmitVO = coldChainDriverOrderService.submitDriverPay(driverId, orderId, request, idempotencyKey);

        return YmmResult.success(paySubmitVO);
    }

    /**
     * 当前司机取消自己的冷运订单。
     *
     * <p>前端请求约定：</p>
     *
     * <pre>
     * POST /cold-chain/driver/orders/888/cancel
     *
     * 请求头：
     * Authorization: Bearer xxxxx
     * Content-Type: application/json
     *
     * 请求体：
     * {
     *   "cancelReason": "暂时无法按时到达装货地"
     * }
     * </pre>
     *
     * <p>为什么使用 POST，而不是 DELETE：</p>
     *
     * <pre>
     * “取消订单”不是物理删除订单；
     * 它本质是一次状态流转，例如：
     * WAIT_PAY -> CANCELED。
     *
     * 订单记录、支付记录、取消原因、审计日志通常都要保留；
     * 所以这里更适合设计成一个业务动作接口：
     * POST /{orderId}/cancel。
     * </pre>
     *
     * <p>为什么取消原因放在 @RequestBody 中：</p>
     *
     * <pre>
     * cancelReason 是本次取消动作的业务数据；
     * 使用 JSON 请求体比拼接在 URL 上更适合，也避免 URL 过长或特殊字符编码问题。
     * </pre>
     *
     * @param driverId 当前登录司机 ID，由认证层注入
     * @param orderId 冷运订单 ID
     * @param request 取消订单请求
     * @return 空成功结果
     */
    @PostMapping("/{orderId}/cancel")
    public YmmResult<Void> cancelDriverOrder(@RequestAttribute("currentDriverId") @Positive(message = "当前司机ID必须大于0") Long driverId,

            @PathVariable @Positive(message = "订单ID必须大于0") Long orderId,

            @Valid @RequestBody DriverCancelOrderReq request) {

        // 取消权限、订单状态校验、支付单关闭、锁货释放等业务动作必须由 Service 处理。
        coldChainDriverOrderService.cancelDriverOrder(driverId, orderId, request);

        // 取消成功后不需要额外业务数据，因此返回 YmmResult<Void>。
        return YmmResult.success();
    }
}
