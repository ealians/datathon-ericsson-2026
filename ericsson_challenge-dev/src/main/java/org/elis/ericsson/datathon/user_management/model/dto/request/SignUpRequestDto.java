package org.elis.ericsson.datathon.user_management.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignUpRequestDto {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "La password deve avere almeno 8 caratteri")
    private String password;

    private Boolean privacyPolicy;

    private Boolean marketingPolicy;

    private Boolean acceptedParticipationProjectIxC =false;

    private String whereMeetUs="";

    private String whereMeetUsText="";


    @Override
    public String toString() {
        return "SignUpRequestDto{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email=" + email +
                '}';
    }
}