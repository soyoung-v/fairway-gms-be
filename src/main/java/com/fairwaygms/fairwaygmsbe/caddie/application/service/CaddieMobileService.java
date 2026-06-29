package com.fairwaygms.fairwaygmsbe.caddie.application.service;

import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.MyCaddieRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.MyQueueRes;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieWorkPattern;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieQueueRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieWorkPatternRepository;
import com.fairwaygms.fairwaygmsbe.caddie.exception.CaddieErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaddieMobileService {

    private final CaddieRepository caddieRepository;
    private final CaddieWorkPatternRepository workPatternRepository;
    private final CaddieQueueRepository queueRepository;

    // FR-325: 캐디 본인 기본정보 + 근무패턴 조회 — userId로 연동된 caddie 조회
    public MyCaddieRes getMyInfo(AuthenticatedUser auth) {
        validateCaddy(auth);
        Caddie caddie = findCaddieByUserId(auth.getUserId());

        // 근무 패턴은 없을 수 있음 (초기 승인 직후 등)
        CaddieWorkPattern pattern = workPatternRepository
                .findByCaddie_IdAndIsDeletedFalse(caddie.getId())
                .orElse(null);

        return MyCaddieRes.of(caddie, pattern);
    }

    // FR-326: 캐디 본인 대기 순번 조회 — queueDate 미입력 시 오늘 날짜 기준
    public MyQueueRes getMyQueue(AuthenticatedUser auth, LocalDate queueDate) {
        validateCaddy(auth);
        Caddie caddie = findCaddieByUserId(auth.getUserId());

        LocalDate targetDate = queueDate != null ? queueDate : LocalDate.now();

        // 순번 미등록이면 null 반환 (배정 전 상태)
        Integer queueNumber = queueRepository
                .findByCaddie_IdAndQueueDateAndIsDeletedFalse(caddie.getId(), targetDate)
                .map(q -> q.getQueueNumber())
                .orElse(null);

        return MyQueueRes.of(queueNumber, targetDate);
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
