package com.nguyenquyen.authservice.repository;


import com.nguyenquyen.authservice.entity.RefreshToken;
import com.nguyenquyen.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    // "Logout all devices" — revoke all tokens for a user
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    void revokeAllUserTokens(User user);

    // Cleanup expired tokens (scheduled job ke liye — future scope)
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < CURRENT_TIMESTAMP OR rt.revoked = true")
    void deleteExpiredAndRevokedTokens();
}
