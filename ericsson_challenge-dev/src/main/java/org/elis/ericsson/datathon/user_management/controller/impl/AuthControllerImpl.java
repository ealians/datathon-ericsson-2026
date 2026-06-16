package org.elis.ericsson.datathon.user_management.controller.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.elis.ericsson.datathon.user_management.controller.AuthController;
import org.elis.ericsson.datathon.user_management.model.dto.AuthResponseDTO;
import org.elis.ericsson.datathon.user_management.model.dto.LoginDto;
import org.elis.ericsson.datathon.user_management.model.dto.TokenRefreshResponseDto;
import org.elis.ericsson.datathon.user_management.model.dto.request.SignUpRequestDto;
import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.model.entity.UserPrincipal;
import org.elis.ericsson.datathon.user_management.model.exception.RequestError;
import org.elis.ericsson.datathon.user_management.model.projection.UserMeInfo;
import org.elis.ericsson.datathon.user_management.security.CurrentUser;
import org.elis.ericsson.datathon.user_management.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.elis.ericsson.datathon.user_management.constants.Endpoints.API;
import static org.elis.ericsson.datathon.user_management.constants.Endpoints.AUTH;

@RestController
@RequestMapping(API + AUTH)
public class AuthControllerImpl implements AuthController {

    private final AuthService authService;

    public AuthControllerImpl(AuthService authService  ) {
        this.authService = authService;
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid LoginDto loginDto) {
        return authService.login(loginDto);

    }

    /**
     * Get the current user.
     *
     * @param userPrincipal the current user
     * @return the current user
     */

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserMeInfo> getCurrentUser(@CurrentUser UserPrincipal userPrincipal) {
        // Return the current user found by id.
        return authService.getCurrentUser(userPrincipal);
    }

    @Override
    public ResponseEntity<UserProfile> registerUser(SignUpRequestDto signUpRequestDto) throws RequestError, Exception {
        return authService.registerUser(signUpRequestDto);
    }

    @Override
    public ResponseEntity<Boolean> logout(HttpServletRequest request, HttpServletResponse response) {
        return authService.logout(request, response);
    }

    @Override

    @PostMapping("/refreshToken")
    public ResponseEntity<TokenRefreshResponseDto> refreshtoken(@RequestBody @Valid String refreshToken) throws Exception {
        return authService.refreshtoken(refreshToken);
    }

    @Override
    public ResponseEntity<?> getAuthenticationToChangePassword(String token) {
        return authService.getAuthenticationToChangePassword(token);
    }

    @Override
    public ResponseEntity<List<Role>> getPossibleRoles() {
        return authService.getPossibleRoles();
    }

    @Override
    public ResponseEntity<AuthResponseDTO> getSession() {
        return authService.getSession();
    }

    /**
     * Create the first user in the database.
     *
     * @return the created user
     */
    @PostMapping("/createFirstUser")
    public ResponseEntity<?> createFirstUser(HttpServletRequest req) throws Exception {
        return authService.createFirstUser(req);
    }

    @Override
    @GetMapping("/adminExists")
    public ResponseEntity<Boolean> adminExists() {
        return authService.adminExists();
    }
}


