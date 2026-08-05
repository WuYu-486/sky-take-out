package com.sky.controller.user;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.dto.OrdersCancelDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/order")
@Slf4j
@Api(tags = "C端用户订单相关接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     *
     * @param orderSubmitDTO 下单参数
     * @return 订单信息
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO orderSubmitDTO){
        if (orderSubmitDTO == null) {
            return Result.error("下单参数不能为空");
        }
        log.info("用户下单{}",orderSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submit(orderSubmitDTO);
        return Result.success(orderSubmitVO);
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        if (ordersPaymentDTO == null) {
            return Result.error("支付参数不能为空");
        }
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 历史订单查询
     *
     * @param ordersPageQueryDTO 分页查询参数
     * @return 分页结果
     */
    @GetMapping("/historyOrders")
    @ApiOperation("C端用户历史订单查询")
    public Result<PageResult> historyOrders(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("历史订单查询：{}", ordersPageQueryDTO);
        PageResult pageResult = orderService.historyOrders(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 查询订单详情
     *
     * @param id 订单id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("C端用户查询订单详情")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {
        log.info("查询订单详情:{}", id);
        OrderVO orderVO = orderService.getOrderDetailById(id);
        return Result.success(orderVO);
    }

    /**
     * 取消订单
     *
     * @param id 订单id
     * @param ordersCancelDTO 取消原因
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("C端用户取消订单")
    public Result<String> cancel(@PathVariable Long id, @RequestBody(required = false) OrdersCancelDTO ordersCancelDTO){
        if (ordersCancelDTO == null) {
            ordersCancelDTO = new OrdersCancelDTO();
        }
        ordersCancelDTO.setId(id);
        log.info("取消订单:{}", ordersCancelDTO);
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }
    /**
     * 再来一单（将原订单商品重新加入购物车）
     *
     * @param id 订单id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("C端用户再来一单")
    public Result<String> repetition(@PathVariable Long id){
        log.info("再来一单:{}", id);
        orderService.repetition(id);
        return Result.success();
    }

}
