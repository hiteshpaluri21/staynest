package com.staynest.iam.repository;

import com.staynest.iam.entity.User;
import com.staynest.iam.enums.Role;
import com.staynest.iam.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    List<User> findByStatus(UserStatus status);
    List<User> findByRoleAndStatus(Role role, UserStatus status);
    boolean existsByEmail(String email);
}
