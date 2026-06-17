package org.elis.ericsson.datathon.user_management.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.elis.ericsson.datathon.user_management.model.dto.request.CreateFirstUserRequestDto;
import org.elis.ericsson.datathon.user_management.model.dto.response.CreateFirstUserResponseDto;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.model.exception.RequestError;
import org.elis.ericsson.datathon.user_management.model.dto.AuthResponseDTO;
import org.elis.ericsson.datathon.user_management.model.dto.LoginDto;
import org.elis.ericsson.datathon.user_management.model.dto.TokenRefreshResponseDto;
import org.elis.ericsson.datathon.user_management.model.dto.request.SignUpRequestDto;
import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.model.entity.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.elis.ericsson.datathon.user_management.model.projection.UserMeInfo;

import java.util.List;

public interface AuthService {
    ResponseEntity<AuthResponseDTO> login(LoginDto loginDto);

    ResponseEntity<UserProfile> registerUser(SignUpRequestDto signUpRequestDto) throws Exception, RequestError;

    ResponseEntity<TokenRefreshResponseDto> refreshtoken(String refreshToken) throws Exception;

    ResponseEntity<CreateFirstUserResponseDto> createFirstUser(CreateFirstUserRequestDto requestDto) throws Exception;

    ResponseEntity<UserMeInfo> getCurrentUser(UserPrincipal userPrincipal);

    ResponseEntity<?> getAuthenticationToChangePassword(String token);

    ResponseEntity<Boolean> logout(HttpServletRequest request, HttpServletResponse response);

    ResponseEntity<List<Role>> getPossibleRoles();

    ResponseEntity<AuthResponseDTO> getSession();

}
