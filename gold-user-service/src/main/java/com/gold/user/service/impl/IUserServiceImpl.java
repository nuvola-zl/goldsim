package com.gold.user.service.impl;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gold.api.domain.dto.UserInfoDTO;
import com.gold.common.code.ErrorCode;
import com.gold.common.config.RabbitMQConfig;
import com.gold.common.context.UserContext;
import com.gold.common.exception.BusinessException;
import com.gold.common.message.UserRegisterMessage;
import com.gold.common.result.Result;
import com.gold.user.domain.dto.UserLoginDTO;
import com.gold.user.domain.dto.UserPasswordDTO;
import com.gold.user.domain.dto.UserRealNameDTO;
import com.gold.user.domain.dto.UserUpdateDTO;
import com.gold.user.domain.vo.UserInfoVO;
import com.gold.user.domain.vo.UserLoginVO;
import com.gold.user.mapper.UserMapper;
import com.gold.user.domain.pojo.User;
import com.gold.user.service.IUserService;
import com.gold.user.util.EncryptUtil;
import com.gold.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IUserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;

    @Override
    public void register(UserLoginDTO userLoginDTO) {
        //判断用户名是否存在
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, userLoginDTO.getUsername()));
        if (user != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名已存在,请重新输入");
        }
         user=User.builder()
                .username(userLoginDTO.getUsername())
                .password(EncryptUtil.bcryptEncrypt(userLoginDTO.getPassword()))
                .phone(userLoginDTO.getPhone())
                .realNameVerified(0)
                .role(0)
                .realBalance(BigDecimal.ZERO)
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        //2.将User保存到数据库
        saveOrUpdate(user);

        // ========== 修正：同步昵称到排行榜 Redis ==========
        // 用 user.getId()（刚生成的ID），用 username 当昵称
        stringRedisTemplate.opsForHash().put(
                "sim:user:nickname",
                user.getId().toString(),      // 用刚保存的 userId
                user.getUsername()              //  用用户名当昵称
        );
        // ================================================

        // 发送 MQ 消息，通知模拟盘服务初始化虚拟账户
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_USER,
                RabbitMQConfig.ROUTING_KEY_REGISTER,
                new UserRegisterMessage(user.getId())
        );


    }

    @Override
    public Result<UserLoginVO> login(UserLoginDTO userLoginDTO) {
        // 1. 判断用户名是否存在
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, userLoginDTO.getUsername()));
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名不存在，请重新输入");
        }
        // 2. 判断密码是否正确
        if (!EncryptUtil.bcryptCheck(userLoginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码错误，请重新输入");
        }
        // 3. 判断手机号是否匹配
        if (!userLoginDTO.getPhone().equals(user.getPhone())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号错误，请重新输入");
        }

        // 4. 生成Sa-Token登录令牌
        String token = JwtUtil.createToken(user.getId());

        // 5. 实体拷贝到VO，手动赋值token
        UserLoginVO vo = BeanUtil.copyProperties(user, UserLoginVO.class);
        vo.setToken(token);

        return Result.success(vo);
    }

    @Override
    public void logout() {
        // 获取当前登录用户ID
        Long userId = UserContext.getUserId();

        // Sa-Token 注销当前会话（清除Redis中的token信息）
        StpUtil.logout();

        // 可选：记录登出日志
        // log.info("用户登出成功, userId={}", userId);
    }

    // ==================== 查询用户信息 ====================

    @Override
    public Result<UserInfoVO> getUserInfo(Long userId) {
        User user = getById(userId);
        if (user == null) {
            return Result.error(ErrorCode.RESOURCE_NOT_FOUND,"用户不存在");
        }
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);

        // 脱敏处理
        if (user.getPhone() != null) {
            vo.setPhone(desensitizePhone(user.getPhone()));
        }
        if (user.getIdCard() != null) {
            // 数据库里存的是 AES 密文，先解密再脱敏展示
            String decrypted = EncryptUtil.aesDecrypt(user.getIdCard());
            vo.setIdCard(desensitizeIdCard(decrypted != null ? decrypted : user.getIdCard()));
        }

        return Result.success(vo);
    }

    /**
     * 修改用户信息
     */
    @Override
    public Result<String> updateUserInfo(UserUpdateDTO userUpdateDTO) {
        Long userId= UserContext.getUserId();
        User user = getById(userId);
        if (user == null) {
            return Result.error(ErrorCode.RESOURCE_NOT_FOUND,"用户不存在");
        }
        BeanUtils.copyProperties(userUpdateDTO, user);
        updateById(user);
        return Result.success("修改成功");
    }

    /**
     * 修改用户密码
     */
    @Override
    public void updatePassword(UserPasswordDTO userPasswordDTO) {
        Long userId= UserContext.getUserId();
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户不存在");
        }
        // 2. 判断原密码是否正确
        if (!EncryptUtil.bcryptCheck(userPasswordDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "原密码错误，请重新输入");
        }
        //判断两次输入的密码是否一致
        if (!userPasswordDTO.getNewPassword().equals(userPasswordDTO.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致，请重新输入");
        }


        // 4. 更新密码
        user.setPassword(EncryptUtil.bcryptEncrypt(userPasswordDTO.getNewPassword()));
        updateById(user);
    }

    /**
     * 实名认证
     */
    @Override
    public void realName(UserRealNameDTO userRealNameDTO) {
        Long userId = UserContext.getUserId();
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户不存在");
        }
        if (user.getRealNameVerified() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户已实名认证");
        }

        // 赋值
        user.setRealName(userRealNameDTO.getRealName());

        // ========== 修正：身份证加密存储 ==========
        user.setIdCard(EncryptUtil.aesEncrypt(userRealNameDTO.getIdCard()));
        // ========================================

        user.setRealNameVerified(1);
        updateById(user);
    }


    // ==================== 脱敏工具方法 ====================

    private String desensitizePhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String desensitizeIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "************" + idCard.substring(idCard.length() - 2);
    }

}
