package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Api(tags = "通用接口")
@Slf4j
@RestController
@RequestMapping("/admin/common")
public class CommondController {

    private final AliOssUtil aliOssUtil;

    public CommondController(AliOssUtil aliOssUtil) {
        this.aliOssUtil = aliOssUtil;
    }

    /*
        文件上传
         */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(@RequestParam MultipartFile file){
        log.info("文件上传: {}", file.getOriginalFilename());

        try {
            //获取文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                return Result.error("文件名不能为空");
            }
            String substring = originalFilename.substring(originalFilename.lastIndexOf("."));
            String objectName = UUID.randomUUID().toString() + substring;

            String pathSite = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(pathSite);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return Result.error("文件上传失败");
        }

    }
}
