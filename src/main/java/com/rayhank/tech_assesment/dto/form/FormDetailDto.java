package com.rayhank.tech_assesment.dto.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FormDetailDto {

    private Long id;
    private String name;
    private String slug;
    private String description;

    @JsonProperty("limit_one_response")
    private boolean limitOneResponse;

    @JsonProperty("creator_id")
    private Long creatorId;

    @JsonProperty("allowed_domains")
    private List<String> allowedDomains;

    private List<QuestionDto> questions;
}
