package com.gold.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gold.common.code.ErrorCode;
import com.gold.common.exception.BusinessException;
import com.gold.common.result.Result;
;
import com.gold.user.domain.dto.MerchantLoginDTO;
import com.gold.user.domain.pojo.Merchant;
import com.gold.user.domain.vo.MerchantLoginVO;
import com.gold.user.mapper.MerchantMapper;
import com.gold.user.service.MerchantService;
import com.gold.user.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;

    @Override
    public Result<MerchantLoginVO> login(MerchantLoginDTO dto) {
        Merchant merchant = merchantMapper.selectByUsername(dto.getUsername());
        if (merchant == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不存在");
        }
        if (!EncryptUtil.bcryptCheck(dto.getPassword(), merchant.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码错误");
        }
        if (merchant.getStatus() == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号已被禁用");
        }

        // 登录，token前缀用 M 区分商家
        StpUtil.login("M" + merchant.getId());

        MerchantLoginVO vo = new MerchantLoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setMerchantId(merchant.getId());
        vo.setMerchantName(merchant.getMerchantName());

        return Result.success(vo);
    }
}