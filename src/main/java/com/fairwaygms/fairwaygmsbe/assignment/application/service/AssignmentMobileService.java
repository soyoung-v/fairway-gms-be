package com.fairwaygms.fairwaygmsbe.assignment.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.MyAssignmentDetailRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.MyAssignmentRes;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.CartAssignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.AssignmentStatus;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.CartAssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.exception.AssignmentErrorCode;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.exception.CaddieErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentMobileService {

    private final AssignmentRepository assignmentRepository;
    private final CartAssignmentRepository cartAssignmentRepository;
    private final CaddieRepository caddieRepository;

    // API-518: 캐디 본인 배정 목록 — targetDate 미입력 시 오늘 기준 (FR-526)
    public List<MyAssignmentRes> getMyAssignments(LocalDate targetDate, AuthenticatedUser auth) {
        validateCaddy(auth);
        Caddie caddie = findCaddieByUserId(auth.getUserId());
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();

        List<Assignment> assignments = assignmentRepository.findConfirmedByCaddieAndDate(caddie.getId(), date);
        Map<Long, CartAssignment> cartByTeeTimeId = buildCartMap(caddie.getGolfCourse().getId(), date);

        return assignments.stream()
                .map(a -> MyAssignmentRes.from(a,
                        cartByTeeTimeId.get(a.getReservationTeam().getTeeTime().getId())))
                .toList();
    }

    // API-519: 캐디 본인 티타임 상세 — 본인 배정만 조회 가능 (FR-527)
    public MyAssignmentDetailRes getMyAssignmentDetail(Long assignmentId, AuthenticatedUser auth) {
        validateCaddy(auth);
        Caddie caddie = findCaddieByUserId(auth.getUserId());

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND));

        // 타인 배정 조회 차단 — 존재 여부 노출 방지를 위해 404로 응답
        if (!assignment.getCaddie().getId().equals(caddie.getId())) {
            throw new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND);
        }
        // 확정 전(DRAFT 단계) 배정은 캐디에게 숨김
        if (assignment.getStatus() != AssignmentStatus.CONFIRMED
                && assignment.getStatus() != AssignmentStatus.COMPLETED) {
            throw new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND);
        }

        Map<Long, CartAssignment> cartByTeeTimeId =
                buildCartMap(caddie.getGolfCourse().getId(), assignment.getAssignmentDate());

        return MyAssignmentDetailRes.from(assignment,
                cartByTeeTimeId.get(assignment.getReservationTeam().getTeeTime().getId()));
    }

    private Map<Long, CartAssignment> buildCartMap(Long golfCourseId, LocalDate date) {
        return cartAssignmentRepository.findActiveByGolfCourseAndDate(golfCourseId, date)
                .stream()
                .collect(Collectors.toMap(
                        ca -> ca.getTeeTime().getId(),
                        ca -> ca,
                        (a, b) -> a));
    }

    private Caddie findCaddieByUserId(Long userId) {
        return caddieRepository.findByUser_IdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(CaddieErrorCode.CADDIE_NOT_FOUND));
    }

    private void validateCaddy(AuthenticatedUser auth) {
        if (auth.getRole() != UserRole.CADDY) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
