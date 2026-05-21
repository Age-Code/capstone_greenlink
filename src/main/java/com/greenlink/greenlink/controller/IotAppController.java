package com.greenlink.greenlink.controller;

import com.greenlink.greenlink.common.ApiResponse;
import com.greenlink.greenlink.dto.iot.IotAppDto;
import com.greenlink.greenlink.security.CustomUserDetails;
import com.greenlink.greenlink.service.IotAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-plants/{userPlantId}/iot")
public class IotAppController {

    private final IotAppService iotAppService;

    /**
     * 내 식물 IoT 최신 상태 조회
     *
     * GET /api/user-plants/{userPlantId}/iot/latest
     *
     * 조회 내용:
     * - 재배 공간 정보
     * - 라즈베리파이 최신 환경 데이터
     * - ESP 최신 토양수분 데이터
     * - 최신 식물 이미지
     */
    @GetMapping("/latest")
    public ApiResponse<IotAppDto.IotLatestResDto> getLatestIotStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userPlantId
    ) {
        IotAppDto.IotLatestResDto response =
                iotAppService.getLatestIotStatus(
                        userDetails.getUserId(),
                        userPlantId
                );

        return ApiResponse.success("최신 IoT 상태 조회 성공", response);
    }

    /**
     * 내 식물 사진 기록 조회
     *
     * GET /api/user-plants/{userPlantId}/iot/images
     */
    @GetMapping("/images")
    public ApiResponse<List<IotAppDto.PlantImageDto>> getPlantImages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userPlantId
    ) {
        List<IotAppDto.PlantImageDto> response =
                iotAppService.getPlantImages(
                        userDetails.getUserId(),
                        userPlantId
                );

        return ApiResponse.success("식물 이미지 목록 조회 성공", response);
    }

    /**
     * 물 주기 요청
     *
     * POST /api/user-plants/{userPlantId}/iot/water
     *
     * Request Body 없음.
     *
     * 서버에서 고정 급수 시간 5초로 DeviceCommand를 생성한다.
     */
    @PostMapping("/water")
    public ApiResponse<IotAppDto.WaterCommandResDto> requestWater(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userPlantId
    ) {
        IotAppDto.WaterCommandResDto response =
                iotAppService.requestWater(
                        userDetails.getUserId(),
                        userPlantId
                );

        return ApiResponse.success("급수 명령이 요청되었습니다.", response);
    }

    /**
     * 조명 켜기 요청
     *
     * POST /api/user-plants/{userPlantId}/iot/light/on
     */
    @PostMapping("/light/on")
    public ApiResponse<IotAppDto.LightCommandResDto> requestLightOn(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userPlantId
    ) {
        IotAppDto.LightCommandResDto response =
                iotAppService.requestLightOn(
                        userDetails.getUserId(),
                        userPlantId
                );

        return ApiResponse.success("조명 켜기 명령이 요청되었습니다.", response);
    }

    /**
     * 조명 끄기 요청
     *
     * POST /api/user-plants/{userPlantId}/iot/light/off
     */
    @PostMapping("/light/off")
    public ApiResponse<IotAppDto.LightCommandResDto> requestLightOff(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userPlantId
    ) {
        IotAppDto.LightCommandResDto response =
                iotAppService.requestLightOff(
                        userDetails.getUserId(),
                        userPlantId
                );

        return ApiResponse.success("조명 끄기 명령이 요청되었습니다.", response);
    }

    /**
     * 라즈베리파이 환경 센서 새로고침 요청
     *
     * POST /api/user-plants/{userPlantId}/iot/refresh
     *
     * SENSOR_REFRESH command를 생성한다.
     * 대상: 온도, 습도, 조도
     * 제외: ESP32 토양수분
     */
    @PostMapping("/refresh")
    public ApiResponse<IotAppDto.SensorRefreshResDto> requestSensorRefresh(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userPlantId
    ) {
        IotAppDto.SensorRefreshResDto response =
                iotAppService.requestSensorRefresh(
                        userDetails.getUserId(),
                        userPlantId
                );

        return ApiResponse.success(sensorRefreshMessage(response), response);
    }

    private String sensorRefreshMessage(IotAppDto.SensorRefreshResDto response) {
        if (!Boolean.TRUE.equals(response.getAlreadyPending())) {
            return "센서 새로고침 명령이 생성되었습니다.";
        }

        if ("SENSOR_REFRESH_COOLDOWN".equals(response.getDuplicateReason())) {
            return "방금 센서 새로고침을 요청했습니다. 잠시 후 최신 상태를 다시 확인해주세요.";
        }

        return "이미 센서 새로고침 명령이 대기 중입니다.";
    }
}
