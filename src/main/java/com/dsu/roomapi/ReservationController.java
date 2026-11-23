package com.dsu.roomapi;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

// Nested when the room matters (list, book), flat when it does not (read, move, cancel, mine).
// Week 11: the caller comes from Spring (authentication.getName()), never from the request body.
@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/rooms/{roomId}/reservations")
    public List<ReservationResponse> listForRoom(@PathVariable Long roomId) {
        return reservationService.findByRoom(roomId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    // Any logged-in user may book. The owner is the caller.
    @PostMapping("/rooms/{roomId}/reservations")
    public ResponseEntity<ReservationResponse> create(
            @PathVariable Long roomId,
            @Valid @RequestBody ReservationCreateRequest request,
            Authentication authentication) {
        Reservation r = reservationService.create(roomId, authentication.getName(),
                request.startTime(), request.endTime());
        return ResponseEntity
                .created(URI.create("/api/reservations/" + r.getId()))
                .body(ReservationResponse.from(r));
    }

    // Week 11: replaces ?reservedBy=. A fixed path wins over {id}, so "mine" is never an id.
    @GetMapping("/reservations/mine")
    public List<ReservationResponse> mine(Authentication authentication) {
        return reservationService.findMine(authentication.getName()).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    // The owner or an admin.
    @GetMapping("/reservations/{id}")
    public ReservationResponse findOne(@PathVariable Long id, Authentication authentication) {
        return ReservationResponse.from(
                reservationService.get(id, authentication.getName(), isAdmin(authentication)));
    }

    // Lab G: move a booking. The owner only.
    @PutMapping("/reservations/{id}")
    public ReservationResponse reschedule(@PathVariable Long id,
                                          @Valid @RequestBody ReservationCreateRequest request,
                                          Authentication authentication) {
        return ReservationResponse.from(reservationService.reschedule(id,
                authentication.getName(), request.startTime(), request.endTime()));
    }

    // The owner or an admin.
    @DeleteMapping("/reservations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        reservationService.delete(id, authentication.getName(), isAdmin(authentication));
    }

    // Works for Basic and tokens: both give ROLE_ authorities.
    private static boolean isAdmin(Authentication a) {
        return a.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN"));
    }
}
