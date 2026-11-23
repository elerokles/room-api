package com.dsu.roomapi;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

// Week 7: the reservation table as an entity. room_id becomes a reference to a Room.
// Week 11: owner_id (an account) replaces the typed reserved_by name.
@Entity
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many reservations, one room. LAZY: load the room only when someone asks for it.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    // Many reservations, one account. The owner comes from the login, never from the request.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    protected Reservation() { } // for Hibernate

    public Reservation(Room room, AppUser owner,
                       LocalDateTime startTime, LocalDateTime endTime) {
        this.room = room;
        this.owner = owner;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean isOwnedBy(String username) {
        return owner.getUsername().equals(username);
    }

    // Lab G: one named change. Dirty checking writes the UPDATE at commit.
    public void reschedule(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public AppUser getOwner() { return owner; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
}
