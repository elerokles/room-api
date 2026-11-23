package com.dsu.roomapi;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Week 11, rule C: ownership is checked here, not in SecurityConfig. A URL rule sees
// /api/reservations/9 but cannot see who owns reservation 9. The service can.
// The service does not read Spring Security: the controller passes the username and
// whether the caller is an admin.
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final AppUserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              AppUserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public List<Reservation> findByRoom(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new NotFoundException("room " + roomId + " not found");
        }
        return reservationRepository.findByRoomIdOrderByStartTime(roomId);
    }

    // GET /api/reservations/mine: one select with join fetch (no N+1).
    public List<Reservation> findMine(String username) {
        return reservationRepository.findMine(username);
    }

    // Read one: the owner or an admin. 404 first, then 403.
    public Reservation get(Long id, String username, boolean admin) {
        Reservation r = find(id);
        requireOwnerOrAdmin(r, username, admin);
        return r;
    }

    // Read, check, then save, as one unit of work. Cheap checks first: 400, then 404, then 409.
    // The owner is the logged-in user, never a value from the request.
    @Transactional
    public Reservation create(Long roomId, String username,
                              LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("room " + roomId + " not found"));
        if (reservationRepository.countOverlapping(roomId, startTime, endTime) > 0) {
            throw new ConflictException("room " + roomId + " is already booked at that time");
        }
        AppUser owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("user " + username + " not found"));
        return reservationRepository.save(new Reservation(room, owner, startTime, endTime));
    }

    // Lab G: move a booking. 400 -> 404 -> 403 (owner only, not even an admin) -> 409.
    // No save(): the reservation is managed, dirty checking writes the UPDATE at commit.
    @Transactional
    public Reservation reschedule(Long id, String username,
                                  LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        Reservation r = find(id);
        requireOwner(r, username);
        Long roomId = r.getRoom().getId();
        if (reservationRepository.countOverlappingExcept(roomId, id, startTime, endTime) > 0) {
            throw new ConflictException("room " + roomId + " is already booked at that time");
        }
        r.reschedule(startTime, endTime);
        return r;
    }

    // Cancel: the owner or an admin. 404 first, then 403.
    @Transactional
    public void delete(Long id, String username, boolean admin) {
        Reservation r = find(id);
        requireOwnerOrAdmin(r, username, admin);
        reservationRepository.delete(r);
    }

    private Reservation find(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("reservation " + id + " not found"));
    }

    private static void requireOwner(Reservation r, String username) {
        if (!r.isOwnedBy(username)) {
            throw new ForbiddenException(
                    "reservation " + r.getId() + " belongs to another user");
        }
    }

    private static void requireOwnerOrAdmin(Reservation r, String username, boolean admin) {
        if (!admin) {
            requireOwner(r, username);
        }
    }
}
