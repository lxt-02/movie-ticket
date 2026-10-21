package com.mv.userservice.infrastructure.adapter;

import com.mv.userservice.application.port.out.LoadUserPort;
import com.mv.userservice.domain.model.user.aggregate.User;
import com.mv.userservice.domain.repository.UserRepository;
import com.mv.userservice.infrastructure.persistence.entity.UserEntity;
import com.mv.userservice.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.mv.userservice.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository, LoadUserPort, com.mv.userservice.application.port.out.SaveUserPort {

    private final UserJpaRepository userJpaRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public com.mv.userservice.application.dto.PageResult<User> search(String keyword,
            com.mv.userservice.domain.model.user.enums.UserRole role,
            com.mv.userservice.domain.model.user.enums.UserStatus status, int page, int size) {
        var result = userJpaRepository.search(keyword, role, status,
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by("fullName")));
        return new com.mv.userservice.application.dto.PageResult<>(
                result.getContent().stream().map(mapper::toDomain).toList(), page, size, result.getTotalElements());
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = userJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
