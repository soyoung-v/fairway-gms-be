package com.fairwaygms.fairwaygmsbe;

import com.fairwaygms.fairwaygmsbe.auth.application.service.AdminBootstrapService;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import com.fairwaygms.fairwaygmsbe.auth.application.service.AdminUserService;
import com.fairwaygms.fairwaygmsbe.auth.application.service.AuthService;
import com.fairwaygms.fairwaygmsbe.auth.application.service.EmailService;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.CaddieMobileService;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.CaddieService;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.DailyStatusService;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.DesignatedCartService;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.QueueService;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.service.GolfCourseService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FairwayGmsBeApplicationTests {

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AdminBootstrapService adminBootstrapService;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private CaddieService caddieService;

    @MockitoBean
    private DesignatedCartService designatedCartService;

    @MockitoBean
    private DailyStatusService dailyStatusService;

    @MockitoBean
    private QueueService queueService;

    @MockitoBean
    private CaddieMobileService caddieMobileService;

    @MockitoBean
    private GolfCourseService golfCourseService;

    @MockitoBean
    private EmailService emailService;

    // JPA 비활성화 테스트 프로필에서 @EnableJpaAuditing과 충돌 방지
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void contextLoads() {
    }

}
