package com.gold.user;

import com.gold.user.domain.dto.UserLoginDTO;
import com.gold.user.service.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserRegisterTest {

    @Autowired
    private IUserService userService;

    @Test
    void testRegister() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("test009");
        dto.setPassword("123456");
        dto.setPhone("13800138111");
        userService.register(dto);
        System.out.println("注册执行成功，MQ消息已发送");
    }
}