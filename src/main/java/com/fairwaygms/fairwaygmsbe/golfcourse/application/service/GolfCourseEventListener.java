package com.fairwaygms.fairwaygmsbe.golfcourse.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.application.event.AssignmentCompletedEvent;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.CartAssignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.CartAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GolfCourseEventListener {

    private final CartAssignmentRepository cartAssignmentRepository;

    // 배정 완료 → 당일 배정된 카트를 AVAILABLE 상태로 복원
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAssignmentCompleted(AssignmentCompletedEvent event) {
        List<CartAssignment> cartAssignments = cartAssignmentRepository.findByGolfCourseAndDate(
                event.getGolfCourseId(), event.getScheduleDate());

        if (cartAssignments.isEmpty()) {
            log.debug("카트 복원 — 당일 카트 배정 없음 (golfCourseId={}, date={})",
                    event.getGolfCourseId(), event.getScheduleDate());
            return;
        }

        cartAssignments.forEach(ca -> ca.getCart().markAvailable());
        log.info("카트 복원 완료 — {}대 (golfCourseId={}, date={})",
                cartAssignments.size(), event.getGolfCourseId(), event.getScheduleDate());
    }
}
