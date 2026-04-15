package com.rayhank.tech_assesment.dto.form;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddQuestionResponse {
    private String message;
    private QuestionDto question;
}
