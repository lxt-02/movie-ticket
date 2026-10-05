package com.mv.userservice.application.service;

import com.mv.userservice.application.port.in.GetUserUseCase;
import com.mv.userservice.application.port.out.LoadUserPort;
import com.mv.userservice.application.query.GetUserQuery;
import com.mv.userservice.domain.model.user.aggregate.User;
import com.mv.userservice.domain.model.user.exception.UserBlockedException;
import com.mv.userservice.domain.model.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUserService implements GetUserUseCase {

    private final LoadUserPort loadUserPort;

    @Override
    public User getById(GetUserQuery query) {
        return loadUserPort.findById(query.getUserId())
                .orElseThrow(() -> new UserNotFoundException(query.getUserId()));
    }

    @Override
    public User validateUser(GetUserQuery query) {
        User user = getById(query);
        if (!user.isActive()) {
            throw new UserBlockedException(query.getUserId());
        }
        return user;
    }
}
