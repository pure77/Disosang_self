package com.pmh.disosang.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // 다음 URL들은 인증 없이 모든 사용자가 접근할 수 있도록 허용합니다.
                        .requestMatchers("/", "/user/signup", "/user/login", "/css/**", "/js/**").permitAll()
                        // 위에서 허용한 URL을 제외한 모든 요청은 인증을 받아야만 합니다.
                        .anyRequest().authenticated()
                )
                .formLogin(formLogin -> formLogin
                        // 1. 우리가 직접 만든 로그인 페이지 주소
                        .loginPage("/login")
                        // 2. 로그인 폼 데이터가 전송될 주소 (Security가 이 주소로 오는 요청을 처리)
                        .loginProcessingUrl("/loginProc")
                        // 3.로그인 폼의 아이디 입력 필드 name 속성 값 (기본값 username에서 email로 변경)
                        .usernameParameter("email")
                        // 4. 로그인 성공 시 이동할 기본 주소
                        .defaultSuccessUrl("/home/home", true)
                        // 5. 로그인 실패 시 이동할 주소
                        .failureUrl("/user/login?error=true")
                        .permitAll()
                ).rememberMe(rememberMe -> rememberMe
                        .key("my-remember-me-key") // 쿠키 암호화에 사용할 키 (아무 문자열이나 가능)
                        .tokenValiditySeconds(86400 * 14)) // 쿠키 만료 시간 (예: 14일)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true));

        return http.build();
    }

}
