package com.fairwaygms.fairwaygmsbe.caddie.domain.repository;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaddieRepository extends JpaRepository<Caddie, Long> {

    Optional<Caddie> findByIdAndIsDeletedFalse(Long id);

    List<Caddie> findByGolfCourse_IdAndIsDeletedFalse(Long golfCourseId);

    List<Caddie> findByGolfCourse_IdAndStatusAndIsDeletedFalse(Long golfCourseId, CaddieStatus status);

    boolean existsByGolfCourse_IdAndCaddieNumberAndIsDeletedFalse(Long golfCourseId, String caddieNumber);

    Optional<Caddie> findByUser_IdAndIsDeletedFalse(Long userId);

    Optional<Caddie> findByGolfCourse_IdAndNameAndIsDeletedFalse(Long golfCourseId, String name);
}
