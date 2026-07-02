package com.fairwaygms.fairwaygmsbe.board.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.application.event.AssignmentConfirmedEvent;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardEventListener {

    private final AssignmentRepository assignmentRepository;
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

        String title = event.getScheduleDate() + " 배정 시간표";
        String content = buildScheduleContent(assignments);

        boardPostRepository.save(BoardPost.create(
                event.getGolfCourseId(),
                event.getConfirmedByUserId(),
                PostCategory.SCHEDULE_NOTICE,
                title, content));
    }

    private String buildScheduleContent(List<Assignment> assignments) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        // 부 번호 기준 오름차순 그룹핑 (OperationPeriod 없으면 0부로 처리)
        Map<Integer, List<Assignment>> byPeriod = new TreeMap<>(
                assignments.stream().collect(
                        Collectors.groupingBy(a -> {
                            TeeTime teeTime = a.getReservationTeam().getTeeTime();
                            return teeTime.getOperationPeriod() != null
                                    ? teeTime.getOperationPeriod().getPeriodNumber()
                                    : 0;
                        })
                )
        );

        StringBuilder sb = new StringBuilder();
        byPeriod.forEach((period, list) -> {
            sb.append("[").append(period).append("부]\n");
            list.stream()
                    .sorted(Comparator
                            .comparing((Assignment a) -> a.getReservationTeam().getTeeTime().getStartTime())
                            .thenComparing(a -> a.getReservationTeam().getTeeTime().getCourse().getName()))
                    .forEach(a -> {
                        TeeTime teeTime = a.getReservationTeam().getTeeTime();
                        String courseName = teeTime.getCourse().getName();
                        String teeUpTime = teeTime.getStartTime().format(timeFmt);
                        // 출근시간 = 티업시간 - 1시간
                        String arrivalTime = teeTime.getStartTime().minusHours(1).format(timeFmt);
                        String caddieName = a.getCaddie().getName();
                        String halfBack = Boolean.TRUE.equals(a.getIsHalfBack()) ? " [투근무]" : "";
                        sb.append(courseName).append(" ")
                                .append(teeUpTime)
                                .append(" (출근 ").append(arrivalTime).append(")")
                                .append(" - ").append(caddieName).append(" 캐디")
                                .append(halfBack).append("\n");
                    });
            sb.append("\n");
        });

        return sb.toString().trim();
    }
}
