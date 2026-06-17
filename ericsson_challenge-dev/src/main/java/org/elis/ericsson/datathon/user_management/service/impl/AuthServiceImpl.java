package org.elis.ericsson.datathon.user_management.service.impl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.elis.ericsson.datathon.user_management.model.dto.AuthResponseDTO;
import org.elis.ericsson.datathon.user_management.model.dto.LoginDto;
import org.elis.ericsson.datathon.user_management.model.dto.TokenRefreshResponseDto;
import org.elis.ericsson.datathon.user_management.model.dto.request.CreateFirstUserRequestDto;
import org.elis.ericsson.datathon.user_management.model.dto.request.SignUpRequestDto;
import org.elis.ericsson.datathon.user_management.model.dto.response.CreateFirstUserResponseDto;
import org.elis.ericsson.datathon.user_management.model.entity.*;
import org.elis.ericsson.datathon.user_management.model.exception.ExpiredJwtException;
import org.elis.ericsson.datathon.user_management.model.exception.InvalidCredentialsException;
import org.elis.ericsson.datathon.user_management.model.exception.ItemNotFoundException;
import org.elis.ericsson.datathon.user_management.model.exception.RequestError;
import org.elis.ericsson.datathon.user_management.model.projection.UserMeInfo;
import org.elis.ericsson.datathon.user_management.repository.*;
import org.elis.ericsson.datathon.user_management.security.CustomAuthenticationManager;
import org.elis.ericsson.datathon.user_management.security.JwtUtility;
import org.elis.ericsson.datathon.user_management.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

import static org.elis.ericsson.datathon.user_management.constants.ExceptionMessages.*;


