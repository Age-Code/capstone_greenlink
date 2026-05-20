package com.greenlink.greenlink.dto;

import lombok.*;

public class FcmTokenDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveReqDto {
        private String fcmToken;
        private String platform;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveResDto {
        private Long id;
        private Long userId;
        private String platform;
        private Boolean saved;
    }
}