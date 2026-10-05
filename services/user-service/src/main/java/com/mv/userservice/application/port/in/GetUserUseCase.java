package com.mv.userservice.application.port.in;

import com.mv.userservice.application.query.GetUserQuery;
import com.mv.userservice.domain.model.user.aggregate.User;

public interface GetUserUseCase {
    User getById(GetUserQuery query);

    User validateUser(GetUserQuery query);
}
