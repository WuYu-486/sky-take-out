package com.sky.controller.user;

import com.sky.constant.RedisKeyConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/user/shop")
@RestController("userShopController")
@Api(tags = "C端店铺管理")
@Slf4j
public class ShopController {

    @Autowired
    RedisTemplate redisTemplate;

    /**
     * 查询店铺营业状态
     *
     * @return 0打烊 1营业
     */
    @GetMapping("/status")
    @ApiOperation("查询店铺营业状态")
    public Result<Integer> getStatus(){
        log.info("查询店铺营业状态");
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(RedisKeyConstant.SHOP_STATUS);
        return Result.success(shopStatus != null ? shopStatus :0);
    }
}
