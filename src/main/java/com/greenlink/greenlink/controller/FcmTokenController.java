package com.greenlink.greenlink.controller;

import com.greenlink.greenlink.security.CustomUserDetails;
import com.greenlink.greenlink.dto.FcmTokenDto;
import com.greenlink.greenlink.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/fcm-token")
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PostMapping
    public ResponseEntity<?> saveFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody FcmTokenDto.SaveReqDto reqDto
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(response(false, "로그인이 필요합니다.", null));
        }

        FcmTokenDto.SaveResDto data = fcmTokenService.saveToken(userDetails.getUserId(), reqDto);

        return ResponseEntity.ok(response(true, "FCM 토큰이 저장되었습니다.", data));
    }

    private Map<String, Object> response(Boolean success, String message, Object data) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", success);
        body.put("message", message);
        body.put("data", data);
        return body;
    }
}