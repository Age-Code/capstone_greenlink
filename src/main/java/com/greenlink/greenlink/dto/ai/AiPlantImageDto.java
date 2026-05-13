package com.greenlink.greenlink.dto.ai;

import com.greenlink.greenlink.domain.ai.AiImageStatus;
import com.greenlink.greenlink.domain.ai.AiPlantImage;
import lombok.*;

import java.time.LocalDateTime;

public class AiPlantImageDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveAiImageReqDto {
        private String finalAiUrl;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiImageResDto {
        private Long aiPlantImageId;
        private Long plantImageId;
        private Long userPlantId;
        private String originalImageUrl;
        private String aiImageUrl;
        private AiImageStatus status;
        private LocalDateTime createdAt;

        public static AiImageResDto from(AiPlantImage aiPlantImage) {
            return AiImageResDto.builder()
                    .aiPlantImageId(aiPlantImage.getId())
                    .plantImageId(aiPlantImage.getPlantImage().getId())
                    .userPlantId(aiPlantImage.getUserPlant().getId())
                    .originalImageUrl(aiPlantImage.getOriginalImageUrl())
                    .aiImageUrl(aiPlantImage.getAiImageUrl())
                    .status(aiPlantImage.getStatus())
                    .createdAt(aiPlantImage.getCreatedAt())
                    .build();
        }
    }
}