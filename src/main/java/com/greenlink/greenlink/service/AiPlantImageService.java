package com.greenlink.greenlink.service;

import com.greenlink.greenlink.domain.plant.UserPlant;
import com.greenlink.greenlink.domain.ai.AiPlantImage;
import com.greenlink.greenlink.domain.iot.PlantImage;
import com.greenlink.greenlink.dto.ai.AiPlantImageDto;
import com.greenlink.greenlink.repository.AiPlantImageRepository;
import com.greenlink.greenlink.repository.PlantImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiPlantImageService {

    private final PlantImageRepository plantImageRepository;
    private final AiPlantImageRepository aiPlantImageRepository;

    @Transactional
    public AiPlantImageDto.AiImageResDto saveAiImageResult(
            Long plantImageId,
            AiPlantImageDto.SaveAiImageReqDto request
    ) {
        if (request.getFinalAiUrl() == null || request.getFinalAiUrl().isBlank()) {
            throw new IllegalArgumentException("finalAiUrl은 필수입니다.");
        }

        PlantImage plantImage = plantImageRepository.findById(plantImageId)
                .orElseThrow(() -> new IllegalArgumentException("원본 식물 이미지를 찾을 수 없습니다."));

        UserPlant userPlant = plantImage.getUserPlant();

        if (userPlant == null) {
            throw new IllegalStateException("원본 이미지에 연결된 userPlant가 없습니다.");
        }

        AiPlantImage aiPlantImage = AiPlantImage.success(
                plantImage,
                userPlant,
                request.getFinalAiUrl()
        );

        AiPlantImage saved = aiPlantImageRepository.save(aiPlantImage);

        return AiPlantImageDto.AiImageResDto.from(saved);
    }
}