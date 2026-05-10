package com.greenlink.greenlink.repository;

import com.greenlink.greenlink.domain.quest.Quest;
import com.greenlink.greenlink.domain.quest.QuestType;
import com.greenlink.greenlink.domain.quest.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestRepository extends JpaRepository<Quest, Long> {
    List<Quest> findAllByDeletedFalse();

    List<Quest> findAllByActiveTrueAndDeletedFalse();

    List<Quest> findAllByQuestTypeAndActiveTrueAndDeletedFalse(QuestType questType);

    List<Quest> findAllByTargetTypeAndActiveTrueAndDeletedFalse(TargetType targetType);

    List<Quest> findAllByQuestTypeAndTargetTypeAndActiveTrueAndDeletedFalse(
            QuestType questType,
            TargetType targetType
    );

    Optional<Quest> findByIdAndActiveTrueAndDeletedFalse(Long id);

    boolean existsByTitleAndDeletedFalse(String title);
}