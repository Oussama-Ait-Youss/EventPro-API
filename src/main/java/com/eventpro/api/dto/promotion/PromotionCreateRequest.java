package com.eventpro.api.dto.promotion;

import com.eventpro.api.model.enums.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Setter
public class PromotionCreateRequest {



    private Long id;
    private Long organizer_id;
    private Long event_id;


    @NotBlank(message = "code is required")
    private String code;


    private DiscountType discountType;
    private BigDecimal discountValue;

    @NotNull(message = "start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;


    private int usageLimit;

}
