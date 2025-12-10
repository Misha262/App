package com.app.controller;

import com.app.model.User;
import com.app.security.JwtUtil;
import com.app.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
public class OAuthController {

    private final AuthService authService = new AuthService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping("/oauth2/success")
    public ResponseEntity<String> oauthSuccess(OAuth2AuthenticationToken authentication) throws Exception {
        if (authentication == null || !(authentication.getPrincipal() instanceof DefaultOAuth2User principal)) {
            return ResponseEntity.status(401).body("OAuth2 authentication missing");
        }

        Map<String, Object> attrs = principal.getAttributes();
        String email = (String) attrs.getOrDefault("email", "");
        String name = (String) attrs.getOrDefault("name", email);

        User user = authService.loginOrRegisterOAuth(name, email);
        String token = JwtUtil.generateToken(user.getUserId(), user.getEmail(), 60L * 60L * 24L * 7L);

        String json = mapper.writeValueAsString(Map.of("token", token, "user", user));
        String safeJson = new String(json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        String html = """
                <!doctype html>
                <html><body>
                <script>
                const data = %s;
                localStorage.setItem('token', data.token);
                localStorage.setItem('user', JSON.stringify(data.user));
                window.location.href = '%s';
                </script>
                </body></html>
                """.formatted(safeJson, frontendUrl);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
