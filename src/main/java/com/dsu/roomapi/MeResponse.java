package com.dsu.roomapi;

import java.util.List;

// GET /api/auth/me: who the server thinks is asking, and which authorities it gave them.
public record MeResponse(String username, List<String> authorities) { }
