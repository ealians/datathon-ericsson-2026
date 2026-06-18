package org.elis.ericsson.datathon.user_management.constants;

import lombok.Data;

@Data
public class SecurityConstants {

    private SecurityConstants() {
    }
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String JWT_SECRET = System.getenv("JWT_SECRET") != null
            ? System.getenv("JWT_SECRET")
            : "dev-only-fallback-key-do-not-use-in-production-must-be-at-least-64-bytes-long-for-hs512!";
    public static final String TOKEN_TYPE = "JWT";
    public static final String TOKEN_ISSUER = "secure-api";
    public static final String TOKEN_AUDIENCE = "secure-app";
    public static final int TOKEN_EXPIRATION= 15*60*1000;
    public static final int REFRESH_TOKEN_EXPIRATION= 60*60*1000*12;
    public static final int MAX_VOTES_PER_USER_ON_SAME_IDEA = 5;

}
