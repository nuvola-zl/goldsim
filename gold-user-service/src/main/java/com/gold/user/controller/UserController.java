package com.gold.user.controller;

import com.gold.api.feign.FileFeignApi;
import com.gold.common.result.Result;
import com.gold.user.domain.dto.UserLoginDTO;
import com.gold.user.domain.vo.UserLoginVO;
import com.gold.user.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Slf4j  // ← 加日志
@RestController
@RequestMapping("user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    private final FileFeignApi fileFeignApi;

    /**
     * 注册用户
     */
    @PostMapping("register")
    public void register(@RequestBody @Valid UserLoginDTO userLoginDTO ) {
        userService.register(userLoginDTO);
    }
    /**
     * 登录用户
     */
    @PostMapping("login")
    public Result<UserLoginVO> login(@RequestBody @Valid UserLoginDTO userLoginDTO) {
        return userService.login(userLoginDTO);
    }


    /**
     * 上传用户头像
     */
    @PostMapping("uploadAvatar")
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file) {  // ← 改这里  // ← 加 @RequestParam

        if (file == null || file.isEmpty()) {

            return Result.error("A00001","请选择头像文件");
        }


        Result<String> result = fileFeignApi.upload(file);



        if (result == null) {
            log.error("【UserController】Feign 返回 null");
            return Result.error("A00001","头像上传失败: 服务无响应");
        }

        // 你的 Result 成功时 code=1
        if (result.isError()) {
            log.error("【UserController】Feign 返回错误: code={}, msg={}", result.getCode(), result.getMsg());
            return Result.error("A00001","头像上传失败: " + result.getMsg());
        }

        log.info("【UserController】上传成功，URL={}", result.getData());
        return Result.success(result.getData());
    }

    /**
     * 用户登出
     * 接口：POST /api/user/logout
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }
}
