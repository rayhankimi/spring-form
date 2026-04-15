package com.rayhank.tech_assesment.dto.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionDto {

    private Long id;

    @JsonProperty("form_id")
    private Long formId;

    private String name;

    @JsonProperty("choice_type")
    private String choiceType;

    private String choices;

    @JsonProperty("is_required")
    private boolean isRequired;
}
