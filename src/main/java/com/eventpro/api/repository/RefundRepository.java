package com.eventpro.api.repository;

import com.eventpro.api.model.Refund;
import com.eventpro.api.model.enums.RefundStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByPayment_Id(Long paymentId);

    List<Refund> findByStatus(RefundStatus status);
}