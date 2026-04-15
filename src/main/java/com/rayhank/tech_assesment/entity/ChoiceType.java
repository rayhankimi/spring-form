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

    // Deserializes from display name, e.g. "multiple choice" → MULTIPLE_CHOICE
    public static ChoiceType fromDisplayName(String displayName) {
        if (displayName == null) return null;
        for (ChoiceType type : values()) {
            if (type.toJsonValue().equalsIgnoreCase(displayName.trim())) {
                return type;
            }
        }
        return null;
    }
}
