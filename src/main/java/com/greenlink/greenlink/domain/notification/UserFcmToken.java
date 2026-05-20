package com.greenlink.greenlink.domain.notification;

import com.greenlink.greenlink.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "user_fcm_token",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_fcm_token_user_token",
                        columnNames = {"user_id", "fcm_token"}
                )
        }
)
public class UserFcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FCM 토큰 소유 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Firebase Cloud Messaging registration token
     */
    @Column(name = "fcm_token", nullable = false, length = 500)
    private String fcmToken;

    /**
     * ANDROID / IOS
     */
    @Column(name = "platform", length = 20)
    private String platform;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (modifiedAt == null) {
            modifiedAt = now;
        }

        if (deleted == null) {
            deleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }

    public void reactivate(String platform) {
        this.platform = platform;
        this.deleted = false;
        this.modifiedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deleted = true;
        this.modifiedAt = LocalDateTime.now();
    }
}