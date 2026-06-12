package org.elis.ericsson.datathon.user_management.model.dto.request;

import lombok.Data;

@Data
public class AuthenticateRequest {

    String Token;
    String username;
}
