package com.gold.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gold.api.domain.dto.UserInfoDTO;
import com.gold.common.result.Result;
import com.gold.user.domain.dto.UserLoginDTO;
import com.gold.user.domain.dto.UserPasswordDTO;
import com.gold.user.domain.dto.UserRealNameDTO;
import com.gold.user.domain.dto.UserUpdateDTO;
import com.gold.user.domain.pojo.User;
import com.gold.user.domain.vo.UserInfoVO;
import com.gold.user.domain.vo.UserLoginVO;
import jakarta.validation.Valid;

public interface IUserService extends IService<User> {
    /**
     * 用户注册
     */
    void register(@Valid UserLoginDTO userLoginDTO);

    /**
     * 用户登录
     */
    Result<UserLoginVO> login(@Valid UserLoginDTO userLoginDTO);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 获取用户信息
     * @param userId
     * @return
     */
    Result<UserInfoVO> getUserInfo(Long userId);

    /**
     * 修改用户信息
     */
    Result<String> updateUserInfo(@Valid UserUpdateDTO userUpdateDTO);

    /**
     * 修改用户密码
     */
    void updatePassword(@Valid UserPasswordDTO userPasswordDTO);

    /**
     * 实名认证
     */
    void realName(@Valid UserRealNameDTO userRealNameDTO);
}
