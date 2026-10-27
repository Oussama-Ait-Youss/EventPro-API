package com.eventpro.api.dto.promotion;

import com.eventpro.api.model.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PromotionUpdateRequest {

    @NotBlank(message = "Promotion code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Discount value must be greater than 0"
    )
    private BigDecimal discountValue;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @NotNull(message = "Usage limit is required")
    @Positive(message = "Usage limit must be greater than 0")
    private Integer usageLimit;
}