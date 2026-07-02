package com.springrag.security.oauth;

import com.springrag.security.service.TokenService;
import com.springrag.user.domain.User;
import com.springrag.user.domain.UserRole;
import com.springrag.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * After a successful Google/GitHub login, map the social identity onto our {@link User} (find-or-create
 * by email, account-link if the email already exists), mint the SAME internal JWT password users get,
 * and redirect the SPA to {@code /?token=<jwt>}. The SPA reads the token, stores the session, and from
 * then on every flow (ownership, roles, interviews, coaching, reports, profile) is identical to a
 * password user — there is one authorization model, not two.
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final UserRepository userRepository;
    private final TokenService tokenService;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId();   // "google" | "github"
        OAuth2User principal = token.getPrincipal();

        String email = extractEmail(principal, provider);
        String name = extractName(principal, email);
        User user = resolveUser(email, name, provider);

        String jwt = tokenService.issueToken(user);
        log.info("OAuth2 login success. provider={}, userId={}", provider, user.getId());

        String target = "/?token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, target);
    }

    /** Find an account by email (account-linking) or create a new candidate. Never downgrades role. */
    private User resolveUser(String email, String name, String provider) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setUsername(name);
            u.setRole(UserRole.USER);
            u.setAuthProvider(provider.toUpperCase());
            return userRepository.save(u);
        });
    }

    private String extractEmail(OAuth2User p, String provider) {
        Object email = p.getAttribute("email");
        if (email != null && !email.toString().isBlank()) return email.toString();
        // GitHub may hide the email — derive a stable handle so repeat logins map to the same account.
        Object login = p.getAttribute("login");
        Object id = p.getAttribute("id");
        return (login != null ? login : id) + "@" + provider + ".oauth";
    }

    private String extractName(OAuth2User p, String email) {
        Object name = p.getAttribute("name");
        if (name != null && !name.toString().isBlank()) return name.toString();
        Object login = p.getAttribute("login");
        return login != null ? login.toString() : email.split("@")[0];
    }
}
