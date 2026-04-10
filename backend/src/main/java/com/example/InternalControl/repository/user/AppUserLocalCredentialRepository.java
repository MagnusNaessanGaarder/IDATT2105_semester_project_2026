package com.example.InternalControl.repository.user;

import com.example.InternalControl.model.user.AppUserLocalCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserLocalCredentialRepository extends JpaRepository<AppUserLocalCredential, Long> {

    long deleteByUserUserId(Long userId);
}
