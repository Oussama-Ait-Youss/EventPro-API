package com.eventpro.api.repository;

import com.eventpro.api.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<Promotion,Long> {
    boolean existsByCodeIgnoreCase(String code);

    boolean existsByEventId(Long eventId);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
