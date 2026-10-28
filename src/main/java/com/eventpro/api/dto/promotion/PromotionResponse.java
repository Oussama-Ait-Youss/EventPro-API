package com.eventpro.api.dto.promotion;

import com.eventpro.api.model.enums.DiscountType;
import com.eventpro.api.model.enums.PromotionStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PromotionResponse {

        private Long id;

        private Long organizerId;

        private Long eventId;

        private String code;

        private DiscountType discountType;

        private BigDecimal discountValue;

        private LocalDateTime startDate;

        private LocalDateTime endDate;


        private PromotionStatus status;

        private Integer usageLimit;

        private Integer usageCount;
}