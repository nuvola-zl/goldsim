package com.gold.simulation.listener;

import com.gold.common.config.RabbitMQConfig;
import com.gold.common.message.UserRegisterMessage;
import com.gold.simulation.service.VirtualAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisterListener {

    private final VirtualAccountService accountService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_USER_REGISTER)
    public void onUserRegister(UserRegisterMessage message) {
        log.info("收到用户注册消息，userId={}", message.getUserId());
        accountService.initAccount(message.getUserId());
    }
}