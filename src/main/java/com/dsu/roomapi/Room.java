package com.dsu.roomapi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int capacity;

    protected Room() { } // for Hibernate

    public Room(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    // Week 9: one named change. On a managed entity, dirty checking writes the UPDATE at commit.
    public void update(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
}
