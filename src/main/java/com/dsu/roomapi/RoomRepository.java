package com.dsu.roomapi;

import java.util.List;
import java.util.Optional;

public interface RoomRepository {

    List<Room> findAll();

    Optional<Room> findById(Long id);

    /** id == null: store a new room with a fresh id. Otherwise: replace the room with that id. */
    Room save(Room room);

    /** Returns true if a room was removed. */
    boolean deleteById(Long id);
}
