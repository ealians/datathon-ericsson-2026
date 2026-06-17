package org.elis.ericsson.datathon.user_management.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elis.ericsson.datathon.user_management.model.entity.Role;

import java.util.Collection;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateFirstUserResponseDto {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Collection<Role> roles;
}
