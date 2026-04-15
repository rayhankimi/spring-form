package com.rayhank.tech_assesment.dto.response;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class SubmitResponseRequest {

    @NotNull(message = "The answers field is required.")
    private List<AnswerRequest> answers;
}
