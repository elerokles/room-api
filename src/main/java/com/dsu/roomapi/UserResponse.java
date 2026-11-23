package com.dsu.roomapi;

// No hash, ever, in a response.
public record UserResponse(Long id, String username, String role) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
