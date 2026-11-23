package com.dsu.roomapi;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long ttlMinutes;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       @Value("${app.jwt.ttl-minutes}") long ttlMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.ttlMinutes = ttlMinutes;
    }

    // Sign-up stores a BCrypt hash. Everyone who signs up is USER.
    @Transactional
    public AppUser signup(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("username " + username + " is taken");
        }
        return userRepository.save(
                new AppUser(username, passwordEncoder.encode(password), "USER"));
    }

    // Check the password once, then sign a token. Same message for an unknown user and a
    // wrong password, so nobody learns which usernames exist.
    public TokenResponse login(String username, String password) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("wrong username or password"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("wrong username or password");
        }
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("room-api")
                .issuedAt(now)
                .expiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES))
                .subject(user.getUsername())
                .claim("role", user.getRole())
                .build();
        // Without the HS256 header the encoder looks for an RSA key and fails.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
        return new TokenResponse(token, "Bearer", ttlMinutes * 60);
    }
}
