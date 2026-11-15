package com.example.demo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.demo.entities.Role;

// Configuración general de seguridad: qué filtros pasan las peticiones, qué rol
// puede entrar a cada ruta, cómo se responden los errores de acceso y cómo se
// cifran las contraseñas.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        // Nombres de los roles tomados del enum Role
        private static final String CLIENT = Role.CLIENT.name();
        private static final String ADMIN = Role.ADMIN.name();
        private static final String OPERATOR = Role.OPERATOR.name();
        private static final String DELIVERY_PERSON = Role.DELIVERY_PERSON.name();

        @Autowired
        private JwtGenerator jwtGenerator;

        @Autowired
        private CustomUserDetailsService userDetailsService;

        @Autowired
        private JwtAuthEntryPoint jwtAuthEntryPoint;

        @Autowired
        private JwtAccessDeniedHandler jwtAccessDeniedHandler;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // CSRF protege a las aplicaciones que usan cookies de sesión.
                                // Aquí el usuario se identifica con el JWT en un header, así que no hace falta.
                                .csrf(AbstractHttpConfigurer::disable)
                                // Usa la configuración de CorsConfig para aceptar peticiones del front
                                .cors(Customizer.withDefaults())
                                // Permite que la consola de H2 se muestre dentro de un iframe
                                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                                // No usamos el formulario de login ni la autenticación básica de Spring
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                // El servidor no guarda sesiones: cada petición se identifica con su token
                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))
                                // Respuestas en JSON para 401 (sin sesión) y 403 (sin permiso)
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtAuthEntryPoint)
                                                .accessDeniedHandler(jwtAccessDeniedHandler))
                                // Las reglas se revisan en orden y se aplica la primera que coincide,
                                // por eso las rutas más específicas van antes que las generales
                                .authorizeHttpRequests(auth -> auth

                                                // ================= PÚBLICO =================
                                                // Peticiones previas de CORS (el navegador las envía sin token)
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/auth/login", "/auth/signup", "/auth/google",
                                                                "/auth/verify-email", "/auth/resend-verification")
                                                .permitAll()
                                                // Stripe llama al webhook sin JWT: se protege con la firma
                                                // Stripe-Signature, que se valida en PaymentService.
                                                .requestMatchers(HttpMethod.POST, "/payments/webhook").permitAll()
                                                // Página puente: el navegador del celular vuelve de Stripe
                                                // sin token y esta página reabre la app
                                                .requestMatchers(HttpMethod.GET, "/payments/app-return").permitAll()
                                                // Iniciar, consultar y cancelar pagos: solo el cliente dueño
                                                .requestMatchers("/payments/**").hasAuthority(CLIENT)
                                                // Factura abierta desde el QR: el celular no tiene sesión; la protege
                                                // la firma del enlace
                                                .requestMatchers(HttpMethod.GET, "/invoices/public/**").permitAll()
                                                // Datos del QR de mi factura: solo el cliente dueño
                                                .requestMatchers(HttpMethod.GET, "/invoices/*/link")
                                                .hasAuthority(CLIENT)
                                                // Enviar un informe por correo: solo admin
                                                .requestMatchers(HttpMethod.POST, "/reports/email").hasAuthority(ADMIN)
                                                // Suscripción Premium: consultar y pagar, solo clientes
                                                .requestMatchers("/subscriptions/**").hasAuthority(CLIENT)
                                                // Página de error de Spring, consola H2 y documentación Swagger
                                                .requestMatchers("/error", "/h2/**", "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**", "/actuator/health")
                                                .permitAll()
                                                // Páginas de Thymeleaf y sus archivos estáticos
                                                .requestMatchers(HttpMethod.GET, "/", "/home", "/menu", "/aboutUs",
                                                                "/contact",
                                                                "/productDetail/**", "/css/**", "/js/**", "/assets/**",
                                                                "/favicon.ico")
                                                .permitAll()
                                                // Dirección <-> coordenadas (Google Geocoding desde el back):
                                                // el cliente al elegir su dirección y el repartidor para
                                                // ubicar el destino en la app
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/maps/search",
                                                                "/maps/reverse")
                                                .hasAnyAuthority(CLIENT, DELIVERY_PERSON)

                                                .requestMatchers("/maps/**")
                                                .denyAll()

                                                // ================= PRODUCTOS / CARTA =================

                                                // Consultas exclusivas del administrador.
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/products/clients",
                                                                "/products/options")
                                                .hasAuthority(ADMIN)

                                                // Consultas públicas de la carta.
                                                .requestMatchers(HttpMethod.GET, "/products/**")
                                                .permitAll()

                                                .requestMatchers(HttpMethod.GET, "/categories/**")
                                                .permitAll()

                                                .requestMatchers(HttpMethod.GET, "/addons/active/category/**")
                                                .permitAll()

                                                // Las demás operaciones de estos recursos son del administrador.
                                                .requestMatchers("/products/**", "/categories/**", "/addons/**")
                                                .hasAuthority(ADMIN)

                                                // ================= ANUNCIOS DEL HOME =================
                                                // El carrusel del inicio lo ve cualquiera, incluso sin sesión
                                                .requestMatchers(HttpMethod.GET, "/announcements").permitAll()
                                                // Crear, editar o borrar anuncios: solo el admin
                                                .requestMatchers("/announcements/**").hasAuthority(ADMIN)

                                                // ================= RESEÑAS =================
                                                // Las reseñas visibles las ve cualquiera
                                                .requestMatchers(HttpMethod.GET, "/reviews").permitAll()
                                                // Mi reseña (consultar, guardar, eliminar): solo clientes
                                                .requestMatchers("/reviews/me").hasAuthority(CLIENT)
                                                // Listado completo, ocultar y eliminar: solo admin
                                                .requestMatchers("/reviews/**").hasAuthority(ADMIN)

                                                // ================= SESIÓN =================
                                                // /auth/me: cualquier usuario con un token válido
                                                .requestMatchers("/auth/**").authenticated()

                                                // ================= CLIENTE =================
                                                .requestMatchers("/clients/me/**").hasAuthority(CLIENT)
                                                .requestMatchers("/cart/**", "/checkout/**").hasAuthority(CLIENT)
                                                .requestMatchers(HttpMethod.GET, "/coupons/me/**", "/coupons/validate")
                                                .hasAuthority(CLIENT)
                                                .requestMatchers(HttpMethod.GET, "/orders/me").hasAuthority(CLIENT)
                                                .requestMatchers(HttpMethod.PUT, "/orders/*/cancel")
                                                .hasAuthority(CLIENT).requestMatchers("/notifications/**")
                                                .hasAuthority(CLIENT)

                                                // ================= OPERADOR =================
                                                .requestMatchers(HttpMethod.GET, "/operators/me").hasAuthority(OPERATOR)
                                                .requestMatchers(HttpMethod.GET, "/orders/active")
                                                .hasAuthority(OPERATOR)
                                                .requestMatchers(HttpMethod.PUT, "/orders/*/confirm", "/orders/*/cook",
                                                                "/orders/*/send/*", "/orders/*/deliver")
                                                .hasAuthority(OPERATOR)
                                                .requestMatchers(HttpMethod.GET, "/delivery-persons/available")
                                                .hasAuthority(OPERATOR)

                                                // ================= SEGUIMIENTO DE DOMICILIOS =================

                                                // Cliente: consultar sus domicilios.
                                                // El controlador comprueba que el ID corresponda al JWT.
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/delivery/client/**")
                                                .hasAuthority(CLIENT)

                                                // Repartidor: consultar los pedidos que tiene asignados.
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/delivery/person/*/active")
                                                .hasAuthority(DELIVERY_PERSON)

                                                // Repartidor: actualizar su ubicación en un pedido asignado.
                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/delivery/person/*/orders/*/location")
                                                .hasAuthority(DELIVERY_PERSON)

                                                // Repartidor: completar un pedido asignado mediante el PIN.
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/delivery/person/*/orders/*/complete")
                                                .hasAuthority(DELIVERY_PERSON)

                                                // Cualquier otra ruta o método de delivery queda bloqueado
                                                // hasta que tenga una autorización explícita.
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/delivery/person/*/orders/*/start")
                                                .hasAuthority(DELIVERY_PERSON)

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/delivery/operator/active")
                                                .hasAuthority(OPERATOR)
                                                .requestMatchers("/delivery/**")
                                                .denyAll()

                                                // ================= PEDIDOS =================
                                                // Los listados completos son del admin. Van antes del detalle
                                                // porque "/orders/*" también coincide con "/orders/all".
                                                .requestMatchers(HttpMethod.GET, "/orders", "/orders/all")
                                                .hasAuthority(ADMIN)
                                                // Detalle de un pedido: operador y admin
                                                .requestMatchers(HttpMethod.GET, "/orders/*")
                                                .hasAnyAuthority(OPERATOR, ADMIN)

                                                // ================= ADMIN =================
                                                .requestMatchers("/clients/**", "/orders/**", "/coupons/**",
                                                                "/operators/**",
                                                                "/delivery-persons/**", "/sync/**")
                                                .hasAuthority(ADMIN)

                                                // Cualquier otra ruta exige al menos haber iniciado sesión
                                                .anyRequest().authenticated())
                                // El filtro JWT se ejecuta antes del filtro de usuario y contraseña de Spring
                                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        private JwtAuthenticationFilter jwtAuthenticationFilter() {
                return new JwtAuthenticationFilter(jwtGenerator, userDetailsService);
        }

        // Valida correo y contraseña usando CustomUserDetailsService y el
        // PasswordEncoder.
        // Si los datos son correctos, devuelve la autenticación con los roles del
        // usuario.
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
                        throws Exception {
                return configuration.getAuthenticationManager();
        }

        // Cifra las contraseñas con BCrypt
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
