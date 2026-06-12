package org.elis.ericsson.datathon.user_management.controller;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;


import org.elis.ericsson.datathon.user_management.model.dto.TokenRefreshResponseDto;
import org.elis.ericsson.datathon.user_management.model.dto.AuthResponseDTO;
import org.elis.ericsson.datathon.user_management.model.dto.LoginDto;
import org.elis.ericsson.datathon.user_management.model.dto.request.SignUpRequestDto;
import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.model.exception.RequestError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface AuthController {

    @PostMapping("/login")
    ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid LoginDto loginDto);

    //sign up
    @PostMapping("/signup")
    ResponseEntity<UserProfile> registerUser(@Valid @RequestBody SignUpRequestDto signUpRequestDto) throws Exception, RequestError;



    @PostMapping("/logout")

    ResponseEntity<Boolean> logout(HttpServletRequest request, HttpServletResponse response);


    @PostMapping("/refreshToken")
    ResponseEntity<TokenRefreshResponseDto> refreshtoken(@RequestBody @Valid String refreshToken) throws Exception;


    @PostMapping("/tokenResetPassword")
    ResponseEntity<?> getAuthenticationToChangePassword(@RequestParam("token") String token);

    //get possible roles

    @GetMapping("/getPossibleRoles")
    ResponseEntity<List<Role>> getPossibleRoles();



    @GetMapping("/getSession")
    ResponseEntity<AuthResponseDTO> getSession();
}
