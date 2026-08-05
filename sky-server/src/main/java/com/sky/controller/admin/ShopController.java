package com.sky.controller.admin;

import com.sky.constant.RedisKeyConstant;
import com.sky.constant.StatusConstant;
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

    /**
     * 修改店铺营业状态
     *
     * @param status 0打烊 1营业
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("修改店铺营业状态")
    public Result<String> updateStatus(@PathVariable Integer status){
        // 入参校验：状态只能是0（打烊）或1（营业）
        if (!StatusConstant.ENABLE.equals(status) && !StatusConstant.DISABLE.equals(status)) {
            return Result.error("营业状态只能为0或1");
        }
        log.info("修改店铺营业状态：{}", status);
        redisTemplate.opsForValue().set(RedisKeyConstant.SHOP_STATUS, status);
        return Result.success();
    }

}
