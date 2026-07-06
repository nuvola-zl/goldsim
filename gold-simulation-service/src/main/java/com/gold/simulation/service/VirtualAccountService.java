package com.gold.simulation.service;

public interface VirtualAccountService {
    /**
     * 初始化用户虚拟账户
     * @param userId 用户ID
     */
    void initAccount(Long userId);
}
