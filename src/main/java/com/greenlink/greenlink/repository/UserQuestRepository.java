package com.greenlink.greenlink.repository;

import com.greenlink.greenlink.domain.quest.Quest;
import com.greenlink.greenlink.domain.quest.QuestType;
import com.greenlink.greenlink.domain.quest.UserQuest;
import com.greenlink.greenlink.domain.quest.UserQuestStatus;
import com.greenlink.greenlink.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {

    Optional<UserQuest> findByIdAndUserAndDeletedFalse(Long id, User user);

    List<UserQuest> findAllByUserAndDeletedFalse(User user);

    boolean existsByUserAndQuestAndDeletedFalse(User user, Quest quest);

    List<UserQuest> findAllByUserAndQuest_QuestTypeAndDeletedFalse(
            User user,
            QuestType questType
    );

    List<UserQuest> findAllByUserAndStatusAndDeletedFalse(
            User user,
            UserQuestStatus status
    );

    List<UserQuest> findAllByUserAndQuest_QuestTypeAndStatusAndDeletedFalse(
            User user,
            QuestType questType,
            UserQuestStatus status
    );

    Optional<UserQuest> findByUserAndQuestAndStartedAtAndDeletedFalse(
            User user,
            Quest quest,
            LocalDateTime startedAt
    );

    boolean existsByUserAndQuestAndStartedAtAndDeletedFalse(
            User user,
            Quest quest,
            LocalDateTime startedAt
    );

    Optional<UserQuest> findFirstByUserAndQuestAndDeletedFalse(
            User user,
            Quest quest
    );
}