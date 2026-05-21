package com.greenlink.greenlink.service;

import com.greenlink.greenlink.domain.ai.AiPlantImage;
import com.greenlink.greenlink.domain.automation.AutomationSetting;
import com.greenlink.greenlink.domain.iot.CommandStatus;
import com.greenlink.greenlink.domain.iot.CommandType;
import com.greenlink.greenlink.domain.iot.DeviceCommand;
import com.greenlink.greenlink.domain.iot.EspSensorData;
import com.greenlink.greenlink.domain.iot.GrowSpace;
import com.greenlink.greenlink.domain.iot.GrowSpacePlant;
import com.greenlink.greenlink.domain.iot.PlantImage;
import com.greenlink.greenlink.domain.iot.PumpChannel;
import com.greenlink.greenlink.domain.iot.RaspberrySensorData;
import com.greenlink.greenlink.domain.iot.DeviceType;
import com.greenlink.greenlink.domain.iot.IotDevice;
import com.greenlink.greenlink.domain.plant.UserPlant;
import com.greenlink.greenlink.domain.user.User;
import com.greenlink.greenlink.common.WateringBlockedException;
import com.greenlink.greenlink.common.constants.IotThresholds;
import com.greenlink.greenlink.dto.iot.IotAppDto;
import com.greenlink.greenlink.repository.AiPlantImageRepository;
import com.greenlink.greenlink.repository.AutomationSettingRepository;
import com.greenlink.greenlink.repository.DeviceCommandRepository;
import com.greenlink.greenlink.repository.EspSensorDataRepository;
import com.greenlink.greenlink.repository.GrowSpacePlantRepository;
import com.greenlink.greenlink.repository.IotDeviceRepository;
import com.greenlink.greenlink.repository.PlantImageRepository;
import com.greenlink.greenlink.repository.PumpChannelRepository;
import com.greenlink.greenlink.repository.RaspberrySensorDataRepository;
import com.greenlink.greenlink.repository.UserPlantRepository;
import com.greenlink.greenlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IotAppService {

    private final UserRepository userRepository;
    private final UserPlantRepository userPlantRepository;

    private final GrowSpacePlantRepository growSpacePlantRepository;
    private final RaspberrySensorDataRepository raspberrySensorDataRepository;
    private final EspSensorDataRepository espSensorDataRepository;
    private final PlantImageRepository plantImageRepository;
    private final AiPlantImageRepository aiPlantImageRepository;
    private final PumpChannelRepository pumpChannelRepository;
    private final DeviceCommandRepository deviceCommandRepository;
    private final IotDeviceRepository iotDeviceRepository;
    private final AutomationSettingRepository automationSettingRepository;

    /**
     * 내 식물 IoT 최신 상태 조회
     *
     * 조회 데이터:
     * 1. 식물이 속한 재배 공간
     * 2. 재배 공간의 최신 라즈베리파이 환경 데이터
     * 3. 해당 식물의 최신 ESP 토양수분 데이터
     * 4. 해당 식물의 최신 원본 이미지
     * 5. 해당 원본 이미지에 연결된 최신 AI 이미지
     */
    public IotAppDto.IotLatestResDto getLatestIotStatus(
            Long userId,
            Long userPlantId
    ) {
        User user = findActiveUser(userId);

        UserPlant userPlant = findMyUserPlant(userPlantId, user);

        GrowSpace growSpace = findGrowSpaceByUserPlant(userPlant);

        RaspberrySensorData latestEnvironment =
                raspberrySensorDataRepository
                        .findFirstByGrowSpaceAndDeletedFalseOrderByMeasuredAtDesc(growSpace)
                        .orElse(null);

        EspSensorData latestSoil =
                espSensorDataRepository
                        .findFirstByUserPlantAndDeletedFalseOrderByMeasuredAtDesc(userPlant)
                        .orElse(null);

        PlantImage latestImage =
                plantImageRepository
                        .findFirstByUserPlantAndDeletedFalseOrderByCapturedAtDesc(userPlant)
                        .orElseGet(() ->
                                plantImageRepository
                                        .findFirstByGrowSpaceAndDeletedFalseOrderByCapturedAtDesc(growSpace)
                                        .orElse(null)
                        );

        AiPlantImage latestAiImage = null;

        if (latestImage != null) {
            latestAiImage = aiPlantImageRepository
                    .findTopByPlantImageAndDeletedFalseOrderByIdDesc(latestImage)
                    .orElse(null);
        }

        return IotAppDto.IotLatestResDto.of(
                userPlant.getId(),
                growSpace,
                latestEnvironment,
                latestSoil,
                latestImage,
                latestAiImage
        );
    }

    /**
     * 내 식물 사진 기록 조회
     *
     * 기본 정책:
     * - 특정 userPlant 사진이 있으면 그것만 조회
     * - 각 원본 이미지에 연결된 AI 이미지가 있으면 aiImageUrl도 함께 내려준다.
     */
    public List<IotAppDto.PlantImageDto> getPlantImages(
            Long userId,
            Long userPlantId
    ) {
        User user = findActiveUser(userId);

        UserPlant userPlant = findMyUserPlant(userPlantId, user);

        return plantImageRepository
                .findAllByUserPlantAndDeletedFalseOrderByCapturedAtDesc(userPlant)
                .stream()
                .map(plantImage -> {
                    AiPlantImage aiPlantImage = aiPlantImageRepository
                            .findTopByPlantImageAndDeletedFalseOrderByIdDesc(plantImage)
                            .orElse(null);

                    return IotAppDto.PlantImageDto.from(
                            plantImage,
                            aiPlantImage
                    );
                })
                .toList();
    }

    /**
     * 물 주기 요청
     *
     * Request Body 없음.
     * 서버에서 DeviceCommand를 생성한다.
     */
    @Transactional
    public IotAppDto.WaterCommandResDto requestWater(
            Long userId,
            Long userPlantId
    ) {
        User user = findActiveUser(userId);

        UserPlant userPlant = findMyUserPlant(userPlantId, user);

        GrowSpace growSpace = findGrowSpaceByUserPlant(userPlant);

        validateSoilMoistureNotTooWet(userPlant);

        PumpChannel pumpChannel = pumpChannelRepository
                .findByUserPlantAndActiveTrueAndDeletedFalse(userPlant)
                .orElseThrow(() -> new IllegalStateException("해당 식물에 연결된 펌프 채널이 없습니다."));

        validatePumpChannel(growSpace, userPlant, pumpChannel);

        // [GreenLink] 급수 보호모드가 켜진 경우에만 연속 물주기 제한을 적용한다.
        if (isWateringSafetyEnabled(userPlant)) {
            validateNoPendingWaterCommand(userPlant, pumpChannel);

            validateWaterCooldown(userPlant);

            validateWaterRequestLimit(userPlant);
        }

        DeviceCommand command = DeviceCommand.createWaterCommand(
                growSpace,
                userPlant,
                pumpChannel.getRaspberryDevice(),
                pumpChannel
        );

        DeviceCommand savedCommand = deviceCommandRepository.save(command);

        return IotAppDto.WaterCommandResDto.from(savedCommand);
    }

    @Transactional
    public IotAppDto.LightCommandResDto requestLightOn(
            Long userId,
            Long userPlantId
    ) {
        return requestLightCommand(
                userId,
                userPlantId,
                CommandType.LIGHT_ON
        );
    }

    @Transactional
    public IotAppDto.LightCommandResDto requestLightOff(
            Long userId,
            Long userPlantId
    ) {
        return requestLightCommand(
                userId,
                userPlantId,
                CommandType.LIGHT_OFF
        );
    }

    @Transactional
    public IotAppDto.SensorRefreshResDto requestSensorRefresh(
            Long userId,
            Long userPlantId
    ) {
        User user = findActiveUser(userId);

        UserPlant userPlant = findMyUserPlant(userPlantId, user);

        GrowSpace growSpace = findGrowSpaceByUserPlant(userPlant);

        IotDevice raspberryDevice = findActiveRaspberryDevice(growSpace);

        return findPendingSensorRefreshCommand(growSpace, raspberryDevice)
                .map(command -> IotAppDto.SensorRefreshResDto.from(
                        command,
                        true,
                        "PENDING_SENSOR_REFRESH"
                ))
                .orElseGet(() -> findRecentSensorRefreshCommand(growSpace, raspberryDevice)
                        .map(command -> IotAppDto.SensorRefreshResDto.from(
                                command,
                                true,
                                "SENSOR_REFRESH_COOLDOWN"
                        ))
                        .orElseGet(() -> {
                            // [GreenLink] SENSOR_REFRESH는 라즈베리파이 온도/습도/조도만 즉시 갱신한다.
                            DeviceCommand command = DeviceCommand.createSensorRefreshCommand(
                                    growSpace,
                                    userPlant,
                                    raspberryDevice
                            );

                            DeviceCommand savedCommand = deviceCommandRepository.save(command);

                            return IotAppDto.SensorRefreshResDto.from(
                                    savedCommand,
                                    false,
                                    null
                            );
                        }));
    }

    private IotAppDto.LightCommandResDto requestLightCommand(
            Long userId,
            Long userPlantId,
            CommandType commandType
    ) {
        User user = findActiveUser(userId);

        UserPlant userPlant = findMyUserPlant(userPlantId, user);

        GrowSpace growSpace = findGrowSpaceByUserPlant(userPlant);

        IotDevice raspberryDevice = findActiveRaspberryDevice(growSpace);

        validateNoPendingLightCommand(userPlant);

        DeviceCommand command = DeviceCommand.createLightCommand(
                growSpace,
                userPlant,
                raspberryDevice,
                commandType
        );

        DeviceCommand savedCommand = deviceCommandRepository.save(command);

        return IotAppDto.LightCommandResDto.from(savedCommand);
    }

    private IotDevice findActiveRaspberryDevice(GrowSpace growSpace) {
        return iotDeviceRepository
                .findFirstByGrowSpaceAndDeviceTypeAndActiveTrueAndDeletedFalse(
                        growSpace,
                        DeviceType.RASPBERRY_PI
                )
                .orElseThrow(() -> new IllegalStateException("재배 공간에 연결된 라즈베리파이가 없습니다."));
    }

    private Optional<DeviceCommand> findPendingSensorRefreshCommand(
            GrowSpace growSpace,
            IotDevice raspberryDevice
    ) {
        return deviceCommandRepository
                .findTopByGrowSpaceAndIotDeviceAndCommandTypeAndCommandStatusInAndDeletedFalseOrderByRequestedAtDesc(
                        growSpace,
                        raspberryDevice,
                        CommandType.SENSOR_REFRESH,
                        List.of(CommandStatus.PENDING, CommandStatus.PROCESSING)
                );
    }

    private Optional<DeviceCommand> findRecentSensorRefreshCommand(
            GrowSpace growSpace,
            IotDevice raspberryDevice
    ) {
        LocalDateTime after = LocalDateTime.now()
                .minusSeconds(IotThresholds.SENSOR_REFRESH_COOLDOWN_SECONDS);

        return deviceCommandRepository
                .findTopByGrowSpaceAndIotDeviceAndCommandTypeAndRequestedAtAfterAndDeletedFalseOrderByRequestedAtDesc(
                        growSpace,
                        raspberryDevice,
                        CommandType.SENSOR_REFRESH,
                        after
                );
    }

    private void validateNoPendingLightCommand(UserPlant userPlant) {
        boolean lightOnExists = deviceCommandRepository
                .existsByUserPlantAndCommandTypeAndCommandStatusInAndDeletedFalse(
                        userPlant,
                        CommandType.LIGHT_ON,
                        List.of(CommandStatus.PENDING, CommandStatus.PROCESSING)
                );

        boolean lightOffExists = deviceCommandRepository
                .existsByUserPlantAndCommandTypeAndCommandStatusInAndDeletedFalse(
                        userPlant,
                        CommandType.LIGHT_OFF,
                        List.of(CommandStatus.PENDING, CommandStatus.PROCESSING)
                );

        if (lightOnExists || lightOffExists) {
            throw new IllegalStateException("이미 처리 중인 조명 명령이 있습니다.");
        }
    }

    private void validateSoilMoistureNotTooWet(UserPlant userPlant) {
        EspSensorData latestSoil = espSensorDataRepository
                .findFirstByUserPlantAndDeletedFalseOrderByMeasuredAtDesc(userPlant)
                .orElse(null);

        if (latestSoil == null || latestSoil.getSoilMoisturePercent() == null) {
            return;
        }

        Double percent = latestSoil.getSoilMoisturePercent();

        if (percent >= IotThresholds.SOIL_MOISTURE_TOO_WET_PERCENT) {
            throw WateringBlockedException.tooWet(
                    userPlant.getId(),
                    percent,
                    IotThresholds.SOIL_MOISTURE_TOO_WET_PERCENT
            );
        }
    }

    private void validatePumpChannel(
            GrowSpace growSpace,
            UserPlant userPlant,
            PumpChannel pumpChannel
    ) {
        if (!pumpChannel.getGrowSpace().getId().equals(growSpace.getId())) {
            throw new IllegalStateException("펌프 채널이 해당 재배 공간에 속해 있지 않습니다.");
        }

        if (!pumpChannel.getUserPlant().getId().equals(userPlant.getId())) {
            throw new IllegalStateException("펌프 채널이 해당 식물에 연결되어 있지 않습니다.");
        }

        if (pumpChannel.getRaspberryDevice() == null) {
            throw new IllegalStateException("펌프 채널에 연결된 라즈베리파이가 없습니다.");
        }

        if (!pumpChannel.getRaspberryDevice().isRaspberryPi()) {
            throw new IllegalStateException("펌프 채널은 라즈베리파이 기기에 연결되어야 합니다.");
        }

        if (!pumpChannel.getRaspberryDevice().getActive()) {
            throw new IllegalStateException("라즈베리파이 기기가 비활성화되어 있습니다.");
        }
    }

    private boolean isWateringSafetyEnabled(UserPlant userPlant) {
        return automationSettingRepository
                .findByUserPlantAndDeletedFalse(userPlant)
                .map(setting -> setting.getWateringSafetyEnabled() == null
                        || Boolean.TRUE.equals(setting.getWateringSafetyEnabled()))
                .orElse(true);
    }

    private void validateNoPendingWaterCommand(
            UserPlant userPlant,
            PumpChannel pumpChannel
    ) {
        // [GreenLink] 미처리 WATER command가 있으면 중복 물주기를 방지한다.
        boolean existsByUserPlant = deviceCommandRepository
                .existsByUserPlantAndCommandTypeAndCommandStatusInAndDeletedFalse(
                        userPlant,
                        CommandType.WATER,
                        List.of(CommandStatus.PENDING, CommandStatus.PROCESSING)
                );

        boolean existsByPumpChannel = deviceCommandRepository
                .existsByPumpChannelAndCommandTypeAndCommandStatusInAndDeletedFalse(
                        pumpChannel,
                        CommandType.WATER,
                        List.of(CommandStatus.PENDING, CommandStatus.PROCESSING)
                );

        if (existsByUserPlant || existsByPumpChannel) {
            throw WateringBlockedException.pendingWaterCommand(userPlant.getId());
        }
    }

    private void validateWaterCooldown(UserPlant userPlant) {
        // [GreenLink] 토양 수분센서는 주기적으로 갱신되므로 연속 물주기 요청을 서버에서 제한한다.
        LocalDateTime after = LocalDateTime.now()
                .minusSeconds(IotThresholds.WATER_COOLDOWN_SECONDS);

        boolean existsRecent = deviceCommandRepository
                .existsByUserPlantAndCommandTypeAndRequestedAtAfterAndDeletedFalse(
                        userPlant,
                        CommandType.WATER,
                        after
                );

        if (existsRecent) {
            throw WateringBlockedException.cooldown(
                    userPlant.getId(),
                    IotThresholds.WATER_COOLDOWN_SECONDS
            );
        }
    }

    private void validateWaterRequestLimit(UserPlant userPlant) {
        // [GreenLink] 10분 내 3회까지만 물주기를 허용해 과도한 급수를 막는다.
        LocalDateTime after = LocalDateTime.now()
                .minusSeconds(IotThresholds.WATER_WINDOW_SECONDS);

        long recentCount = deviceCommandRepository
                .countByUserPlantAndCommandTypeAndRequestedAtAfterAndDeletedFalse(
                        userPlant,
                        CommandType.WATER,
                        after
                );

        if (recentCount >= IotThresholds.WATER_MAX_REQUESTS_PER_WINDOW) {
            throw WateringBlockedException.requestLimit(
                    userPlant.getId(),
                    IotThresholds.WATER_WINDOW_SECONDS,
                    IotThresholds.WATER_MAX_REQUESTS_PER_WINDOW
            );
        }
    }

    private GrowSpace findGrowSpaceByUserPlant(UserPlant userPlant) {
        GrowSpacePlant growSpacePlant = growSpacePlantRepository
                .findByUserPlantAndActiveTrueAndDeletedFalse(userPlant)
                .orElseThrow(() -> new IllegalStateException("해당 식물이 재배 공간에 연결되어 있지 않습니다."));

        return growSpacePlant.getGrowSpace();
    }

    private UserPlant findMyUserPlant(
            Long userPlantId,
            User user
    ) {
        return userPlantRepository.findByIdAndUserAndDeletedFalse(userPlantId, user)
                .orElseThrow(() -> new IllegalArgumentException("식물을 찾을 수 없습니다."));
    }

    private User findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
