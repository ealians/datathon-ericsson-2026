package org.elis.ericsson.datathon.user_management.controller;

import org.elis.ericsson.datathon.user_management.model.exception.ItemNotFoundException;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import static org.elis.ericsson.datathon.user_management.constants.Endpoints.API;
import static org.elis.ericsson.datathon.user_management.constants.Endpoints.PROFILE;


@RestController
@RequestMapping(API + PROFILE)
public interface UserProfileController {

    // Lista di profili
    @GetMapping("")
    ResponseEntity<List<UserProfile>> getAllProfiles();

    // Elimina un profilo utente
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteProfile(@PathVariable Long id) throws ItemNotFoundException;

}
