package com.fairwaygms.fairwaygmsbe;

import com.fairwaygms.fairwaygmsbe.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FairwayGmsBeApplicationTests {

    @MockitoBean
    private AuthService authService;

    @Test
    void contextLoads() {
    }

}
