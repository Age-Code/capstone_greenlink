package com.greenlink.greenlink.controller;

import com.greenlink.greenlink.domain.iot.DeviceType;
import com.greenlink.greenlink.domain.item.ItemType;
import com.greenlink.greenlink.domain.quest.QuestType;
import com.greenlink.greenlink.domain.quest.ResetCycle;
import com.greenlink.greenlink.domain.quest.TargetType;
import com.greenlink.greenlink.dto.AdminDto;
import com.greenlink.greenlink.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWebController {

    private final AdminService adminService;

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping({"", "/", "/index"})
    public String index() {
        return "admin/index";
    }

    // --- User Management ---
    @GetMapping("/users")
    public String userList(Model model) {
        model.addAttribute("users", adminService.getAllUsers());
        return "admin/user/list";
    }

    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        model.addAttribute("user", adminService.getUser(id));
        return "admin/user/detail";
    }

    @PostMapping("/users/{id}/toggle-role")
    public String toggleUserRole(@PathVariable Long id) {
        adminService.toggleUserRole(id);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return "redirect:/admin/users";
    }

    // --- Plant Management ---
    @GetMapping("/plants")
    public String plantList(Model model) {
        model.addAttribute("plants", adminService.getAllPlants());
        return "admin/plant/list";
    }

    @GetMapping("/plants/new")
    public String createPlantForm(Model model) {
        model.addAttribute("plantDto", new AdminDto.CreatePlantReqDto());
        return "admin/plant/create";
    }

    @PostMapping("/plants")
    public String createPlant(@ModelAttribute AdminDto.CreatePlantReqDto plantDto, Model model) {
        try {
            adminService.createPlant(plantDto);
            return "redirect:/admin/plants";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("plantDto", plantDto);
            return "admin/plant/create";
        }
    }

    // --- Item Management ---
    @GetMapping("/items")
    public String itemList(Model model) {
        model.addAttribute("items", adminService.getAllItems());
        return "admin/item/list";
    }

    @GetMapping("/items/new")
    public String createItemForm(Model model) {
        model.addAttribute("itemDto", new AdminDto.CreateItemReqDto());
        model.addAttribute("itemTypes", ItemType.values());
        return "admin/item/create";
    }

    @PostMapping("/items")
    public String createItem(@ModelAttribute AdminDto.CreateItemReqDto itemDto, Model model) {
        try {
            adminService.createItem(itemDto);
            return "redirect:/admin/items";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("itemDto", itemDto);
            model.addAttribute("itemTypes", ItemType.values());
            return "admin/item/create";
        }
    }

    // --- Quest Management ---
    @GetMapping("/quests")
    public String questList(Model model) {
        model.addAttribute("quests", adminService.getAllQuests());
        return "admin/quest/list";
    }

    @GetMapping("/quests/new")
    public String createQuestForm(Model model) {
        model.addAttribute("questDto", new AdminDto.CreateQuestReqDto());
        model.addAttribute("questTypes", QuestType.values());
        model.addAttribute("targetTypes", TargetType.values());
        model.addAttribute("resetCycles", ResetCycle.values());
        return "admin/quest/create";
    }

    @PostMapping("/quests")
    public String createQuest(@ModelAttribute AdminDto.CreateQuestReqDto questDto, Model model) {
        try {
            adminService.createQuest(questDto);
            return "redirect:/admin/quests";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("questDto", questDto);
            model.addAttribute("questTypes", QuestType.values());
            model.addAttribute("targetTypes", TargetType.values());
            model.addAttribute("resetCycles", ResetCycle.values());
            return "admin/quest/create";
        }
    }

    // --- IoT Management ---
    @GetMapping("/iot")
    public String iotList(Model model) {
        model.addAttribute("devices", adminService.getAllIotDevices());
        return "admin/iot/list";
    }

    @GetMapping("/iot/new")
    public String createIotForm(Model model) {
        model.addAttribute("iotDto", new AdminDto.CreateIotDeviceReqDto());
        model.addAttribute("deviceTypes", DeviceType.values());
        return "admin/iot/create";
    }

    @PostMapping("/iot")
    public String createIot(@ModelAttribute AdminDto.CreateIotDeviceReqDto iotDto, Model model) {
        try {
            adminService.createIotDevice(iotDto);
            return "redirect:/admin/iot";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("iotDto", iotDto);
            model.addAttribute("deviceTypes", DeviceType.values());
            return "admin/iot/create";
        }
    }
}
