package com.greenlink.greenlink.repository;

import com.greenlink.greenlink.domain.item.Item;
import com.greenlink.greenlink.domain.item.ItemType;
import com.greenlink.greenlink.domain.item.UserItem;
import com.greenlink.greenlink.domain.item.UserItemStatus;
import com.greenlink.greenlink.domain.plant.UserPlant;
import com.greenlink.greenlink.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    Optional<UserItem> findByIdAndUserAndDeletedFalse(Long id, User user);

    boolean existsByUserAndItemAndDeletedFalse(User user, Item item);

    List<UserItem> findAllByUserAndDeletedFalse(User user);

    List<UserItem> findAllByUserAndStatusAndDeletedFalse(
            User user,
            UserItemStatus status
    );

    List<UserItem> findAllByUserAndItem_ItemTypeAndDeletedFalse(
            User user,
            ItemType itemType
    );

    List<UserItem> findAllByUserAndItem_ItemTypeAndStatusAndDeletedFalse(
            User user,
            ItemType itemType,
            UserItemStatus status
    );

    List<UserItem> findAllByUserAndItemAndDeletedFalse(
            User user,
            Item item
    );

    long countByUserAndItemAndStatusInAndDeletedFalse(
            User user,
            Item item,
            Collection<UserItemStatus> statuses
    );

    long countByUserAndItemAndStatusAndDeletedFalse(
            User user,
            Item item,
            UserItemStatus status
    );

    Optional<UserItem> findFirstByUserAndItem_ItemTypeAndUserPlantAndStatusAndDeletedFalse(
            User user,
            ItemType itemType,
            UserPlant userPlant,
            UserItemStatus status
    );

    List<UserItem> findAllByUserAndItem_ItemTypeAndUserPlantAndStatusAndDeletedFalse(
            User user,
            ItemType itemType,
            UserPlant userPlant,
            UserItemStatus status
    );
}