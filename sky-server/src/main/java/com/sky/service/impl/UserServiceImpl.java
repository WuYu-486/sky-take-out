package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.exception.BaseException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户业务层
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    //微信接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;


    @Override
    public User login(UserLoginDTO userLoginDTO) {
        // 入参校验：微信登录code不能为空
        if (userLoginDTO.getCode() == null || userLoginDTO.getCode().trim().isEmpty()) {
            throw new BaseException("微信登录code不能为空");
        }

        String openid;
        try {
            openid = getOpenid(userLoginDTO.getCode());
        } catch (Exception e) {
            // 微信接口调用失败（网络异常、参数错误等）
            log.error("调用微信接口获取openid失败：{}", e.getMessage());
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //判断用户openid是否为空,如果为空则抛出异常
        if (openid == null || openid.isEmpty()) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        //判断用户是否为新用户
        User user = userMapper.getByOpenid(openid);

        //新用户则自动完成注册
        if(user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
            log.info("新用户注册成功：userId={}", user.getId());
        }

        //返回用户信息
        return user;
    }

    private String getOpenid(String code){
        //调用微信接口服务,获取用户openid
        Map<String,String> map = new HashMap<>();
        map.put("appid",weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        JSONObject jsonObject = JSON.parseObject(json);
        // 微信返回错误时（如code无效），没有openid字段
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
