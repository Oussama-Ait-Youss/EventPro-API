package com.eventpro.api.service;


import com.eventpro.api.dto.refund.RefundRequest;
import com.eventpro.api.dto.refund.RefundResponse;

import java.util.List;

public interface RefundService {

    RefundResponse requestRefund(
            Long clientId,
            Long paymentId,
            RefundRequest request
    );

    RefundResponse approveRefund(Long refundId);

    RefundResponse rejectRefund(Long refundId);

    List<RefundResponse> getPaymentRefunds(Long paymentId);
}