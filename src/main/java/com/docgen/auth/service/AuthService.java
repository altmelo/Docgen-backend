package com.docgen.auth.service;

import com.docgen.auth.dto.AuthResponse;
import com.docgen.auth.dto.LoginRequest;
import com.docgen.auth.dto.RegisterRequest;
import com.docgen.auth.exception.AuthException;
import com.docgen.entity.User;
import com.docgen.entity.UserRole;
import com.docgen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        var user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setCpf(request.getCpf());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : UserRole.Operador);

        var savedUser = userRepository.save(user);
        var savedUserDetails = toUserDetails(savedUser);
        var accessToken = jwtService.generateAccessToken(savedUserDetails);
        var refreshToken = jwtService.generateRefreshToken(savedUserDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpiration())
                .tokenType("Bearer")
                .user(AuthResponse.UserInfo.builder()
                        .id(savedUser.getId().toString())
                        .name(savedUser.getName())
                        .email(savedUser.getEmail())
                        .cpf(savedUser.getCpf())
                        .role(savedUser.getRole().name())
                        .build())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        var userDetails = toUserDetails(user);
        var accessToken = jwtService.generateAccessToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpiration())
                .tokenType("Bearer")
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId().toString())
                        .name(user.getName())
                        .email(user.getEmail())
                        .cpf(user.getCpf())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        try {
            var userEmail = jwtService.extractUsername(refreshToken);
            if (userEmail == null || userEmail.isBlank()) {
                throw new AuthException("Refresh token inválido");
            }

            var user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new AuthException("Usuário não encontrado"));

            var userDetails = toUserDetails(user);
            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                throw new AuthException("Refresh token inválido ou expirado");
            }

            var newAccessToken = jwtService.generateAccessToken(userDetails);
            var newRefreshToken = jwtService.generateRefreshToken(userDetails);

            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(jwtService.getAccessTokenExpiration())
                    .tokenType("Bearer")
                    .user(AuthResponse.UserInfo.builder()
                            .id(user.getId().toString())
                            .name(user.getName())
                            .email(user.getEmail())
                            .cpf(user.getCpf())
                            .role(user.getRole().name())
                            .build())
                    .build();
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException("Falha ao processar refresh token", ex);
        }
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}
