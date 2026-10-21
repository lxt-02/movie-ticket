package com.mv.userservice.application.port.out;

import com.mv.userservice.domain.model.user.aggregate.User;

public interface SaveUserPort {
    User save(User user);
}
