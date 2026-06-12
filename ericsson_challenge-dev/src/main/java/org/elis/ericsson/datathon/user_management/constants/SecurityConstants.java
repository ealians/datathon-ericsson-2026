package org.elis.ericsson.datathon.user_management.constants;

import lombok.Data;

@Data
public class SecurityConstants {

    private SecurityConstants() {
    }
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String JWT_SECRET = "jAYg8t4u9y$B&E)O@pcQrmhWdau5M8w?z%CUF-JempRgUkXP3llu8x/L?o(G+DbU";
    public static final String TOKEN_TYPE = "JWT";
    public static final String TOKEN_ISSUER = "secure-api";
    public static final String TOKEN_AUDIENCE = "secure-app";
    public static final int TOKEN_EXPIRATION= 30*60*1000*10;
    public static final int REFRESH_TOKEN_EXPIRATION= 60*60*1000*12;
    public static final int MAX_VOTES_PER_USER_ON_SAME_IDEA = 5;

}
