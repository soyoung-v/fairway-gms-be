package com.fairwaygms.fairwaygmsbe.caddie.domain.repository;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieDesignatedCart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaddieDesignatedCartRepository extends JpaRepository<CaddieDesignatedCart, Long> {

    // 캐디당 활성 지정카트는 1건만 허용 — Service에서 신규 등록 전 기존 건 비활성화 처리
    Optional<CaddieDesignatedCart> findByCaddie_IdAndIsActiveTrueAndIsDeletedFalse(Long caddieId);

    boolean existsByCaddie_IdAndCart_IdAndIsDeletedFalse(Long caddieId, Long cartId);
}
