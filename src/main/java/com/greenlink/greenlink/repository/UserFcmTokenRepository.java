package com.greenlink.greenlink.repository;

import com.greenlink.greenlink.domain.notification.UserFcmToken;
import com.greenlink.greenlink.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

    Optional<UserFcmToken> findByUserAndFcmToken(User user, String fcmToken);

    List<UserFcmToken> findAllByUserAndDeletedFalse(User user);

    List<UserFcmToken> findAllByUserIdAndDeletedFalse(Long userId);
}