package com.fairwaygms.fairwaygmsbe.caddie.application.service;

import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.RegisterDailyStatusReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.DailyStatusRes;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieDailyStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusType;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieDailyStatusRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.exception.CaddieErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyStatusService {

    private final CaddieRepository caddieRepository;
    private final CaddieDailyStatusRepository dailyStatusRepository;
    private final GolfCourseRepository golfCourseRepository;
    private final com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver contextResolver;

    // ADMIN은 X-Selected-Golf-Course-Id 헤더의 선택 골프장, MANAGER는 소속 골프장을 대상으로 한다
    private Long targetGolfCourseId(AuthenticatedUser auth) {
        return auth.isAdmin() ? contextResolver.resolveTargetGolfCourseId(auth) : auth.getGolfCourseId();
    }

    // FR-317~323: 일별 근무 상태 등록 (휴무/결근/당번/조출/특수근무/배정제외)
    public DailyStatusRes register(RegisterDailyStatusReq req, AuthenticatedUser auth) {
        validateManager(auth);

        Caddie caddie = caddieRepository.findByIdAndIsDeletedFalse(req.caddieId())
                .orElseThrow(() -> new BusinessException(CaddieErrorCode.CADDIE_NOT_FOUND));
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);

        // 당번(DUTY) 타입은 우선순위(FIRST/SECOND) 필수
        if (req.type() == DailyStatusType.DUTY && req.priority() == null) {
            throw new BusinessException(CaddieErrorCode.DUTY_PRIORITY_REQUIRED);
        }

        GolfCourse golfCourse = golfCourseRepository.findByIdAndIsDeletedFalse(caddie.getGolfCourse().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));

        CaddieDailyStatus status = CaddieDailyStatus.create(
                caddie, golfCourse, req.statusDate(), req.type(), req.priority(), req.note()
        );
        dailyStatusRepository.save(status);
        return DailyStatusRes.from(status);
    }

    // FR-315, FR-316: 일별 근무 상태 조회 (날짜 기준)
    @Transactional(readOnly = true)
    public List<DailyStatusRes> getByDate(Long golfCourseId, LocalDate statusDate, AuthenticatedUser auth) {
        validateManager(auth);
        Long targetId = targetGolfCourseId(auth);
        return dailyStatusRepository.findByGolfCourse_IdAndStatusDateAndIsDeletedFalse(targetId, statusDate)
                .stream()
                .map(DailyStatusRes::from)
                .toList();
    }

    // FR-316: 일별 근무 상태 삭제 (소프트 삭제)
    public void delete(Long statusId, AuthenticatedUser auth) {
        validateManager(auth);
        CaddieDailyStatus status = dailyStatusRepository.findByIdAndIsDeletedFalse(statusId)
                .orElseThrow(() -> new BusinessException(CaddieErrorCode.DAILY_STATUS_NOT_FOUND));
        validateGolfCourseAccess(status.getGolfCourse().getId(), auth);
        status.delete();
    }

    private void validateManager(AuthenticatedUser auth) {
        if (auth.getRole() != UserRole.MANAGER && !auth.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateGolfCourseAccess(Long caddieGolfCourseId, AuthenticatedUser auth) {
        if (auth.isAdmin()) return;
        if (!caddieGolfCourseId.equals(targetGolfCourseId(auth))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
