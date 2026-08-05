package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Transactional
    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO orderSubmitDTO) {

        Long userId = BaseContext.getCurrentId();

        //异常排除：校验地址簿存在且属于当前用户
        AddressBook addressBook = addressBookMapper.getById(orderSubmitDTO.getAddressBookId());
        if(addressBook == null || !addressBook.getUserId().equals(userId)){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(userId);
        if(shoppingCartList == null || shoppingCartList.isEmpty()){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(orderSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName()
                + addressBook.getDistrictName() + addressBook.getDetail());
        orders.setUserId(userId);
        orderMapper.insert(orders);
        //向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        //清空当前用户的购物车数据
        shoppingCartMapper.deleteById(userId);
        //封装VO返回结果
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        // 入参校验
        if (ordersPaymentDTO == null || ordersPaymentDTO.getOrderNumber() == null || ordersPaymentDTO.getOrderNumber().isEmpty()) {
            throw new OrderBusinessException(MessageConstant.PARAM_ERROR);
        }

        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询订单，校验订单存在且属于当前用户
        Orders ordersDB = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!ordersDB.getUserId().equals(userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 已支付的订单直接返回，避免重复支付
        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            log.info("订单已支付，无需重复支付，订单号={}", ordersPaymentDTO.getOrderNumber());
            return buildMockPayVO();
        }

        // 模拟支付成功（教学演示模式，未接入真实微信支付）：
        // 直接更新订单为已支付、待接单状态
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .payMethod(ordersPaymentDTO.getPayMethod())
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
        log.warn("模拟支付成功：订单号={}，请尽快接入真实微信支付", ordersPaymentDTO.getOrderNumber());

        // 返回模拟的预支付参数（前端跳过微信调起，直接跳转支付成功页）
        return buildMockPayVO();
    }

    /**
     * 构建模拟支付的预支付参数
     *
     * @return
     */
    private OrderPaymentVO buildMockPayVO() {
        return OrderPaymentVO.builder()
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .nonceStr(RandomStringUtils.randomNumeric(32))
                .packageStr("prepay_id=mock")
                .signType("RSA")
                .paySign("mock")
                .build();
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        if (ordersDB == null) {
            log.error("支付回调失败：订单不存在，订单号={}", outTradeNo);
            return;
        }

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    @Override
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 前端历史订单列表需要展示每个订单的菜品明细，这里逐单组装 orderDetailList
        List<OrderVO> orderVOList = new ArrayList<>();
        if (page != null && page.getResult() != null) {
            for (Orders orders : page.getResult()) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailMapper.getByOrderId(orders.getId()));
                orderVOList.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), orderVOList);
    }

    @Override
    public OrderVO getOrderDetailById(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 校验订单是否属于当前登录用户
        if (!orders.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 根据订单id查询订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 组装VO返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        // 入参校验
        if (ordersCancelDTO == null || ordersCancelDTO.getId() == null) {
            throw new OrderBusinessException(MessageConstant.PARAM_ERROR);
        }

        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 校验订单是否属于当前登录用户
        if (!ordersDB.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        Integer status = ordersDB.getStatus();
        if (status == null || Orders.COMPLETED.equals(status) || Orders.CANCELLED.equals(status)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 如果已支付，需要先退款
        refundIfPaid(ordersDB);

        // 更新订单状态、取消原因、取消时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 再来一单：将原订单中的商品重新加入购物车
     *
     * @param id 订单id
     */
    @Override
    public void repetition(Long id) {
        // 查询当前用户
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        if (orderDetailList == null || orderDetailList.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 将订单明细转换为购物车数据
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartList.add(shoppingCart);
        }

        // 批量插入购物车
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    @Override
    public PageResult page(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.conditionSearch(ordersPageQueryDTO);

        // 组装VO：填充订单明细和菜品摘要
        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> records = page.getResult();
        if (records != null) {
            for (Orders orders : records) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);

                // 查询订单明细
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
                orderVO.setOrderDetailList(orderDetailList);

                // 拼接菜品摘要字符串，如：鱼香肉丝x2,米饭x1
                orderVO.setOrderDishes(buildOrderDishes(orderDetailList));

                orderVOList.add(orderVO);
            }
        }

        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 统计各状态订单数量
     *
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 查询各状态订单数量
        List<Map<String, Object>> list = orderMapper.countByStatus();

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(0);
        orderStatisticsVO.setConfirmed(0);
        orderStatisticsVO.setDeliveryInProgress(0);

        if (list != null && !list.isEmpty()) {
            for (Map<String, Object> map : list) {
                Integer status = (Integer) map.get("status");
                Integer count = ((Number) map.get("count")).intValue();
                if (status == null || count == null) {
                    continue;
                }
                // 2待接单 3待派送 4派送中
                if (Orders.TO_BE_CONFIRMED.equals(status)) {
                    orderStatisticsVO.setToBeConfirmed(count);
                } else if (Orders.CONFIRMED.equals(status)) {
                    orderStatisticsVO.setConfirmed(count);
                } else if (Orders.DELIVERY_IN_PROGRESS.equals(status)) {
                    orderStatisticsVO.setDeliveryInProgress(count);
                }
            }
        }

        return orderStatisticsVO;
    }

    /**
     * 管理端根据id查询订单详情（不做归属校验）
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO getById(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 根据订单id查询订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 组装VO返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        // 拼接菜品摘要字符串（与列表页一致）
        orderVO.setOrderDishes(buildOrderDishes(orderDetailList));

        return orderVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        // 入参校验
        if (ordersConfirmDTO == null || ordersConfirmDTO.getId() == null) {
            throw new OrderBusinessException(MessageConstant.PARAM_ERROR);
        }

        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersConfirmDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 只有待接单(2)状态的订单才能接单
        if (!Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 更新订单状态为已接单(3)，并设置预计送达时间（当前时间 + 60分钟）
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CONFIRMED)
                .estimatedDeliveryTime(LocalDateTime.now().plusMinutes(60))
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        // 入参校验
        if (ordersRejectionDTO == null || ordersRejectionDTO.getId() == null) {
            throw new OrderBusinessException(MessageConstant.PARAM_ERROR);
        }

        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        Integer status = ordersDB.getStatus();
        if (status == null || Orders.CONFIRMED.equals(status) || Orders.DELIVERY_IN_PROGRESS.equals(status)
                || Orders.COMPLETED.equals(status) || Orders.CANCELLED.equals(status)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 如果已支付，需要先退款
        refundIfPaid(ordersDB);

        // 更新订单状态为已取消(6)，记录拒单原因和拒单时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 管理端取消订单（不做归属校验）
     *
     * @param ordersCancelDTO
     */
    @Override
    public void cancelByAdmin(OrdersCancelDTO ordersCancelDTO) {
        // 入参校验
        if (ordersCancelDTO == null || ordersCancelDTO.getId() == null) {
            throw new OrderBusinessException(MessageConstant.PARAM_ERROR);
        }

        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        Integer status = ordersDB.getStatus();
        if (status == null || Orders.COMPLETED.equals(status) || Orders.CANCELLED.equals(status)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 如果已支付，需要先退款
        refundIfPaid(ordersDB);

        // 更新订单状态为已取消(6)，记录取消原因和取消时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     *
     * @param id 订单id
     */
    @Override
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 只有已接单(3)状态的订单才能派送
        if (!Orders.CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 更新订单状态为派送中(4)，并记录派送时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.DELIVERY_IN_PROGRESS)
                .deliveryTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id 订单id
     */
    @Override
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 只有派送中(4)状态的订单才能完成
        if (!Orders.DELIVERY_IN_PROGRESS.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 更新订单状态为已完成(5)
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.COMPLETED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 订单已支付则发起微信退款
     *
     * @param orders 订单
     */
    private void refundIfPaid(Orders orders) {
        if (!Orders.PAID.equals(orders.getPayStatus())) {
            return;
        }
        // 教学演示模式：支付为模拟支付，退款同样模拟，不调用真实微信退款接口
        log.warn("模拟退款成功：orderNumber={}，请尽快接入真实微信支付", orders.getNumber());
    }

    /**
     * 拼接订单菜品摘要，如：鱼香肉丝x2,米饭x1
     *
     * @param orderDetailList 订单明细
     * @return 菜品摘要字符串
     */
    private String buildOrderDishes(List<OrderDetail> orderDetailList) {
        StringBuilder sb = new StringBuilder();
        if (orderDetailList != null) {
            for (OrderDetail detail : orderDetailList) {
                sb.append(detail.getName())
                  .append("x")
                  .append(detail.getNumber())
                  .append(",");
            }
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

}







