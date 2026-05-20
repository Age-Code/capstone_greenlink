package com.greenlink.greenlink.common;

import com.greenlink.greenlink.dto.iot.IotAppDto;
import lombok.Getter;

@Getter
public class WateringBlockedException extends RuntimeException {

    private final IotAppDto.WaterBlockedResDto response;

    public WateringBlockedException(
            Long userPlantId,
            Double soilMoisturePercent,
            Double tooWetThresholdPercent
    ) {
        super("토양 수분이 "
                + String.format("%.1f", soilMoisturePercent)
                + "%로 충분히 높아 물주기를 실행할 수 없습니다.");

        this.response = IotAppDto.WaterBlockedResDto.builder()
                .userPlantId(userPlantId)
                .soilMoisturePercent(soilMoisturePercent)
                .tooWetThresholdPercent(tooWetThresholdPercent)
                .canWater(false)
                .build();
    }
}
