package com.gold.user.service;

import com.gold.common.result.Result;
import com.gold.user.domain.dto.MerchantLoginDTO;
import com.gold.user.domain.vo.MerchantLoginVO;


public interface MerchantService {

    Result<MerchantLoginVO> login(MerchantLoginDTO dto);
}