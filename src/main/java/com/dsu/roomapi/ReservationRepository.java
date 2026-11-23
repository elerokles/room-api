package com.dsu.roomapi;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // RoomId is a path: Spring reads it as room.id.
    List<Reservation> findByRoomIdOrderByStartTime(Long roomId);

    boolean existsByRoomId(Long roomId);

    // Week 11: GET /api/reservations/mine. One query: join fetch the room and the owner.
    @Query("""
            select r from Reservation r
            join fetch r.room
            join fetch r.owner o
            where o.username = :username
            order by r.startTime
            """)
    List<Reservation> findMine(@Param("username") String username);

    // Lab D: two bookings overlap when new.start < old.end AND new.end > old.start.
    // Strict < and > so 11:00-12:00 does not clash with 10:00-11:00.
    @Query("""
            select count(r) from Reservation r
            where r.room.id = :roomId
              and r.startTime < :endTime
              and r.endTime > :startTime
            """)
    long countOverlapping(@Param("roomId") Long roomId,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    // Lab G task 1: the same, but a reservation never overlaps itself.
    @Query("""
            select count(r) from Reservation r
            where r.room.id = :roomId
              and r.id <> :id
              and r.startTime < :endTime
              and r.endTime > :startTime
            """)
    long countOverlappingExcept(@Param("roomId") Long roomId,
                                @Param("id") Long id,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
}
