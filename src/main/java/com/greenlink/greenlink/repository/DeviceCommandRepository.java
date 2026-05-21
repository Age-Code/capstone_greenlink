package com.greenlink.greenlink.repository;

import com.greenlink.greenlink.domain.iot.CommandStatus;
import com.greenlink.greenlink.domain.iot.CommandType;
import com.greenlink.greenlink.domain.iot.DeviceCommand;
import com.greenlink.greenlink.domain.iot.GrowSpace;
import com.greenlink.greenlink.domain.iot.IotDevice;
import com.greenlink.greenlink.domain.iot.PumpChannel;
import com.greenlink.greenlink.domain.plant.UserPlant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {

    Optional<DeviceCommand> findByIdAndDeletedFalse(Long id);

    List<DeviceCommand> findAllByGrowSpaceAndDeletedFalseOrderByRequestedAtDesc(
            GrowSpace growSpace
    );

    List<DeviceCommand> findAllByUserPlantAndDeletedFalseOrderByRequestedAtDesc(
            UserPlant userPlant
    );

    List<DeviceCommand> findAllByIotDeviceAndDeletedFalseOrderByRequestedAtAsc(
            IotDevice iotDevice
    );

    List<DeviceCommand> findAllByIotDeviceAndCommandStatusAndDeletedFalseOrderByRequestedAtAsc(
            IotDevice iotDevice,
            CommandStatus commandStatus
    );

    List<DeviceCommand> findAllByIotDeviceAndCommandStatusInAndDeletedFalseOrderByRequestedAtAsc(
            IotDevice iotDevice,
            Collection<CommandStatus> commandStatuses
    );

    List<DeviceCommand> findAllByIotDeviceAndCommandTypeAndCommandStatusAndDeletedFalseOrderByRequestedAtAsc(
            IotDevice iotDevice,
            CommandType commandType,
            CommandStatus commandStatus
    );

    boolean existsByUserPlantAndCommandTypeAndCommandStatusInAndDeletedFalse(
            UserPlant userPlant,
            CommandType commandType,
            Collection<CommandStatus> commandStatuses
    );

    boolean existsByPumpChannelAndCommandTypeAndCommandStatusInAndDeletedFalse(
            PumpChannel pumpChannel,
            CommandType commandType,
            Collection<CommandStatus> commandStatuses
    );

    Optional<DeviceCommand> findTopByGrowSpaceAndIotDeviceAndCommandTypeAndCommandStatusInAndDeletedFalseOrderByRequestedAtDesc(
            GrowSpace growSpace,
            IotDevice iotDevice,
            CommandType commandType,
            Collection<CommandStatus> commandStatuses
    );

    boolean existsByUserPlantAndCommandTypeAndRequestedAtAfterAndDeletedFalse(
            UserPlant userPlant,
            CommandType commandType,
            LocalDateTime requestedAt
    );

    long countByUserPlantAndCommandTypeAndRequestedAtAfterAndDeletedFalse(
            UserPlant userPlant,
            CommandType commandType,
            LocalDateTime requestedAt
    );

    Optional<DeviceCommand> findTopByGrowSpaceAndIotDeviceAndCommandTypeAndRequestedAtAfterAndDeletedFalseOrderByRequestedAtDesc(
            GrowSpace growSpace,
            IotDevice iotDevice,
            CommandType commandType,
            LocalDateTime requestedAt
    );

    /**
     * 특정 식물의 특정 명령 타입 중 가장 최근 명령 조회
     *
     * 물 주기 쿨다운 확인에 사용한다.
     */
    Optional<DeviceCommand> findTopByUserPlantAndCommandTypeAndDeletedFalseOrderByRequestedAtDesc(
            UserPlant userPlant,
            CommandType commandType
    );

    List<DeviceCommand> findByUserPlantAndCommandTypeAndRequestedAtBetweenAndDeletedFalseOrderByRequestedAtAsc(
            UserPlant userPlant,
            CommandType commandType,
            LocalDateTime start,
            LocalDateTime end
    );
}
