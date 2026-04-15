package com.rayhank.tech_assesment.dto.form;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetFormDetailResponse {
    private String message;
    private FormDetailDto form;
}
