package com.gold.user.controller;

import com.gold.common.result.Result;

import com.gold.user.domain.dto.MerchantLoginDTO;
import com.gold.user.domain.vo.MerchantLoginVO;
import com.gold.user.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/login")
    public Result<MerchantLoginVO> login(@Valid @RequestBody MerchantLoginDTO dto) {
        return merchantService.login(dto);
    }
}