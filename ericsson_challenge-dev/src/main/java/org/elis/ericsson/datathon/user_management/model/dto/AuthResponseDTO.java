package org.elis.ericsson.datathon.user_management.model.dto;

import lombok.*;
import org.elis.ericsson.datathon.user_management.model.entity.Role;

import java.util.Collection;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Data
public class AuthResponseDTO {

    private Long id;
    private String email;
    private String name;
    private Collection<Role> role;
    private String token;
    private String refreshToken;
    private int duration;

    @Override
    public String toString() {
        return "AuthResponseDTO{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", role=" + role +
                ", token='" + token + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", duration=" + duration +
                '}';
    }
}
