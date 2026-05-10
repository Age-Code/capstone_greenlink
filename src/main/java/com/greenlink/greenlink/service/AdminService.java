package com.greenlink.greenlink.service;

import com.greenlink.greenlink.domain.item.Item;
import com.greenlink.greenlink.domain.item.ItemType;
import com.greenlink.greenlink.domain.plant.Plant;
import com.greenlink.greenlink.domain.quest.Quest;
import com.greenlink.greenlink.dto.AdminDto;
import com.greenlink.greenlink.repository.ItemRepository;
import com.greenlink.greenlink.repository.PlantRepository;
import com.greenlink.greenlink.repository.QuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final PlantRepository plantRepository;
    private final ItemRepository itemRepository;
    private final QuestRepository questRepository;

    @Transactional
    public AdminDto.PlantResDto createPlant(AdminDto.CreatePlantReqDto request) {
        if (plantRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new IllegalArgumentException("이미 등록된 식물 이름입니다.");
        }

        Plant plant = Plant.create(
                request.getName(),
                request.getCategory(),
                request.getDescription(),
                request.getImageUrl(),
                request.getGrowthDays());

        Plant savedPlant = plantRepository.save(plant);

        return AdminDto.PlantResDto.from(savedPlant);
    }

    @Transactional
    public AdminDto.ItemResDto createItem(AdminDto.CreateItemReqDto request) {
        if (itemRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new IllegalArgumentException("이미 등록된 아이템 이름입니다.");
        }

        Item item;

        if (request.getItemType() == ItemType.SEED) {
            if (request.getLinkedPlantId() == null) {
                throw new IllegalArgumentException("씨앗 아이템은 linkedPlantId가 필요합니다.");
            }

            Plant linkedPlant = plantRepository.findByIdAndDeletedFalse(request.getLinkedPlantId())
                    .orElseThrow(() -> new IllegalArgumentException("연결할 식물을 찾을 수 없습니다."));

            item = Item.createSeed(
                    request.getName(),
                    request.getDescription(),
                    request.getImageUrl(),
                    linkedPlant);
        } else if (request.getItemType() == ItemType.POT) {
            item = Item.createPot(
                    request.getName(),
                    request.getDescription(),
                    request.getImageUrl());
        } else if (request.getItemType() == ItemType.NUTRIENT) {
            item = Item.createNutrient(
                    request.getName(),
                    request.getDescription(),
                    request.getImageUrl());
        } else {
            throw new IllegalArgumentException("지원하지 않는 아이템 타입입니다.");
        }

        Item savedItem = itemRepository.save(item);

        return AdminDto.ItemResDto.from(savedItem);
    }

    @Transactional
    public AdminDto.QuestResDto createQuest(AdminDto.CreateQuestReqDto request) {
        if (questRepository.existsByTitleAndDeletedFalse(request.getTitle())) {
            throw new IllegalArgumentException("이미 등록된 퀘스트 제목입니다.");
        }

        Item rewardItem = null;

        if (request.getRewardItemId() != null) {
            rewardItem = itemRepository.findByIdAndDeletedFalse(request.getRewardItemId())
                    .orElseThrow(() -> new IllegalArgumentException("보상 아이템을 찾을 수 없습니다."));
        }

        if (request.getRewardQuantity() > 0 && rewardItem == null) {
            throw new IllegalArgumentException("보상 수량이 1개 이상이면 rewardItemId가 필요합니다.");
        }

        Quest quest = Quest.create(
                request.getTitle(),
                request.getDescription(),
                request.getQuestType(),
                request.getTargetType(),
                request.getTargetValue(),
                rewardItem,
                request.getRewardQuantity(),
                request.getResetCycle());

        Quest savedQuest = questRepository.save(quest);

        return AdminDto.QuestResDto.from(savedQuest);
    }

    private final com.greenlink.greenlink.repository.UserRepository userRepository;
    private final com.greenlink.greenlink.repository.IotDeviceRepository iotDeviceRepository;
    private final com.greenlink.greenlink.repository.GrowSpaceRepository growSpaceRepository;
    private final com.greenlink.greenlink.repository.UserPlantRepository userPlantRepository;

    public java.util.List<com.greenlink.greenlink.domain.user.User> getAllUsers() {
        return userRepository.findAllByDeletedFalse();
    }

    public com.greenlink.greenlink.domain.user.User getUser(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional
    public void toggleUserRole(Long id) {
        userRepository.findById(id).ifPresent(com.greenlink.greenlink.domain.user.User::toggleRole);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.findById(id).ifPresent(com.greenlink.greenlink.domain.user.User::delete);
    }

    public java.util.List<Plant> getAllPlants() {
        return plantRepository.findAllByDeletedFalse();
    }

    public java.util.List<Item> getAllItems() {
        return itemRepository.findAllByDeletedFalse();
    }

    public java.util.List<Quest> getAllQuests() {
        return questRepository.findAllByDeletedFalse();
    }

    public java.util.List<com.greenlink.greenlink.domain.iot.IotDevice> getAllIotDevices() {
        return iotDeviceRepository.findAllByDeletedFalse();
    }

    @Transactional
    public AdminDto.IotDeviceResDto createIotDevice(AdminDto.CreateIotDeviceReqDto request) {
        if (iotDeviceRepository.existsByDeviceKeyAndDeletedFalse(request.getDeviceKey())) {
            throw new IllegalArgumentException("이미 존재하는 기기 키입니다.");
        }

        com.greenlink.greenlink.domain.iot.IotDevice device;

        if (request.getDeviceType() == com.greenlink.greenlink.domain.iot.DeviceType.RASPBERRY_PI) {
            if (request.getGrowSpaceId() == null) {
                throw new IllegalArgumentException("라즈베리파이는 GrowSpace ID가 필요합니다.");
            }
            com.greenlink.greenlink.domain.iot.GrowSpace growSpace = growSpaceRepository.findByIdAndDeletedFalse(request.getGrowSpaceId())
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 GrowSpace ID입니다."));
            device = com.greenlink.greenlink.domain.iot.IotDevice.createRaspberryPi(growSpace, request.getDeviceName(), request.getDeviceKey());
        } else if (request.getDeviceType() == com.greenlink.greenlink.domain.iot.DeviceType.ESP32) {
            if (request.getUserPlantId() == null) {
                throw new IllegalArgumentException("ESP32는 UserPlant ID가 필요합니다.");
            }
            com.greenlink.greenlink.domain.iot.GrowSpace growSpace = null;
            if (request.getGrowSpaceId() != null) {
                 growSpace = growSpaceRepository.findByIdAndDeletedFalse(request.getGrowSpaceId()).orElse(null);
            }
            com.greenlink.greenlink.domain.plant.UserPlant userPlant = userPlantRepository.findByIdAndDeletedFalse(request.getUserPlantId())
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 UserPlant ID입니다."));
            device = com.greenlink.greenlink.domain.iot.IotDevice.createEsp32(growSpace, userPlant, request.getDeviceName(), request.getDeviceKey());
        } else {
            throw new IllegalArgumentException("지원하지 않는 기기 타입입니다.");
        }

        return AdminDto.IotDeviceResDto.from(iotDeviceRepository.save(device));
    }
}