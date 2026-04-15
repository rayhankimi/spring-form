package com.rayhank.tech_assesment.dto.form;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GetAllFormsResponse {
    private String message;
    private List<FormDto> forms;
}
