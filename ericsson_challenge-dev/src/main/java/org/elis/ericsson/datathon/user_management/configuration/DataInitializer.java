package org.elis.ericsson.datathon.user_management.configuration;

import org.elis.ericsson.datathon.user_management.model.dto.request.CreateFirstUserRequestDto;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.elis.ericsson.datathon.user_management.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.data-initializer.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final AuthService authService;
    private final UserProfileRepository userProfileRepository;

    public DataInitializer(AuthService authService, UserProfileRepository userProfileRepository) {
        this.authService = authService;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public void run(String... args) {
        if (userProfileRepository.count() > 0) {
            logger.info("First user already present, skipping initialization");
            return;
        }

        try {
            CreateFirstUserRequestDto requestDto = new CreateFirstUserRequestDto();
            requestDto.setEmail("admin@elis.org");
            requestDto.setPassword("password");
            requestDto.setFirstName("firstName_admin");
            requestDto.setLastName("lastName_admin");
            authService.createFirstUser(requestDto);
            logger.info("Default admin user created successfully");
        } catch (Exception e) {
            logger.error("Error creating default admin user: {}", e.getMessage());
        }
    }
}
