package com.gold.user.controller;

import com.gold.api.domain.dto.UserInfoDTO;
import com.gold.common.context.UserContext;
import com.gold.common.result.Result;
import com.gold.user.domain.dto.UserPasswordDTO;
import com.gold.user.domain.dto.UserRealNameDTO;
import com.gold.user.domain.dto.UserUpdateDTO;
import com.gold.user.domain.vo.UserInfoVO;
import com.gold.user.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserInfoController {

    private final IUserService userService;

    /**
     * 根据用户id查询用户信息
     */
    @GetMapping("/info")
    // TODO [安全漏洞-PII枚举] 无所有权校验，任何认证用户可查询任意 userId 的姓名、手机号、身份证等个人信息。应校验 userId 是否等于 UserContext.getUserId()，或仅允许本人/管理员查询
    public Result<UserInfoVO> getUserInfo() {
        Long userId = UserContext.getUserId();
        return userService.getUserInfo(userId);
    }

    /**
     * 修改用户信息
     * @param userUpdateDTO
     */
    @PostMapping("/update")
    // TODO [安全漏洞-越权修改] 未校验操作者身份。updateUserInfo 应校验 DTO 中的 userId（如有）与 UserContext.getUserId() 一致，防止用户 A 修改用户 B 的账户信息
    public Result<String> updateUserInfo(@Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        userService.updateUserInfo(userUpdateDTO);
        return Result.success("修改成功");
    }
    /**
     * 修改用户密码
     */
    @PostMapping("/updatePassword")
    // TODO [安全漏洞-越权修改] 同上，修改密码也应校验操作者身份，确保只能修改自己的密码
    public Result<String> updatePassword(@Valid @RequestBody UserPasswordDTO userPasswordDTO) {
        userService.updatePassword(userPasswordDTO);
        return Result.success("修改成功");
    }

    /**
     * 实名认证
     */
    @PostMapping("/realName")
    // TODO [安全漏洞-越权修改] 同上，实名认证也应校验操作者身份，防止替他人实名
    public Result<String> realName(@Valid @RequestBody UserRealNameDTO userRealNameDTO) {
        userService.realName(userRealNameDTO);
        return Result.success("实名认证成功");
    }


}
