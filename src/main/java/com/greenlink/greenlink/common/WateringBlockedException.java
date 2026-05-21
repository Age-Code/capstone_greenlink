package com.greenlink.greenlink.common;

import com.greenlink.greenlink.dto.iot.IotAppDto;
import lombok.Getter;

@Getter
public class WateringBlockedException extends RuntimeException {

    private final IotAppDto.WaterBlockedResDto response;

    private WateringBlockedException(
            String message,
            IotAppDto.WaterBlockedResDto response
    ) {
        super(message);
        this.response = response;
    }

    public static WateringBlockedException tooWet(
            Long userPlantId,
            Double soilMoisturePercent,
            Double tooWetThresholdPercent
    ) {
        String message = "토양 수분이 "
                + String.format("%.1f", soilMoisturePercent)
                + "%로 충분히 높아 물주기를 실행할 수 없습니다.";

        return new WateringBlockedException(message, IotAppDto.WaterBlockedResDto.builder()
                .userPlantId(userPlantId)
                .reason("TOO_WET")
                .soilMoisturePercent(soilMoisturePercent)
                .tooWetThresholdPercent(tooWetThresholdPercent)
                .canWater(false)
                .build());
    }

    public static WateringBlockedException pendingWaterCommand(Long userPlantId) {
        return new WateringBlockedException(
                "이미 처리 대기 중인 물주기 명령이 있습니다. 잠시 후 다시 시도해주세요.",
                IotAppDto.WaterBlockedResDto.builder()
                        .userPlantId(userPlantId)
                        .reason("PENDING_WATER_COMMAND")
                        .canWater(false)
                        .build()
        );
    }

    public static WateringBlockedException cooldown(
            Long userPlantId,
            Integer cooldownSeconds
    ) {
        return new WateringBlockedException(
                "방금 물주기 명령을 보냈습니다. 잠시 후 다시 시도해주세요.",
                IotAppDto.WaterBlockedResDto.builder()
                        .userPlantId(userPlantId)
                        .reason("WATER_COOLDOWN")
                        .cooldownSeconds(cooldownSeconds)
                        .canWater(false)
                        .build()
        );
    }

    public static WateringBlockedException requestLimit(
            Long userPlantId,
            Integer windowSeconds,
            Integer maxRequests
    ) {
        return new WateringBlockedException(
                "짧은 시간 안에 물주기를 너무 많이 요청했습니다. 잠시 후 다시 시도해주세요.",
                IotAppDto.WaterBlockedResDto.builder()
                        .userPlantId(userPlantId)
                        .reason("WATER_REQUEST_LIMIT")
                        .windowSeconds(windowSeconds)
                        .maxRequests(maxRequests)
                        .canWater(false)
                        .build()
        );
    }
}
