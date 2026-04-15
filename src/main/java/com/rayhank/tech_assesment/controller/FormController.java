package com.rayhank.tech_assesment.controller;

import com.rayhank.tech_assesment.dto.form.*;
import com.rayhank.tech_assesment.service.FormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @Operation(summary = "Get all forms created by the authenticated user")
    public ResponseEntity<GetAllFormsResponse> getAllForms() {
        return ResponseEntity.ok(formService.getAllForms());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get form detail by slug — access restricted by allowed email domain")
    public ResponseEntity<GetFormDetailResponse> getFormBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(formService.getFormBySlug(slug));
    }
}
