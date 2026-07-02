package com.fairwaygms.fairwaygmsbe.notification.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.application.event.AssignmentConfirmedEvent;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.AssignmentStatus;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.board.application.event.BoardPostCreatedEvent;
import com.fairwaygms.fairwaygmsbe.board.application.event.SwapRequestProcessedEvent;
import com.fairwaygms.fairwaygmsbe.board.domain.enums.SwapRequestStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.notification.domain.enums.NotificationType;
import com.fairwaygms.fairwaygmsbe.notification.domain.enums.ReferenceType;
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
public class NotificationEventListener {

    private final FcmPushService fcmPushService;
    private final NotificationService notificationService;
    private final CaddieRepository caddieRepository;
    private final AssignmentRepository assignmentRepository;

    // 게시글 등록 → 같은 골프장 캐디 전체에게 알림
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBoardPostCreated(BoardPostCreatedEvent event) {
        List<Caddie> caddies = caddieRepository.findByGolfCourse_IdAndIsDeletedFalse(event.getGolfCourseId());
        if (caddies.isEmpty()) return;

        List<Long> userIds = caddies.stream()
                .map(c -> c.getUser().getId())
                .toList();

        String title = "새 공지사항";
        String content = event.getTitle();

        fcmPushService.sendPushToAll(userIds, title, content);

        userIds.forEach(userId ->
                notificationService.createNotification(
                        event.getGolfCourseId(), userId,
                        NotificationType.BOARD_POST_CREATED, title, content,
                        event.getPostId(), ReferenceType.BOARD_POST));
    }

    // 교환 요청 처리(승인/거절) → 요청자 캐디에게 알림
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSwapRequestProcessed(SwapRequestProcessedEvent event) {
        Caddie caddie = caddieRepository.findById(event.getRequesterCaddieId()).orElse(null);
        if (caddie == null) {
            log.warn("교환 요청 알림 실패 — caddieId={} 없음", event.getRequesterCaddieId());
            return;
        }

        Long userId = caddie.getUser().getId();
        boolean approved = event.getStatus() == SwapRequestStatus.APPROVED;
        String title = approved ? "교환 요청 승인" : "교환 요청 거절";
        String content = approved
                ? "순번 교환 요청이 승인되었습니다."
                : "순번 교환 요청이 거절되었습니다." + (event.getRejectReason() != null ? " 사유: " + event.getRejectReason() : "");

        fcmPushService.sendPush(userId, title, content);

        notificationService.createNotification(
                caddie.getGolfCourse().getId(), userId,
                NotificationType.SWAP_RESULT, title, content,
                event.getSwapRequestId(), ReferenceType.SWAP_REQUEST);
    }

    // 배정표 확정 → 해당 날짜 배정된 캐디 각자에게 알림
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAssignmentConfirmed(AssignmentConfirmedEvent event) {
        List<Assignment> assignments = assignmentRepository.findByGolfCourseAndDateAndStatus(
                event.getGolfCourseId(), event.getScheduleDate(), AssignmentStatus.CONFIRMED);

        if (assignments.isEmpty()) return;

        String title = "배정표 확정";
        String content = event.getScheduleDate() + " 배정표가 확정되었습니다.";

        List<Long> userIds = assignments.stream()
                .map(a -> a.getCaddie().getUser().getId())
                .distinct()
                .toList();

        fcmPushService.sendPushToAll(userIds, title, content);

        assignments.forEach(a -> notificationService.createNotification(
                event.getGolfCourseId(),
                a.getCaddie().getUser().getId(),
                NotificationType.ASSIGNMENT_CONFIRMED, title, content,
                a.getId(), ReferenceType.ASSIGNMENT));
    }
}
