package com.fairwaygms.fairwaygmsbe.board.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.application.event.AssignmentConfirmedEvent;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.CartAssignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.CartAssignmentRepository;
import com.fairwaygms.fairwaygmsbe.board.domain.entity.BoardPost;
import com.fairwaygms.fairwaygmsbe.board.domain.enums.PostCategory;
import com.fairwaygms.fairwaygmsbe.board.domain.repository.BoardPostRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardEventListener {

    private final AssignmentRepository assignmentRepository;
    private final CartAssignmentRepository cartAssignmentRepository;
    private final BoardPostRepository boardPostRepository;

    // 배정표 확정 → SCHEDULE_NOTICE 게시글 자동 생성 (FCM은 NotificationEventListener가 처리)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAssignmentConfirmed(AssignmentConfirmedEvent event) {
        List<Assignment> assignments = assignmentRepository.findByGolfCourseAndDateWithDetails(
                event.getGolfCourseId(), event.getScheduleDate());

        if (assignments.isEmpty()) {
            log.warn("시간표 게시글 생성 생략 — 배정 없음 (golfCourseId={}, date={})",
                    event.getGolfCourseId(), event.getScheduleDate());
            return;
        }

        // teeTimeId → CartAssignment 맵 (카트 정보 조회)
        Map<Long, CartAssignment> cartByTeeTimeId = cartAssignmentRepository
                .findActiveByGolfCourseAndDate(event.getGolfCourseId(), event.getScheduleDate())
                .stream()
                .collect(Collectors.toMap(
                        ca -> ca.getTeeTime().getId(),
                        ca -> ca,
                        (a, b) -> a));

        String title = event.getScheduleDate() + " 배정 시간표";
        String content = buildScheduleContent(assignments, cartByTeeTimeId);

        boardPostRepository.save(BoardPost.create(
                event.getGolfCourseId(),
                event.getConfirmedByUserId(),
                PostCategory.SCHEDULE_NOTICE,
                title, content));
    }

    private String buildScheduleContent(List<Assignment> assignments,
                                         Map<Long, CartAssignment> cartByTeeTimeId) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        StringBuilder sb = new StringBuilder();

        // 부 번호 기준 오름차순 그룹핑
        Map<Integer, List<Assignment>> byPeriod = new TreeMap<>(
                assignments.stream().collect(
                        Collectors.groupingBy(a -> a.getReservationTeam().getTeeTime()
                                .getOperationPeriod().getPeriodNumber()))
        );

        byPeriod.forEach((period, periodList) -> {
            sb.append("━━━━━━ ").append(period).append("부 ━━━━━━\n\n");

            // 조별 그룹핑 — 조 이름 기준 삽입 순서 유지 (티타임 오름차순으로 정렬 후 그룹핑)
            Map<String, List<Assignment>> byGroup = new LinkedHashMap<>();
            periodList.stream()
                    .sorted(Comparator
                            .comparing((Assignment a) -> a.getReservationTeam().getTeeTime().getStartTime())
                            .thenComparing(a -> a.getReservationTeam().getTeeTime().getCourse().getName()))
                    .forEach(a -> {
                        String groupName = a.getCaddie().getCaddieGroup() != null
                                ? a.getCaddie().getCaddieGroup().getName()
                                : "미편성";
                        byGroup.computeIfAbsent(groupName, k -> new ArrayList<>()).add(a);
                    });

            byGroup.forEach((groupName, groupList) -> {
                sb.append("[").append(groupName).append("]\n");
                groupList.forEach(a -> {
                    TeeTime tt = a.getReservationTeam().getTeeTime();
                    String teeUpTime = tt.getStartTime().format(timeFmt);
                    String arrivalTime = tt.getStartTime().minusHours(1).format(timeFmt);
                    String courseName = tt.getCourse().getName();
                    String caddieName = a.getCaddie().getName();

                    CartAssignment cart = cartByTeeTimeId.get(tt.getId());
                    String cartInfo = cart != null ? "  " + cart.getCart().getCartNumber() + "호카트" : "";
                    String halfBack = Boolean.TRUE.equals(a.getIsHalfBack()) ? "  [투근무]" : "";

                    sb.append(" ")
                            .append(caddieName).append("  ")
                            .append(courseName).append("  ")
                            .append(teeUpTime).append("  (출근 ").append(arrivalTime).append(")")
                            .append(cartInfo)
                            .append(halfBack)
                            .append("\n");
                });
                sb.append("\n");
            });
        });

        return sb.toString().trim();
    }
}
