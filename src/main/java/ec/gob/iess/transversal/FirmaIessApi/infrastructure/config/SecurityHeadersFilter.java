/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * <b>Filtro transversal que agrega cabeceras de seguridad HTTP a todas las
 * respuestas del servicio.</b>
 *
 * <p>Mitiga las vulnerabilidades detectadas en el analisis de seguridad:</p>
 * <ul>
 *   <li><b>Content-Security-Policy</b>  — proteccion contra XSS e inyeccion.</li>
 *   <li><b>X-Frame-Options</b>          — prevencion de clickjacking.</li>
 *   <li><b>X-Content-Type-Options</b>   — evita MIME sniffing del navegador.</li>
 *   <li><b>Strict-Transport-Security</b>— fuerza HTTPS (actua en produccion via F5).</li>
 *   <li><b>Referrer-Policy</b>          — control de informacion enviada en cabecera Referer.</li>
 *   <li><b>Permissions-Policy</b>       — desactiva APIs de navegador no requeridas.</li>
 * </ul>
 *
 * <p>Nota: HSTS es ignorado por los navegadores cuando el transporte es HTTP;
 * en produccion el F5 termina TLS y el header queda activo hacia el cliente.</p>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 *          [Author: Juan Carlos Estevez Hidalgo, Date: 24 jul 2026]
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    /**
     * Politica CSP adaptada para una API REST con Swagger UI habilitado.
     * <ul>
     *   <li>{@code default-src 'self'}          — solo recursos del mismo origen por defecto.</li>
     *   <li>{@code script-src 'unsafe-inline' 'unsafe-eval'} — requerido por Swagger UI.</li>
     *   <li>{@code style-src  'unsafe-inline'}  — requerido por Swagger UI.</li>
     *   <li>{@code img-src data: https:}        — iconos base64 y logos externos de Swagger.</li>
     *   <li>{@code frame-ancestors 'none'}      — equivalente a X-Frame-Options: DENY.</li>
     * </ul>
     * Si Swagger se deshabilita en produccion esta politica puede endurecerse
     * eliminando las directivas unsafe-* desde application.yaml.
     */
    private static final String CSP =
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none'; " +
            "object-src 'none'; " +
            "base-uri 'self';";

    /**
     * Agrega las cabeceras de seguridad a cada respuesta HTTP antes de
     * continuar con la cadena de filtros.
     *
     * @param request     peticion HTTP entrante
     * @param response    respuesta HTTP saliente
     * @param filterChain cadena de filtros del servidor
     * @throws ServletException si ocurre un error en el procesamiento del servlet
     * @throws IOException      si ocurre un error de E/S
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // -- Proteccion contra XSS e inyeccion de contenido -----------------
        response.setHeader("Content-Security-Policy", CSP);

        // -- Prevencion de clickjacking -------------------------------------
        response.setHeader("X-Frame-Options", "DENY");

        // -- Prevencion de MIME sniffing ------------------------------------
        response.setHeader("X-Content-Type-Options", "nosniff");

        // -- Forzar HTTPS (actua en produccion con F5) ----------------------
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // -- Control de informacion en cabecera Referer ---------------------
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // -- Desactivar APIs de navegador no requeridas por la API ----------
        response.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        filterChain.doFilter(request, response);
    }
}
