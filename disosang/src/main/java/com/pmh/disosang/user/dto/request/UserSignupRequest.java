package com.pmh.disosang.user.dto.request;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSignupRequest {

    private String loginid;
    private String name;
    private String email;
    private String password;

}
