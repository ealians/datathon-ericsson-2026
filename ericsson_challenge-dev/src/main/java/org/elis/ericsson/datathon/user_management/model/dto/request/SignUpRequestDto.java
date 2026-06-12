package org.elis.ericsson.datathon.user_management.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
                ", password='" + password + '\'' +
                '}';
    }
}