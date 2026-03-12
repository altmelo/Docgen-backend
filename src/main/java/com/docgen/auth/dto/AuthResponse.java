package com.docgen.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;
    private UserInfo user;
    
    @Data
    @Builder
    public static class UserInfo {
        private String id;
        private String name;
        private String email;
        private String cpf;
        private String role;
    }
}