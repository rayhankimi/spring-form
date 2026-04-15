package com.rayhank.tech_assesment.dto.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FormDto {

    private Long id;
    private String name;
    private String slug;
    private String description;

    @JsonProperty("limit_one_response")
    private boolean limitOneResponse;

    @JsonProperty("creator_id")
    private Long creatorId;
}
