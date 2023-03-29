package fr.miage.orleans.tokens.config;


import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import fr.miage.orleans.tokens.facades.Facade;
import fr.miage.orleans.tokens.facades.Personne;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

@Configuration
@EnableGlobalMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true)

public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/register").permitAll()
                        .requestMatchers("/api/login").permitAll()
                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()));
        return http.build();
    }

    @Bean
    UserDetailsService users(Facade facadeJoueur, PasswordEncoder passwordEncoder) {
        return new CostumUserDetailsService(passwordEncoder,facadeJoueur);
    }


    private static final String SECRET_KEY = "MySuperSecretKeyForJWTsguguofterdfyulioluktydfyiuofdtrsdfuliguydsrtfuftrsesj";

    @Bean
    public JwtEncoder jwtEncoder() {
        // Créer une clé secrète avec SecretKeySpec
        Key key = new SecretKeySpec(SECRET_KEY.getBytes(), "HMACSHA256");
        // Créer un objet OctetSequenceKey pour la clé secrète
        JWK jwk = new OctetSequenceKey.Builder(key.getEncoded())
                .algorithm(JWSAlgorithm.HS256)
                .build();
        // Créer un objet JWKSet avec la clé secrète
        JWKSet jwkSet = new JWKSet(jwk);
        // Créer un JWKSource avec la JWKSet
        JWKSource<SecurityContext> jwkSource = (jwkSelector, context) -> jwkSet.getKeys();

        NimbusJwtEncoder nimbusJwtEncoder = new NimbusJwtEncoder(jwkSource);

        return nimbusJwtEncoder;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Créer une clé secrète avec SecretKeySpec
        Key key = new SecretKeySpec(SECRET_KEY.getBytes(), "HMACSHA256");
        // Créer un objet OctetSequenceKey pour la clé secrète
        JWK jwk = new OctetSequenceKey.Builder(key.getEncoded())
                .algorithm(JWSAlgorithm.HS256)
                .build();
        // Créer un objet JWKSet avec la clé secrète
        JWKSet jwkSet = new JWKSet(jwk);
        // Créer un JWKSource avec la JWKSet
        JWKSource<SimpleSecurityContext> jwkSource = (jwkSelector, context) -> jwkSet.getKeys();

        return NimbusJwtDecoder.withSecretKey(jwk.toOctetSequenceKey().toSecretKey()).build();
    }


    @Bean
    public PasswordEncoder delegatingPasswordEncoder() {
        String idForEncode = "bcrypt";;
        PasswordEncoder defaultEncoder = new BCryptPasswordEncoder();
        Map<String, PasswordEncoder> encoders = Map.of(
                idForEncode, defaultEncoder,
                "noop", NoOpPasswordEncoder.getInstance(),
                "scrypt", SCryptPasswordEncoder.defaultsForSpringSecurity_v4_1(),
                "sha256", new StandardPasswordEncoder()
        );

        return new DelegatingPasswordEncoder(idForEncode, encoders);
    }

    /*    @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    */
    @Bean
    Function<Personne,String> genereTokenFunction() {

        return joueur -> {

            Instant now = Instant.now();
            long expiry = 36000L;

            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer("self")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(expiry))
                    .subject(joueur.email())
                    .claim("scope", "")
                    .build();



            JWSHeader.Builder b = new JWSHeader.Builder(JWSAlgorithm.ES256);

            JwsHeader myJwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();


            return jwtEncoder().encode(JwtEncoderParameters.from(myJwsHeader,claims)).getTokenValue();
        };
    }



}