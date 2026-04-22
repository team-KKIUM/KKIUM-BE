package com.kusitms.kkium.user.service;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.dto.request.UserCreateRequest;
import com.kusitms.kkium.user.dto.response.UserResponse;
import com.kusitms.kkium.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        User user = userRepository.save(new User(request.name()));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new BaseException(USER_NOT_FOUND));
        return UserResponse.from(user);
    }
}
