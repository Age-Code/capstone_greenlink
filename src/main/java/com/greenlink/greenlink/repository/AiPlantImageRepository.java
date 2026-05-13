package com.greenlink.greenlink.repository;

import com.greenlink.greenlink.domain.ai.AiPlantImage;
import com.greenlink.greenlink.domain.iot.PlantImage;
import com.greenlink.greenlink.domain.plant.UserPlant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiPlantImageRepository extends JpaRepository<AiPlantImage, Long> {

    Optional<AiPlantImage> findTopByPlantImageAndDeletedFalseOrderByIdDesc(
            PlantImage plantImage
    );

    Optional<AiPlantImage> findTopByUserPlantAndDeletedFalseOrderByIdDesc(
            UserPlant userPlant
    );
}