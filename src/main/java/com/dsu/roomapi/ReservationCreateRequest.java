package com.dsu.roomapi;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

// Lab E task 1: one-field rules at the door. "start before end" needs two fields,
// so it stays in ReservationService.
// Week 11: no reservedBy. The owner is the logged-in user: nothing to type, nothing to fake.
// Also the body of PUT /api/reservations/{id} (Lab G).
public record ReservationCreateRequest(
        @NotNull(message = "startTime is required")
        LocalDateTime startTime,

        @NotNull(message = "endTime is required")
        LocalDateTime endTime) {
}
