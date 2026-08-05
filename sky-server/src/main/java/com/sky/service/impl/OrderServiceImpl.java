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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
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
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

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
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
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
        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            try {
                weChatPayUtil.refund(
                        ordersDB.getNumber(),
                        ordersDB.getNumber() + System.currentTimeMillis(),
                        ordersDB.getAmount(),
                        ordersDB.getAmount());
            } catch (Exception e) {
                log.error("订单退款失败：{}", e.getMessage());
                throw new OrderBusinessException("订单退款失败");
            }
        }

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
                orderVO.setOrderDishes(sb.toString());

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
        List<Map<Integer, Integer>> list = orderMapper.countByStatus();

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(0);
        orderStatisticsVO.setConfirmed(0);
        orderStatisticsVO.setDeliveryInProgress(0);

        if (list != null && !list.isEmpty()) {
            for (Map<Integer, Integer> map : list) {
                Integer status = map.get("status");
                Integer count = map.get("count");
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
        orderVO.setOrderDishes(sb.toString());

        return orderVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
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
        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            try {
                weChatPayUtil.refund(
                        ordersDB.getNumber(),
                        ordersDB.getNumber() + System.currentTimeMillis(),
                        ordersDB.getAmount(),
                        ordersDB.getAmount());
            } catch (Exception e) {
                log.error("订单退款失败：{}", e.getMessage());
                throw new OrderBusinessException("订单退款失败");
            }
        }

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
        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            try {
                weChatPayUtil.refund(
                        ordersDB.getNumber(),
                        ordersDB.getNumber() + System.currentTimeMillis(),
                        ordersDB.getAmount(),
                        ordersDB.getAmount());
            } catch (Exception e) {
                log.error("订单退款失败：{}", e.getMessage());
                throw new OrderBusinessException("订单退款失败");
            }
        }

        // 更新订单状态为已取消(6)，记录取消原因和取消时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

}







