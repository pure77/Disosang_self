package com.pmh.disosang.user.service;

import com.pmh.disosang.user.UserRepository;
import com.pmh.disosang.user.dto.request.UserSignupRequest;
import com.pmh.disosang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Setter
@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(UserSignupRequest signupRequest) {

        //이메일 중복확인
        if (userRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }

        //비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());

        //사용자 정보 생성 및 저장
        User user = User.builder()
                .name(signupRequest.getName())
                .email(signupRequest.getEmail())
                .password(encodedPassword)
                .build();

        userRepository.save(user);

    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을수 없습니다"));

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
    }


    @Transactional
    public User findByEmailUserInfo(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자를 찾을수 없습니다"));
    }
}
