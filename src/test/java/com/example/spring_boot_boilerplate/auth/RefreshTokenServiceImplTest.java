package com.example.spring_boot_boilerplate.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_boilerplate.auth.entity.RefreshToken;
import com.example.spring_boot_boilerplate.auth.repository.RefreshTokenRepository;
import com.example.spring_boot_boilerplate.auth.service.impl.RefreshTokenServiceImpl;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    RefreshTokenServiceImpl refreshTokenServiceImpl;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Captor
    ArgumentCaptor<RefreshToken> tokenCaptor;

    private static final long EXP_MS = 3600_000L; // 1h
    private static final int MAX_TOKENS = 3;

    @BeforeEach
    void setUp() {
        refreshTokenServiceImpl = new RefreshTokenServiceImpl(refreshTokenRepository, EXP_MS, MAX_TOKENS);
    }

    @Test
    void create_savesToken_and_returnsToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        String token = refreshTokenServiceImpl.create("alice");
        assertNotNull(token);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken saved = tokenCaptor.getValue();
        assertEquals(token, saved.getToken());
        assertEquals("alice", saved.getUsername());
        Instant now = Instant.now();
        assertTrue(saved.getExpiresAt().isAfter(now));
        assertTrue(saved.getExpiresAt().isBefore(now.plus(Duration.ofSeconds(3605)))); // tolerance
    }

    @Test
    void validate_returnsUsername_whenValid() {
        RefreshToken rt = new RefreshToken("t1", "bob", Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByToken("t1")).thenReturn(Optional.of(rt));
        String u = refreshTokenServiceImpl.validate("t1");
        assertEquals("bob", u);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void validate_returnsNull_and_deletes_whenExpired() {
        RefreshToken rt = new RefreshToken("t2", "carol", Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByToken("t2")).thenReturn(Optional.of(rt));
        String u = refreshTokenServiceImpl.validate("t2");
        assertNull(u);
        verify(refreshTokenRepository).delete(rt);
    }

    @Test
    void validate_returnsNull_whenNotFound() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());
        assertNull(refreshTokenServiceImpl.validate("missing"));
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void rotate_returnsNull_whenOldInvalid() {
        // validate -> repository returns empty -> rotate returns null
        when(refreshTokenRepository.findByToken("old")).thenReturn(Optional.empty());
        String r = refreshTokenServiceImpl.rotate("old");
        assertNull(r);
    }

    @Test
    void rotate_deletesOld_and_createsNew_whenValid() {
        RefreshToken old = new RefreshToken("oldToken", "dan", Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByToken("oldToken")).thenReturn(Optional.of(old));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String newToken = refreshTokenServiceImpl.rotate("oldToken");
        assertNotNull(newToken);
        assertNotEquals("oldToken", newToken);
        verify(refreshTokenRepository).delete(old);
        verify(refreshTokenRepository, atLeastOnce()).save(any(RefreshToken.class));
    }

    @Test
    void revoke_deletesExisting() {
        RefreshToken rt = new RefreshToken("r1", "eve", Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByToken("r1")).thenReturn(Optional.of(rt));
        refreshTokenServiceImpl.revoke("r1");
        verify(refreshTokenRepository).delete(rt);
    }

    @Test
    void revoke_doesNothing_whenNotFound() {
        when(refreshTokenRepository.findByToken("x")).thenReturn(Optional.empty());
        refreshTokenServiceImpl.revoke("x");
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void revokeAll_callsRepositoryDeleteByUsername() {
        refreshTokenServiceImpl.revokeAll("frank");
        verify(refreshTokenRepository).deleteByUsername("frank");
    }

    @Test
    void enforceTokenLimit_revokesOldest_whenAtLimit() {
        // create 3 tokens (limit 3) where one is oldest
        List<RefreshToken> tokens = new ArrayList<>();
        tokens.add(new RefreshToken("t1", "greg", Instant.now().minusSeconds(300))); // oldest
        tokens.add(new RefreshToken("t2", "greg", Instant.now().minusSeconds(200)));
        tokens.add(new RefreshToken("t3", "greg", Instant.now().minusSeconds(100)));
        when(refreshTokenRepository.findByUsername("greg")).thenReturn(tokens);
        boolean acted = refreshTokenServiceImpl.enforceTokenLimit("greg");
        assertTrue(acted);
        verify(refreshTokenRepository).delete(argThat(rt -> "t1".equals(rt.getToken())));
    }

    @Test
    void enforceTokenLimit_noAction_whenUnderLimit() {
        List<RefreshToken> tokens = new ArrayList<>();
        tokens.add(new RefreshToken("t1", "hank", Instant.now().minusSeconds(100)));
        when(refreshTokenRepository.findByUsername("hank")).thenReturn(tokens);
        boolean acted = refreshTokenServiceImpl.enforceTokenLimit("hank");
        assertFalse(acted);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void getMaxConcurrentTokens_returnsConfiguredValue() {
        assertEquals(MAX_TOKENS, refreshTokenServiceImpl.getMaxConcurrentTokens());
    }
}
