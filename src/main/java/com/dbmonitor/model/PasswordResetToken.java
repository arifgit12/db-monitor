package com.dbmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Builder.Default
    private Boolean used = false;

    private LocalDateTime createdAt;

    private LocalDateTime usedAt;

    /**
     * Check if the token has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * Check if the token is valid (not used and not expired)
     */
    public boolean isValid() {
        return !used && !isExpired();
    }
}
