package com.dsu.roomapi;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Week 9: one-field rules live on the request record. @Valid in the controller switches them on.
public record RoomCreateRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Min(value = 1, message = "capacity must be between 1 and 20")
        @Max(value = 20, message = "capacity must be between 1 and 20")
        int capacity) {
}
