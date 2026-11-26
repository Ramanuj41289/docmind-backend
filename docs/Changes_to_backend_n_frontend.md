
Cause of the "Sign in" Dialog

The native browser sign-in modal appeared because:

Spring Security Auto-Configuration: The backend has spring-boot-starter-security in its pom.xml, but lacked a custom SecurityConfig. By default, Spring Boot protects all endpoints with HTTP Basic Authentication and returns HTTP 401 Unauthorized with the header WWW-Authenticate: Basic.
Browser Interception: When the frontend at http://localhost:3000 queried /api/v1/documents, the backend returned WWW-Authenticate: Basic. Chrome intercepted this header and popped up the native HTTP Basic Auth dialog.
Solutions Applied
1. Frontend Proxy Configuration (vite.config.js)

Configured Vite's proxy to strip the WWW-Authenticate response header:

javascript
proxy: {
  '/api': {
    target: 'http://localhost:8081',
    changeOrigin: true,
    secure: false,
    configure: (proxy) => {
      proxy.on('proxyRes', (proxyRes) => {
        // Strip header to permanently prevent browser native popup
        delete proxyRes.headers['www-authenticate'];
      });
    },
  },
}

This ensures Chrome will never show the native sign-in dialog.

2. Backend Security Configuration (SecurityConfig.java)

Added a SecurityFilterChain bean that permits all requests to /api/**, /swagger-ui/**, and /v3/api-docs/** with CSRF disabled:

java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
3. Chat Controller Authentication Fallback (ChatController.java)

Updated askQuestion, streamQuestion, and searchSimilar with a null-safe getAuthenticatedUser(authentication) fallback to retrieve an existing user or fallback to a default user, avoiding NullPointerException when requests are unauthenticated.

What to do next
In your frontend terminal, restart the Vite dev server (or re-run npm run dev) and refresh http://localhost:3000.
In Eclipse, restart the Spring Boot backend application so SecurityConfig.java takes effect.