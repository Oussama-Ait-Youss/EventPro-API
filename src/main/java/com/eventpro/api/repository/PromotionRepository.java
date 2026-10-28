package com.eventpro.api.repository;

import com.eventpro.api.model.Promotion;
import com.eventpro.api.model.enums.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion,Long> {
    boolean existsByCodeIgnoreCase(String code);

    boolean existsByEventId(Long eventId);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    List<Promotion> findByStatus(PromotionStatus status);
}
