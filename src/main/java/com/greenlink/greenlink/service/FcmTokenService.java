package com.greenlink.greenlink.service;

import com.greenlink.greenlink.domain.notification.UserFcmToken;
import com.greenlink.greenlink.domain.user.User;
import com.greenlink.greenlink.dto.FcmTokenDto;
import com.greenlink.greenlink.repository.UserFcmTokenRepository;
import com.greenlink.greenlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final UserRepository userRepository;
    private final UserFcmTokenRepository userFcmTokenRepository;

    @Transactional
    public FcmTokenDto.SaveResDto saveToken(Long userId, FcmTokenDto.SaveReqDto reqDto) {
        if (reqDto == null || reqDto.getFcmToken() == null || reqDto.getFcmToken().isBlank()) {
            throw new IllegalArgumentException("FCM 토큰은 필수입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String token = reqDto.getFcmToken().trim();
        String platform = normalizePlatform(reqDto.getPlatform());

        UserFcmToken userFcmToken = userFcmTokenRepository
                .findByUserAndFcmToken(user, token)
                .map(existing -> {
                    existing.reactivate(platform);
                    return existing;
                })
                .orElseGet(() -> UserFcmToken.builder()
                        .user(user)
                        .fcmToken(token)
                        .platform(platform)
                        .deleted(false)
                        .build());

        UserFcmToken saved = userFcmTokenRepository.save(userFcmToken);

        return FcmTokenDto.SaveResDto.builder()
                .id(saved.getId())
                .userId(user.getId())
                .platform(saved.getPlatform())
                .saved(true)
                .build();
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = platform.trim().toUpperCase();

        if (normalized.contains("ANDROID")) {
            return "ANDROID";
        }

        if (normalized.contains("IOS")) {
            return "IOS";
        }

        return normalized;
    }
}