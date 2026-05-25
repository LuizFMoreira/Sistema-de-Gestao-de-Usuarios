package com.luizfernando.gestaousuarios.core;

import com.luizfernando.gestaousuarios.api.security.SecurityFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ✅ BOM: Centraliza toda a configuração de segurança em um lugar
 * Fácil visualizar e modificar política de segurança
 */
@Configuration
public class SecurityConfig {

    private final SecurityFilter securityFilter;

    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    /**
     * ✅ BOM: SecurityFilterChain bem estruturado
     * CORS → CSRF → Autorização → Filtros customizados
     *
     * ⚠️ PROBLEMA: CORS e filtro de segurança estão hardcoded
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                /**
                 * ❌ DOCUMENTAÇÃO FALTANDO: Por que CSRF foi desativado?
                 *
                 * 💡 SUGESTÃO: Adicionar comentário explicando a decisão
                 * Está certo desativar CSRF com JWT, mas deve ser documentado.
                 *
                 * REFATORAÇÃO SUGERIDA:
                 * /**
                 *  * CSRF desabilitado pois a aplicação usa JWT para autenticação.
                 *  * JWT tokens são imunes a CSRF porque:
                 *  * 1. Devem ser enviados no header Authorization (não em cookies)
                 *  * 2. Header Authorization não é enviado automaticamente em requisições cross-origin
                 *  * 3. O cliente (React) controla explicitamente o envio do token
                 *  *
                 *  * Segurança adicional implementada:
                 *  * - Token expira em 2 horas
                 *  * - Rate limiting recomendado no proxy/load balancer
                 *  * - HTTPS obrigatório em produção
                 *  * - SameSite attribute em cookies (se usados)
                 *  * /
                 */
                .csrf(csrf -> csrf.disable()) // JWT é imune a CSRF
                .authorizeHttpRequests(auth -> auth
                        /**
                         * ✅ BOM: OPTIONS liberado para preflight CORS funcionar
                         * Necessário para requisições CORS do navegador
                         */
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        /**
                         * ✅ BOM: Rotas públicas bem definidas
                         * Apenas login e cadastro são públicos, resto requer token
                         */
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        /**
                         * ✅ BOM: Tudo mais requer autenticação
                         * Segurança por padrão
                         */
                        .anyRequest().authenticated()
                )
                /**
                 * ✅ BOM: SecurityFilter roda ANTES do filtro padrão
                 * Permite validar JWT logo no início, evitando processamento desnecessário
                 */
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * ⚠️ PROBLEMA: CORS hardcoded com origem específica
     *
     * 💡 SUGESTÃO: Externalizar para application.properties
     * Problemas atuais:
     * 1. Requer recompilação para mudar origem
     * 2. Diferentes valores para dev/prod
     * 3. Não segue 12-factor app
     * 4. Em produção, frontend terá URL diferente
     *
     * REFATORAÇÃO SUGERIDA:
     * Em application.properties (NUNCA commitar):
     * app.cors.allowed-origins=http://localhost:5173,https://app.example.com
     * app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
     * app.cors.max-age=3600
     *
     * No SecurityConfig:
     * @Value("${app.cors.allowed-origins}")
     * private String allowedOrigins;
     *
     * @Value("${app.cors.max-age:3600}")
     * private long corsMaxAge;
     *
     * @Bean
     * public CorsConfigurationSource corsConfigurationSource() {
     *     CorsConfiguration configuration = new CorsConfiguration();
     *     configuration.setAllowedOrigins(
     *         Arrays.asList(allowedOrigins.split(","))
     *     );
     *     configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
     *     configuration.setAllowedHeaders(List.of("*"));
     *     configuration.setMaxAge(corsMaxAge);
     *
     *     UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
     *     source.registerCorsConfiguration("/**", configuration);
     *     return source;
     * }
     *
     * BENEFÍCIOS:
     * - Fácil configurar diferentes ambientes
     * - Sem recompilação necessária
     * - Segurança em produção (URL real do app)
     * - 12-factor app compliant
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        /**
         * ⚠️ HARDCODED: localhost:5173 em produção não funciona
         * Em produção, o frontend estará em outra URL
         */
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));

        /**
         * ✅ BOM: Métodos HTTP bem definidos
         * Inclui OPTIONS para preflight CORS
         */
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        /**
         * ⚠️ SEGURANÇA: Permite todos os headers (*)
         *
         * 💡 SUGESTÃO: Restringir a headers específicos
         * Atualmente qualquer header é aceito, incluindo potencialmente maliciosos.
         *
         * REFATORAÇÃO SUGERIDA:
         * configuration.setAllowedHeaders(List.of(
         *     "Content-Type",
         *     "Authorization",
         *     "X-Requested-With"
         * ));
         *
         * BENEFÍCIO:
         * - Mais seguro (whitelist ao invés de blacklist)
         * - Mais específico
         * - Melhor performance
         */
        configuration.setAllowedHeaders(List.of("*"));

        /**
         * 💡 SUGESTÃO: Adicionar maxAge para cache de preflight
         * Reduz requisições OPTIONS repetidas
         */
        configuration.setMaxAge(3600L); // 1 hora

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * ✅ BOM: BCryptPasswordEncoder com força padrão
     * BCrypt é industry standard para senha
     *
     * 💡 SUGESTÃO: Considerar aumentar força em produção
     * BCrypt padrão usa strength 10, que é bom mas lento.
     * Para aplicações críticas, usar strength 12+
     *
     * REFATORAÇÃO:
     * @Bean
     * public PasswordEncoder passwordEncoder() {
     *     int strength = environment.getProperty("app.bcrypt.strength", Integer.class, 10);
     *     return new BCryptPasswordEncoder(strength);
     * }
     *
     * application.properties:
     * app.bcrypt.strength=12  // Para produção
     *
     * BENEFÍCIO:
     * - Mais seguro contra força bruta
     * - Configurável por ambiente
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}