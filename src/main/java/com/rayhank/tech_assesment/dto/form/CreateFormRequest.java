package com.rayhank.tech_assesment.dto.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rayhank.tech_assesment.validation.UniqueSlug;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.util.List;

@Getter
public class CreateFormRequest {

    @NotBlank(message = "The name field is required.")
    private String name;

    @NotBlank(message = "The slug field is required.")
    @Pattern(
        regexp = "^[a-zA-Z0-9.-]+$",
        message = "The slug may only contain letters, numbers, dashes, and dots."
    )
    @UniqueSlug
    private String slug;

    private String description;

    @JsonProperty("allowed_domains")
    private List<String> allowedDomains;

    @JsonProperty("limit_one_response")
    private boolean limitOneResponse;
}
