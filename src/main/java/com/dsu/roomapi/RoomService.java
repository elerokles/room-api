package com.dsu.roomapi;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public RoomService(RoomRepository roomRepository,
                       ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<Room> search(Integer minCapacity, String keyword) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (minCapacity == null && !hasKeyword) {
            return roomRepository.findAll();
        }
        if (minCapacity == null) {
            return roomRepository.findByNameContainingIgnoreCase(keyword);
        }
        if (!hasKeyword) {
            return roomRepository.findByCapacityGreaterThanEqual(minCapacity);
        }
        return roomRepository
                .findByCapacityGreaterThanEqualAndNameContainingIgnoreCase(minCapacity, keyword);
    }

    // Week 9: the service names the problem (NotFoundException), the handler turns it into 404.
    public Room get(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("room " + id + " not found"));
    }

    // The capacity rule (1 to 20) is checked by @Valid on RoomCreateRequest before we get here,
    // and the CHECK constraint in the table is the last guard.
    @Transactional
    public Room create(String name, int capacity) {
        return roomRepository.save(new Room(name, capacity));
    }

    // Week 9: dirty checking. Load (managed), change in memory, UPDATE at commit. No save().
    @Transactional
    public Room update(Long id, String name, int capacity) {
        Room room = get(id);
        room.update(name, capacity);
        return room;
    }

    // Week 7, option B: ask before you act. The foreign key still guards the table.
    @Transactional
    public void delete(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new NotFoundException("room " + id + " not found");
        }
        if (reservationRepository.existsByRoomId(id)) {
            throw new ConflictException("room " + id + " has reservations");
        }
        roomRepository.deleteById(id);
    }

    public int count() {
        return (int) roomRepository.count();
    }
}
