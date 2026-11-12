
package com.eventpro.api.repository;

import com.eventpro.api.model.Payment;
import com.eventpro.api.model.enums.PaymentStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByReservation_Id(Long reservationId);

    Optional<Payment> findFirstByReservation_IdAndStatusOrderByIdDesc(
            Long reservationId,
            PaymentStatus status
    );

    boolean existsByReservation_IdAndStatus(
            Long reservationId,
            PaymentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);
}
