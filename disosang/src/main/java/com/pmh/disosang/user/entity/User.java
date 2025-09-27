package com.pmh.disosang.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED) // ✅ 기본 생성자 추가
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true") // DB 기본값 설정
    private boolean isActive;

    @Builder
    public User(Long id, String loginId, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.isActive = true;
    }

    //UserDetails 인터페이스의 메서드들을 오버라이드하여 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 사용자의 권한 목록을 반환합니다. 지금은 간단히 비워둡니다.
        return Collections.emptyList();
    }

    @Override
    public String getUsername() {
        // Spring Security에서 username으로 사용할 값을 반환합니다. (우리는 email을 사용)
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        // 계정이 만료되지 않았는지 여부를 반환합니다. (true: 만료되지 않음)
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 계정이 잠기지 않았는지 여부를 반환합니다. (true: 잠기지 않음)
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 자격 증명(비밀번호)이 만료되지 않았는지 여부를 반환합니다. (true: 만료되지 않음)
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 계정이 활성화 상태인지 여부를 반환합니다. (DB의 is_active 컬럼 값 사용)
        return this.isActive;
    }
}
