package com.fairwaygms.fairwaygmsbe.common.context;

import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// 역할에 따라 현재 요청이 어느 골프장을 대상으로 하는지 결정하는 컴포넌트.
//
// 규칙:
//   - ADMIN  → 요청 헤더 X-Selected-Golf-Course-Id에서 골프장 ID를 읽는다.
//   - MANAGER, CADDY → 로그인 토큰에 있는 소속 골프장 ID를 그대로 사용한다.
//
// 중요: RequestBody나 QueryParam의 golfCourseId는 권한 판단 기준으로 사용하지 않는다.
//       사용자가 임의로 다른 골프장 ID를 넣어도 이 메서드에서 차단된다.
@Component
public class GolfCourseContextResolver {

    // ADMIN이 골프장 범위 API를 호출할 때 보내야 하는 요청 헤더 이름
    public static final String SELECTED_GOLF_COURSE_HEADER = "X-Selected-Golf-Course-Id";

    // 현재 로그인한 사용자의 역할을 기준으로 대상 골프장 ID를 반환한다.
    // TODO: JWT 인증 구현 후 SecurityContext에서 직접 AuthenticatedUser를 꺼내는 방식으로 개선 예정
    public Long resolveTargetGolfCourseId(AuthenticatedUser user) {
        return switch (user.getRole()) {
            // ADMIN은 헤더에서 선택한 골프장을 읽는다
            case ADMIN -> resolveAdminGolfCourseId();
            // MANAGER, CADDY는 자신의 소속 골프장을 사용한다
            case MANAGER, CADDY -> resolveOwnGolfCourseId(user);
        };
    }

    // ADMIN 전용: X-Selected-Golf-Course-Id 헤더에서 골프장 ID를 읽어 반환한다.
    // 헤더가 없거나 숫자가 아니면 예외를 던진다.
    private Long resolveAdminGolfCourseId() {
        HttpServletRequest request = getCurrentRequest();
        String header = request.getHeader(SELECTED_GOLF_COURSE_HEADER);
        if (header == null || header.isBlank()) {
            throw new BusinessException(ErrorCode.GOLF_COURSE_REQUIRED);
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.GOLF_COURSE_REQUIRED,
                    "유효하지 않은 골프장 ID: " + header);
        }
    }

    // MANAGER / CADDY 전용: 로그인 토큰에 저장된 소속 골프장 ID를 반환한다.
    // 소속 골프장이 없으면 (정상적인 경우에는 없어야 함) 예외를 던진다.
    private Long resolveOwnGolfCourseId(AuthenticatedUser user) {
        Long golfCourseId = user.getGolfCourseId();
        if (golfCourseId == null) {
            throw new BusinessException(ErrorCode.GOLF_COURSE_REQUIRED,
                    "소속 골프장 정보가 없습니다.");
        }
        return golfCourseId;
    }

    // 현재 HTTP 요청 객체를 가져온다. Spring MVC 요청 처리 중에만 호출 가능하다.
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
}
