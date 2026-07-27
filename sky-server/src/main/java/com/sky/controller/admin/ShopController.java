package com.sky.controller.admin;

import com.sky.constant.RedisKeyConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺管理")
@Slf4j
public class ShopController {

    @Autowired
    RedisTemplate redisTemplate;
    /*
    查询店铺营业状态
     */
    @GetMapping("/status")
    @ApiOperation("查询店铺营业状态")
    public Result<Integer> getStatus(){
        log.info("查询店铺营业状态");
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(RedisKeyConstant.SHOP_STATUS);
        return Result.success(shopStatus != null ? shopStatus :0);
    }

    /*
    修改店铺营业状态
     */
    @PutMapping("/status/{status}")
    @ApiOperation("修改店铺营业状态")
    public Result updateStatus(@PathVariable Integer status){
        if (status == null || (status !=0 && status !=1)) {
            return Result.error("营业状态只能为0或1喵");
        }
        log.info("修改店铺营业状态：{}", status);
        redisTemplate.opsForValue().set(RedisKeyConstant.SHOP_STATUS, status);
        return Result.success();
    }

}
