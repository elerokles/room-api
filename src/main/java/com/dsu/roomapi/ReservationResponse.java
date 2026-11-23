package com.dsu.roomapi;

import java.time.LocalDateTime;

// Week 7: never send the entity (its room is a LAZY proxy). The contract is ours.
// Week 11: owner is the username, never the AppUser entity (it holds the password hash).
public record ReservationResponse(Long id, Long roomId, String roomName,
                                  String owner, LocalDateTime startTime,
                                  LocalDateTime endTime) {

    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(r.getId(), r.getRoom().getId(), r.getRoom().getName(),
                r.getOwner().getUsername(), r.getStartTime(), r.getEndTime());
    }
}
