package com.rayhank.tech_assesment.dto.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rayhank.tech_assesment.validation.ChoicesRequiredForType;
import com.rayhank.tech_assesment.validation.ValidChoiceType;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
@ChoicesRequiredForType
public class AddQuestionRequest {

    @NotBlank(message = "The name field is required.")
    private String name;

    @NotBlank(message = "The choice type field is required.")
    @ValidChoiceType
    @JsonProperty("choice_type")
    private String choiceType;

    private List<String> choices;

    @JsonProperty("is_required")
    private boolean isRequired;
}
