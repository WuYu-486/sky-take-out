package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/setmeal")
@Api(tags = "套餐相关接口")
public class SetmealController {

    @Autowired
    SetmealService setmealService;

    /*
    新增套餐
     */
    @PostMapping
    @ApiOperation("新增套餐")
    public Result<String> save(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐：{}", setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }

    /*
    分页查询
     */
    @GetMapping("/page")
    @ApiOperation("分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询：{}",setmealPageQueryDTO );
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /*
    根据id查询
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("根据id查询：{}", id);
        SetmealVO setmealVO = setmealService.getById(id);
        return Result.success(setmealVO);
    }

    /*
    修改套餐
     */
    @PutMapping
    @ApiOperation("修改套餐")
    public Result<String> update(@RequestBody SetmealDTO setmealDTO){
        log.info("修改套餐：{}", setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }

    /*
    批量删除
     */
    @DeleteMapping
    @ApiOperation("批量删除")
    public Result<String> delete(@RequestParam List<Long> ids){
        log.info("批量删除：{}", ids);
        setmealService.delete(ids);
        return Result.success();
    }

    /*
    起售停售
     */
    @PostMapping("/status/{status}")
    @ApiOperation("起售停售")
    public Result<String> status(@PathVariable Integer status, Long id){
        log.info("起售停售：{}", status);
        setmealService.status(status, id);
        return Result.success();
    }
}
