package org.elis.ericsson.datathon.user_management.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.elis.ericsson.datathon.user_management.constants.SecurityConstants;
import org.elis.ericsson.datathon.user_management.model.dto.AuthResponseDTO;
import org.elis.ericsson.datathon.user_management.model.entity.RefreshToken;
import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public record JwtUtility(RefreshTokenRepository refreshTokenRepository) {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtility.class);

    @Autowired
    public JwtUtility {
    }

    public AuthResponseDTO generateAuthFromUser(UserProfile user) {
        // Get token and refresh token.
        String token = generateTokenFromUser(user);
        RefreshToken refreshToken = createRefreshToken(user);
        try{
            // Prepare repsonse to send to FE with username, authorities and duration of the token.
            return AuthResponseDTO.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getFirstName() + " " + user.getLastName())
                    .role(user.getRoles())
                    .token(token)
                    .refreshToken(refreshToken.getToken())
                    .duration(SecurityConstants.TOKEN_EXPIRATION)
                    .build();
        }catch ( Exception e){
            logger.error("Error while generating auth response: " + e.getMessage());
        }
        return null;
    }
    /**
     * Generate token from the given user.
     * @param user The user.
     * @return the token.
     */
    public String generateTokenFromUser(UserProfile user) {
        // Get the user roles
        List<String> roles = new ArrayList<>();
        for(Role r : user.getRoles()){
            roles.add(r.getName());
        }

        // Create the token.
        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, Keys.hmacShaKeyFor(SecurityConstants.JWT_SECRET.getBytes()))
                .setHeaderParam("typ", "JWT")
                .setIssuedAt(new Date())
                .setAudience("secure-app")
                .setSubject(Long.toString(user.getId()))
                .setExpiration(new Date(System.currentTimeMillis() + SecurityConstants.TOKEN_EXPIRATION))
                .claim("rol", roles)
                .compact();
    }
    /**
     * Get the user id from the token.
     * @param token The token.
     * @return The user id.
     */
    public Long getUserIdFromToken(String token) {

        Claims claims = Jwts.parser()
                .setSigningKey(SecurityConstants.JWT_SECRET.getBytes())
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }
    /**
     * Check if the given refresh token is valid or expired.
     *
     * @param token the refresh token to be checked.
     */
    public void verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new BadCredentialsException("Refresh token was expired. Please make a new sign in request");
        }

    }
    public String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Create a new refresh token for the given user.
     * @param user the user for which the refresh token will be created.
     * @return the created refresh token.
     */
    public RefreshToken createRefreshToken(UserProfile user) {
        // Cerca un token di aggiornamento esistente per l'utente
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(user.getId())
                .orElse(new RefreshToken()); // Usa un nuovo token solo se non esiste già

        // Imposta i dettagli del token
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(SecurityConstants.REFRESH_TOKEN_EXPIRATION));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    public boolean validateToken(String token) {
        try {
            byte[] signingKey = SecurityConstants.JWT_SECRET.getBytes();

            Jwts.parser().setSigningKey(signingKey).parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }
}
