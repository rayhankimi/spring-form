package com.rayhank.tech_assesment.controller;

import com.rayhank.tech_assesment.dto.form.CreateFormRequest;
import com.rayhank.tech_assesment.dto.form.CreateFormResponse;
import com.rayhank.tech_assesment.service.FormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
@Tag(name = "Forms")
public class FormController {

    private final FormService formService;

    @PostMapping
    @Operation(summary = "Create a new form")
    public ResponseEntity<CreateFormResponse> createForm(@Valid @RequestBody CreateFormRequest request) {
        return ResponseEntity.ok(formService.createForm(request));
    }
}
