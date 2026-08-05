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

    /** 允许上传的图片扩展名 */
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};

    private final AliOssUtil aliOssUtil;

    public CommondController(AliOssUtil aliOssUtil) {
        this.aliOssUtil = aliOssUtil;
    }

    /**
     * 文件上传（图片）
     *
     * @param file 上传的文件
     * @return 文件访问URL
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(@RequestParam MultipartFile file){
        try {
            // 入参校验：文件不能为空
            if (file == null || file.isEmpty()) {
                return Result.error("上传文件不能为空");
            }

            //获取文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                return Result.error("文件名不能为空");
            }

            // 校验文件类型（仅允许常见图片格式）
            String lowerName = originalFilename.toLowerCase();
            boolean allowed = false;
            for (String ext : ALLOWED_EXTENSIONS) {
                if (lowerName.endsWith(ext)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                return Result.error("仅支持上传jpg/jpeg/png/gif/bmp/webp格式的图片");
            }

            log.info("文件上传: {}", originalFilename);
            // 生成唯一对象名，避免文件名冲突
            String substring = originalFilename.substring(originalFilename.lastIndexOf("."));
            String objectName = UUID.randomUUID().toString() + substring;

            // 上传至阿里云OSS
            String pathSite = aliOssUtil.upload(file.getBytes(), objectName);
            log.info("文件上传成功：{}", pathSite);
            return Result.success(pathSite);
        } catch (Exception e) {
            // 非业务异常统一处理，避免异常信息泄露给前端
            log.error("文件上传失败: {}", e.getMessage(), e);
            return Result.error("文件上传失败");
        }
    }
}
