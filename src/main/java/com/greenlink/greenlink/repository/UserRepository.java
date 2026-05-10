package com.greenlink.greenlink.repository;

import com.greenlink.greenlink.domain.user.LoginProvider;
import com.greenlink.greenlink.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedFalse(String email);

    boolean existsByEmailAndDeletedFalse(String email);

    Optional<User> findByProviderAndProviderIdAndDeletedFalse(
            LoginProvider provider,
            String providerId);

    java.util.List<User> findAllByDeletedFalse();
}