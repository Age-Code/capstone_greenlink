package com.greenlink.greenlink.domain.user;

import com.greenlink.greenlink.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인 이메일
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 암호화된 비밀번호
    @Column(nullable = false)
    private String password;

    // 닉네임
    @Column(nullable = false, length = 50)
    private String nickname;

    // 사용자 권한
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginProvider provider;

    @Column
    private String providerId;

    @Column
    private String profileImageUrl;

    private User(
            String email,
            String password,
            String nickname,
            UserRole role,
            LoginProvider provider,
            String providerId,
            String profileImageUrl) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
        this.profileImageUrl = profileImageUrl;
    }

    public static User createUser(String email, String encodedPassword, String nickname) {
        return new User(
                email,
                encodedPassword,
                nickname,
                UserRole.USER,
                LoginProvider.LOCAL,
                null,
                null);
    }

    public static User createAdmin(String email, String encodedPassword, String nickname) {
        return new User(
                email,
                encodedPassword,
                nickname,
                UserRole.ADMIN,
                LoginProvider.LOCAL,
                null,
                null);
    }

    public static User createOAuthUser(
            String email,
            String encodedPassword,
            String nickname,
            LoginProvider provider,
            String providerId,
            String profileImageUrl) {
        return new User(
                email,
                encodedPassword,
                nickname,
                UserRole.USER,
                provider,
                providerId,
                profileImageUrl);
    }

    public void toggleRole() {
        this.role = (this.role == UserRole.USER) ? UserRole.ADMIN : UserRole.USER;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}