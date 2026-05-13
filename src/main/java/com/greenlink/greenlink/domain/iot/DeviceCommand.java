package com.greenlink.greenlink.domain.iot;

import com.greenlink.greenlink.domain.plant.UserPlant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "device_command")
public class DeviceCommand {

    private static final int DEFAULT_WATER_DURATION_SECONDS = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 명령이 발생한 재배 공간
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grow_space_id", nullable = false)
    private GrowSpace growSpace;

    /**
     * 명령 대상 식물
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_plant_id", nullable = false)
    private UserPlant userPlant;

    /**
     * 명령을 처리할 라즈베리파이
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iot_device_id", nullable = false)
    private IotDevice iotDevice;

    /**
     * WATER 명령일 때 사용할 펌프 채널
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pump_channel_id")
    private PumpChannel pumpChannel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommandType commandType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommandStatus commandStatus;

    /**
     * 실제 라즈베리파이가 펌프를 작동시킬 시간
     * 현재 MVP에서는 서버 고정값 5초를 사용한다.
     */
    private Integer durationSeconds;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime processedAt;

    private LocalDateTime completedAt;

    @Column(length = 500)
    private String resultMessage;

    @Column(nullable = false)
    private boolean deleted;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime modifiedAt;

    @Builder
    private DeviceCommand(
            GrowSpace growSpace,
            UserPlant userPlant,
            IotDevice iotDevice,
            PumpChannel pumpChannel,
            CommandType commandType,
            Integer durationSeconds
    ) {
        if (!iotDevice.isRaspberryPi()) {
            throw new IllegalStateException("명령은 라즈베리파이 기기로만 전달할 수 있습니다.");
        }

        this.growSpace = growSpace;
        this.userPlant = userPlant;
        this.iotDevice = iotDevice;
        this.pumpChannel = pumpChannel;
        this.commandType = commandType;
        this.commandStatus = CommandStatus.PENDING;
        this.durationSeconds = durationSeconds;
        this.requestedAt = LocalDateTime.now();
        this.deleted = false;
    }

    public static DeviceCommand createWaterCommand(
            GrowSpace growSpace,
            UserPlant userPlant,
            IotDevice raspberryDevice,
            PumpChannel pumpChannel
    ) {
        return DeviceCommand.builder()
                .growSpace(growSpace)
                .userPlant(userPlant)
                .iotDevice(raspberryDevice)
                .pumpChannel(pumpChannel)
                .commandType(CommandType.WATER)
                .durationSeconds(DEFAULT_WATER_DURATION_SECONDS)
                .build();
    }

    public static DeviceCommand createLightCommand(
            GrowSpace growSpace,
            UserPlant userPlant,
            IotDevice raspberryDevice,
            CommandType commandType
    ) {
        if (commandType != CommandType.LIGHT_ON &&
                commandType != CommandType.LIGHT_OFF) {
            throw new IllegalArgumentException("조명 명령 타입이 아닙니다.");
        }

        return DeviceCommand.builder()
                .growSpace(growSpace)
                .userPlant(userPlant)
                .iotDevice(raspberryDevice)
                .pumpChannel(null)
                .commandType(commandType)
                .durationSeconds(null)
                .build();
    }

    public void markProcessing() {
        if (this.commandStatus != CommandStatus.PENDING) {
            throw new IllegalStateException("대기 중인 명령만 처리 시작할 수 있습니다.");
        }

        this.commandStatus = CommandStatus.PROCESSING;
        this.processedAt = LocalDateTime.now();
    }

    public void completeSuccess(String resultMessage) {
        if (this.commandStatus != CommandStatus.PROCESSING) {
            throw new IllegalStateException("처리 중인 명령만 완료 처리할 수 있습니다.");
        }

        this.commandStatus = CommandStatus.SUCCESS;
        this.completedAt = LocalDateTime.now();
        this.resultMessage = resultMessage;
    }

    public void completeFailed(String resultMessage) {
        if (this.commandStatus != CommandStatus.PROCESSING) {
            throw new IllegalStateException("처리 중인 명령만 실패 처리할 수 있습니다.");
        }

        this.commandStatus = CommandStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.resultMessage = resultMessage;
    }

    public void cancel(String resultMessage) {
        if (this.commandStatus == CommandStatus.SUCCESS ||
                this.commandStatus == CommandStatus.FAILED) {
            throw new IllegalStateException("이미 완료된 명령은 취소할 수 없습니다.");
        }

        this.commandStatus = CommandStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
        this.resultMessage = resultMessage;
    }

    public void delete() {
        this.deleted = true;
    }

    public boolean isPendingOrProcessing() {
        return this.commandStatus == CommandStatus.PENDING ||
                this.commandStatus == CommandStatus.PROCESSING;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.modifiedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.modifiedAt = LocalDateTime.now();
    }
}