package com.rayhank.tech_assesment.dto.form;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateFormResponse {
    private String message;
    private FormDto form;
}
