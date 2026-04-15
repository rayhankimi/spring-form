package com.rayhank.tech_assesment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GetResponsesResponse {
    private String message;
    private List<ResponseDto> responses;
}
