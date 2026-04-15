package com.rayhank.tech_assesment.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChoiceType {
    SHORT_ANSWER,
    PARAGRAPH,
    DATE,
    TIME,
    MULTIPLE_CHOICE,
    DROPDOWN,
    CHECKBOXES;

    // Serializes to "short answer", "multiple choice", etc. to match API spec
    @JsonValue
    public String toJsonValue() {
        return name().toLowerCase().replace('_', ' ');
    }
}
