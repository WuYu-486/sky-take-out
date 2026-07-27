package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品管理")
public class DishController {

    @Autowired
    private DishService dishService;
    /*
    新增菜品
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result<String> save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}", dishDTO);
        dishService.saveDishAndFlavor(dishDTO);
        return Result.success();
    }

    /*
    菜品分页查询
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询：{}", dishPageQueryDTO);
        return dishService.page(dishPageQueryDTO);
    }

    /*
    批量删除菜品及其口味
     */
    @DeleteMapping
    @ApiOperation("批量删除菜品及其口味")
    public Result<String> delete(@RequestParam List<Long> ids){
        log.info("批量删除菜品：{}", ids);
        dishService.delete(ids);
        return Result.success();
    }

    /*
    根据id查询菜品和口味数据
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品和口味数据")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品和口味数据：{}", id);
        return dishService.getById(id);
    }

    /*
    根据id修改菜品和口味数据
     */
    @PutMapping
    @ApiOperation("根据id修改菜品和口味数据")
    public Result<String> update(@RequestBody DishDTO dishDTO){
        log.info("根据id修改菜品和口味数据：{}", dishDTO);
        dishService.update(dishDTO);
        return Result.success();
    }

    /*
    根据分类id查询菜品
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId){
        log.info("根据分类id查询菜品：{}", categoryId);
        return dishService.list(categoryId);
    }
}
