package com.dsu.roomapi;

// Week 9: entities stay inside. Every room endpoint returns this record.
public record RoomResponse(Long id, String name, int capacity) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity());
    }
}