@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private final UserProfileRepository userProfileRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtUtility tokenProvider;
    private final JwtUtility jwtUtility;

    private final CustomAuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public AuthServiceImpl(
            JwtUtility jwtUtility,
            CustomAuthenticationManager authenticationManager,
            RefreshTokenRepository refreshTokenRepository,
            RoleRepository roleRepository, UserProfileRepository userProfileRepository, PasswordResetTokenRepository passwordResetTokenRepository, JwtUtility tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtUtility = jwtUtility;
        this.refreshTokenRepository = refreshTokenRepository;
        this.roleRepository = roleRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public ResponseEntity<AuthResponseDTO> login(LoginDto loginDto) {
        try {
            logger.debug("Enter into AuthService.login : Parameters : {}", loginDto);
            UserProfile user;
            UsernamePasswordAuthenticationToken authenticationToken;

            user = userProfileRepository.findByEmail(loginDto.getEmail()).orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));

            authenticationToken = new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword());

            Authentication authentication;

            try {
                authentication = authenticationManager.authenticate(authenticationToken);
            } catch (Exception e) {
                throw new InvalidCredentialsException(INVALID_CREDENTIALS);
            }

            if (authentication.isAuthenticated()) {
                // Set the authentication in the Security Context.
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.info("User {} successfully logged in", loginDto.getEmail());
                // Generate the token and return the wrapped response.
                return ResponseEntity.ok(tokenProvider.generateAuthFromUser(user));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            logger.warn("Error in AuthServiceImpl.login " + loginDto.getEmail() + " " + e.getMessage());
            throw e;
        }
    }

    @Override
    public ResponseEntity<UserProfile> registerUser(SignUpRequestDto signUpRequestDto) throws RequestError {
        try {
            logger.debug("Enter into AuthService.registerUser : Parameters : {}", signUpRequestDto);
            // Check if the email is already in use.
            if (userProfileRepository.existsByEmail(signUpRequestDto.getEmail()))
                throw new RequestError("Email " + signUpRequestDto.getEmail() + " is already in use!");

            if (!signUpRequestDto.getPrivacyPolicy()) {
                throw new RequestError("Privacy Policy must be accepted!");
            }

            // Trying to find the role with the given name, if not found a bad request exception will be thrown.
            Role role = roleRepository.findByName("ROLE_USER").orElseThrow(() -> new ItemNotFoundException(ROLE_NOT_FOUND));
            // Create the collection to store all the roles.
            ArrayList<Role> roles = new ArrayList<>();
            roles.add(role);

            // Creating user's account
            UserProfile user = new UserProfile();
            user.setFirstName(signUpRequestDto.getFirstName());
            user.setLastName(signUpRequestDto.getLastName());
            user.setEmail(signUpRequestDto.getEmail().toLowerCase());
            user.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));
            user.setRoles(roles);

            // Save the user in the database.
            UserProfile result = userProfileRepository.save(user);
            result.setPassword(null);

            // Return the created user.
            return ResponseEntity.ok(result);
        } catch (RequestError e) {
            logger.error("Error in AuthServiceImpl.registerUser " + e.getMessage());
            throw (e);
        }
    }


    @Override
    public ResponseEntity<TokenRefreshResponseDto> refreshtoken(String refreshToken) throws Exception {

        try {
            logger.debug("Enter into AuthService.refreshtoken : Parameters : {}", refreshToken);

            // Find the refresh token in the database.
            RefreshToken refreshTokenDb = refreshTokenRepository.findByToken(refreshToken).orElseThrow(() -> new Exception("Refresh Token non trovato"));

            // Check if the refresh token is expired.
            jwtUtility.verifyExpiration(refreshTokenDb);

            // Get the user from the refresh token.
            UserProfile userLogged = refreshTokenDb.getUser();

            //Generate new Token
            String newToken = jwtUtility.generateTokenFromUser(userLogged);

            // Generate new refresh token.
            RefreshToken newRefreshToken = jwtUtility.createRefreshToken(userLogged);
            TokenRefreshResponseDto tokenRefreshResponse = new TokenRefreshResponseDto(newToken, newRefreshToken.getToken());


            // Return refresh response.
            return ResponseEntity.ok(tokenRefreshResponse);
        } catch (Exception e) {
            logger.error("Error in AuthServiceImpl.refreshtoken " + e.getMessage());
            throw e;
        } finally {
            logger.debug("Exit from AuthService.refreshtoken : Parameters : {}", refreshToken);
        }
    }

    @Override
    public ResponseEntity<CreateFirstUserResponseDto> createFirstUser(CreateFirstUserRequestDto requestDto) throws Exception {
        try {
            logger.debug("Enter into AuthService.createFirstUser");
            // Check if the first user is already present.
            if (userProfileRepository.count() > 0)
                throw new Exception("First user already present!");
            // Get ADMIN and USER role.
            Optional<Role> ruoloAdmin = roleRepository.findByName("ROLE_ADMIN");
            Optional<Role> ruoloUser = roleRepository.findByName("ROLE_USER");
            ArrayList<Role> roles = new ArrayList<>();

            // If the roles are not present, create them.
            if (ruoloAdmin.isEmpty()) {
                Role role = new Role();
                role.setName("ROLE_ADMIN");
                roles.add(roleRepository.save(role));
            } else {
                roles.add(ruoloAdmin.get());
            }
            if (ruoloUser.isEmpty()) {
                Role role = new Role();
                role.setName("ROLE_USER");
                roles.add(roleRepository.save(role));
            } else {
                roles.add(ruoloUser.get());
            }

            UserProfile user = new UserProfile();
            user.setEmail(requestDto.getEmail().toLowerCase());
            user.setFirstName(requestDto.getFirstName());
            user.setLastName(requestDto.getLastName());
            user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
            user.setRoles(roles);
            // Save the user in the database.
            try {
                userProfileRepository.save(user);
            } catch (Exception e) {
                throw new InvalidCredentialsException("User already present!");
            }

            CreateFirstUserResponseDto responseDto = CreateFirstUserResponseDto.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .roles(user.getRoles())
                    .build();

            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            logger.error("Error in AuthServiceImpl.createFirstUser" + e.getMessage());
            throw e;
        }
    }

    @Override
    public ResponseEntity<UserMeInfo> getCurrentUser(UserPrincipal userPrincipal) {
        try {
            logger.debug("Enter into AuthService.getCurrentUser : Parameters : {}", userPrincipal);
            if (userPrincipal == null)
                //return 401
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            UserMeInfo user = userProfileRepository.getUserProjectedMeById(userPrincipal.getId())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with id : " + userPrincipal.getId()));

            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Error in AuthServiceImpl.getCurrentUser " + e.getMessage());
            throw e;
        }
    }

    @Override
    public ResponseEntity<?> getAuthenticationToChangePassword(String token) {
        try {

            logger.debug("Enter into AuthService.getAuthenticationToChangePassword : Parameters : {}", token);
            // Find the password reset token using the given token.
            Optional<PasswordResetToken> userPasswToken = passwordResetTokenRepository.findByToken(token);

            // Check if the token is present.
            if (userPasswToken.isEmpty()) {
                throw new ExpiredJwtException("Token non valido");
            }

            // Retrieve the user from the token.
            UserProfile user = userPasswToken.get().getUser();

            // Request the token to change the password.

            String result = validatePasswordResetToken(token);
            if (result != null) {
                logger.error("Error in AuthServiceImpl.getAuthenticationToChangePassword" + result);
                throw new ExpiredJwtException("Token non valido");
            } else {
                return ResponseEntity.ok(tokenProvider.generateAuthFromUser(user));
            }
        } catch (Exception e) {
            logger.error("Error in AuthServiceImpl.getAuthenticationToChangePassword " + e.getMessage());
            throw e;
        }
    }

    @Override
    public ResponseEntity<Boolean> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            logger.debug("Enter into AuthService.logout");
            // Get the Spring Authentication object of the current request.
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // In case you are not filtering the users of this request url.
            if (authentication != null) {
                new SecurityContextLogoutHandler().logout(request, response, authentication); // <= This is the call you are looking for.
            }
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            logger.error("Error in AuthServiceImpl.logout " + e.getMessage());
            throw e;
        }
    }

    @Override
    public ResponseEntity<List<Role>> getPossibleRoles() {

        try {
            logger.debug("Enter into AuthService.getPossibleRoles");
            List<Role> roles = roleRepository.findAll();
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            logger.error("Error in AuthServiceImpl.getPossibleRoles " + e.getMessage());
            throw e;
        }
    }

    @Override
    public ResponseEntity<AuthResponseDTO> getSession() {
        try {
            logger.debug("Enter into AuthService.getSession");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                UserProfile user = userProfileRepository.findById(userPrincipal.getId()).orElseThrow(() -> new UsernameNotFoundException("User con id : " + userPrincipal.getId() + " non trovato"));
                return ResponseEntity.ok(tokenProvider.generateAuthFromUser(user));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            logger.error("Error in AuthServiceImpl.getSession " + e.getMessage());
            throw e;
        }

    }


    /*
      Validate the password reset token.
      @param token The token to validate.
     * @return invalidToken if the token is invalid, expired if the token is expired
     */

    /**
     * Check if the token is expired.
     *
     * @param passToken The token to check.
     * @return True if the token is expired.
     */
    private boolean isTokenExpired(PasswordResetToken passToken) {
        return passToken.getExpiryDate().compareTo(Instant.now()) <= 0;
    }

    public String validatePasswordResetToken(String token) {
        try {
            logger.debug("Enter into AuthService.validatePasswordResetToken : Parameters : {}", token);
            Optional<PasswordResetToken> userPasswToken = passwordResetTokenRepository.findByToken(token);
            if (userPasswToken.isEmpty()) {
                throw new ExpiredJwtException("Token non valido");
            }
            final PasswordResetToken passToken = userPasswToken.get();

            return isTokenExpired(passToken) ? "expired"
                    : null;
        } catch (Exception e) {
            logger.error("Error in AuthServiceImpl.validatePasswordResetToken " + e.getMessage());
            throw e;
        }
    }
}
