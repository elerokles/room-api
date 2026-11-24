package com.dsu.roomapi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// Week 12, Part 1: ReservationService alone. The repositories are Mockito fakes:
// no Spring, no server, no database. A service throws; the handler turns it into a status,
// so these tests check the exception, not the status code.
class ReservationServiceTest {

    private RoomRepository roomRepository;
    private ReservationRepository reservationRepository;
    private AppUserRepository userRepository;
    private ReservationService service;

    private final Room room = roomWithId(1L, "Seminar A", 8);
    private final AppUser mina = new AppUser("mina", "hash", "USER");
    private final LocalDateTime ten = LocalDateTime.of(2026, 12, 1, 10, 0);
    private final LocalDateTime eleven = LocalDateTime.of(2026, 12, 1, 11, 0);
    private final LocalDateTime twelve = LocalDateTime.of(2026, 12, 1, 12, 0);

    // Our Room has no constructor that takes an id (Hibernate sets it), so the test sets it.
    private static Room roomWithId(Long id, String name, int capacity) {
        Room r = new Room(name, capacity);
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }

    @BeforeEach                                   // runs before EVERY test: fresh fakes each time
    void setUp() {
        roomRepository = mock(RoomRepository.class);
        reservationRepository = mock(ReservationRepository.class);
        userRepository = mock(AppUserRepository.class);
        // Same order as the constructor in ReservationService.
        service = new ReservationService(reservationRepository, roomRepository, userRepository);
    }

    // ---------- Step 3 ----------

    @Test
    void createRefusesEndBeforeStart() {                       // 400
        assertThrows(IllegalArgumentException.class,
                () -> service.create(1L, "mina", eleven, ten));
        verify(reservationRepository, never()).save(any());     // and nothing was saved
    }

    @Test
    void createRefusesMissingRoom() {                          // 404
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.create(99L, "mina", ten, eleven));
    }

    // ---------- Step 4 ----------

    @Test
    void createRefusesOverlap() {                              // 409
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(reservationRepository.countOverlapping(eq(1L), any(), any())).thenReturn(1L);

        assertThrows(ConflictException.class, () -> service.create(1L, "mina", ten, eleven));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void deleteRefusesAnotherUser() {                          // 403, rule C
        Reservation minas = new Reservation(room, mina, ten, eleven);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(minas));

        assertThrows(ForbiddenException.class, () -> service.delete(1L, "omar", false));
        verify(reservationRepository, never()).delete(any());
    }

    // ---------- Lab H1 ----------

    @Test
    void createSavesWithTheLoggedInOwner() {                   // Task 1
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(reservationRepository.countOverlapping(eq(1L), any(), any())).thenReturn(0L);
        when(userRepository.findByUsername("mina")).thenReturn(Optional.of(mina));
        when(reservationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Reservation saved = service.create(1L, "mina", ten, eleven);

        assertEquals("mina", saved.getOwner().getUsername());
        assertSame(room, saved.getRoom());
        assertEquals(ten, saved.getStartTime());
        assertEquals(eleven, saved.getEndTime());
        verify(reservationRepository).save(any());
    }

    @Test
    void ownerMayDelete() {                                    // Task 2
        Reservation minas = new Reservation(room, mina, ten, eleven);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(minas));

        assertDoesNotThrow(() -> service.delete(1L, "mina", false));
        verify(reservationRepository).delete(minas);
    }

    @Test
    void adminMayDeleteAnotherUsersReservation() {             // Task 3
        Reservation minas = new Reservation(room, mina, ten, eleven);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(minas));

        assertDoesNotThrow(() -> service.delete(1L, "admin", true));
        verify(reservationRepository).delete(minas);
    }

    @Test
    void adminMayNotReschedule() {                             // Task 4, Lab G: owner only
        Reservation minas = new Reservation(room, mina, ten, eleven);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(minas));

        assertThrows(ForbiddenException.class,
                () -> service.reschedule(1L, "admin", eleven, twelve));
        // still at the old time: nothing was moved
        assertEquals(ten, minas.getStartTime());
        assertEquals(eleven, minas.getEndTime());
    }

    // ---------- Done early ----------

    @Test
    void rescheduleOntoBusyTimeIs409() {
        Reservation minas = new Reservation(room, mina, ten, eleven);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(minas));
        when(reservationRepository.countOverlappingExcept(eq(1L), eq(1L), any(), any()))
                .thenReturn(1L);

        assertThrows(ConflictException.class,
                () -> service.reschedule(1L, "mina", eleven, twelve));
        assertEquals(ten, minas.getStartTime());
    }
}
