package greencity.security.service.impl;

import static greencity.constant.ErrorMessage.BAD_GOOGLE_TOKEN;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import greencity.entity.User;
import greencity.entity.enums.ROLE;
import greencity.entity.enums.UserStatus;
import greencity.security.dto.SuccessSignInDto;
import greencity.security.jwt.JwtTokenTool;
import greencity.security.service.GoogleSecurityService;
import greencity.service.UserService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@inheritDoc}
 */
@Service
@Slf4j
public class GoogleSecurityServiceImpl implements GoogleSecurityService {
    private UserService userService;
    private GoogleIdTokenVerifier verifier;
    private JwtTokenTool tokenTool;

    /**
     * Constructor.
     *
     * @param userService {@link UserService} - service of {@link User} logic.
     * @param tokenTool   {@link JwtTokenTool} - tool for jwt logic.
     * @param clientId    {@link String} - google client id.
     */
    public GoogleSecurityServiceImpl(UserService userService,
                                     JwtTokenTool tokenTool,
                                     @Value("${google.clientId}") String clientId
    ) {
        this.userService = userService;
        this.tokenTool = tokenTool;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
            .setAudience(
                Collections.singletonList(clientId)
            )
            .build();
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public SuccessSignInDto authenticate(String idToken) {
        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken != null) {
                GoogleIdToken.Payload payload = googleIdToken.getPayload();
                String email = payload.getEmail();
                String familyName = (String) payload.get("family_name");
                String givenName = (String) payload.get("given_name");
                User byEmail = userService.findByEmail(email);
                if (byEmail != null) {
                    String accessToken = tokenTool.createAccessToken(byEmail.getEmail(), byEmail.getRole());
                    String refreshToken = tokenTool.createRefreshToken(byEmail.getEmail());
                    log.info("Google sign-in exist user - {}", byEmail.getEmail());
                    return new SuccessSignInDto(accessToken, refreshToken, byEmail.getFirstName());
                } else {
                    User user = User.builder()
                        .email(email)
                        .lastName(familyName)
                        .firstName(givenName)
                        .role(ROLE.ROLE_USER)
                        .dateOfRegistration(LocalDateTime.now())
                        .lastVisit(LocalDateTime.now())
                        .userStatus(UserStatus.ACTIVATED)
                        .build();
                    userService.save(user);
                    String accessToken = tokenTool.createAccessToken(user.getEmail(), user.getRole());
                    String refreshToken = tokenTool.createRefreshToken(user.getEmail());
                    log.info("Google sign-up and sign-in user - {}", user.getEmail());
                    return new SuccessSignInDto(accessToken, refreshToken, user.getFirstName());
                }
            }
            throw new IllegalArgumentException(BAD_GOOGLE_TOKEN);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException(BAD_GOOGLE_TOKEN + ". " + e.getMessage());
        }
    }
}
