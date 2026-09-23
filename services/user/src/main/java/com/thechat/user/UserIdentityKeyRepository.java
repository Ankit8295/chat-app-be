package com.thechat.user;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityKeyRepository extends JpaRepository<UserIdentityKey, UUID> {

    List<UserIdentityKey> findAllByUserIdIn(Collection<UUID> userIds);
}
