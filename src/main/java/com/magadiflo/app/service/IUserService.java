package com.magadiflo.app.service;

import com.magadiflo.app.domain.User;

import java.util.List;
import java.util.Optional;

public interface IUserService {
    List<User> findAllUsers();

    Optional<User> findUser(Long id);

    User saveUser(User user);

    Boolean verifyToken(String token);
}
