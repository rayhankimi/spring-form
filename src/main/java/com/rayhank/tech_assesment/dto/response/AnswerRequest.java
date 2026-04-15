package com.rayhank.tech_assesment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AnswerRequest {

    @JsonProperty("question_id")
    private Long questionId;

    private String value;
}
