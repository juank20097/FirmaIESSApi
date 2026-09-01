/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.application.usecase;

import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.HybridEncryptionService;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.config.LoteContextHolder;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.FirmarDocumentoItem;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.FirmarLoteRequest;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.FirmarLoteResponse;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.FirmarParametros;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.FirDocfirmadoJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * <b>Caso de uso integrador que orquesta el flujo completo de firma digital.</b>
 *
 * <p>Flujo:</p>
 * <ol>
 *   <li>POST /servicio/documentos -- obtiene tokenJwt</li>
 *   <li>POST /api/appfirmardocumentotransversal -- firma el lote</li>
 *   <li>Polling en BD esperando callbacks con docs firmados</li>
 *   <li>Devuelve respuesta al sistema externo</li>
 * </ol>
 *
 * @author Juan Carlos Estevez Hidalgo
 * @version Revision: 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirLoteUseCase {

    /** Maximo de documentos permitidos por bloque. Configurable desde el .env. */
    @Value("${firmadigital.max-documentos:20}")
    private int maxDocumentos;

    /** Tiempo de espera entre intentos de polling (ms). */
    private static final long ESPERA_MS = 3000;

    /** Intentos maximos de polling. */
    private static final int MAX_INTENTOS = 10;

    /** Estado de respuesta para errores. */
    private static final String ESTADO_ERROR = "ERROR";

    private final FirDocfirmadoJpaRepository firmadosRepo;
    private final ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.CertificadoJpaRepository certificadoRepo;
    private final RestTemplate restTemplate;
    private final LoteContextHolder loteContextHolder;
    private final HybridEncryptionService encryptionService;

    /** URL base de WildFly. */
    @Value("${firmadigital.url}")
    private String firmadigitalUrl;

    /** Razon de firma desde el .env. */
    @Value("${firmadigital.razon}")
    private String razon;

    /** API Key de firmadigital. */
    @Value("${firmadigital.apikey}")
    private String firmadigitalApiKey;

    /** Version de FirmaEC. */
    @Value("${firmadigital.version}")
    private String version;

    /** Sistema institucional. */
    @Value("${firmadigital.sistema}")
    private String sistema;

    /**
     * <b>Orquesta el flujo completo de firma de un lote de documentos.</b>
     *
     * @param request DTO con idLote, cedula, certificado, documentos y parametros
     * @return respuesta con totales y estado del lote
     */
    public FirmarLoteResponse firmar(FirmarLoteRequest request) {

        // Validaciones basicas
        if (request.getDocumentos() == null || request.getDocumentos().isEmpty()) {
            return buildResponse(request, 0, 0, "Debe enviar al menos un documento.", ESTADO_ERROR);
        }
        if (request.getDocumentos().size() > maxDocumentos) {
            return buildResponse(request, request.getDocumentos().size(), 0,
                    "Maximo " + maxDocumentos + " documentos por bloque.", ESTADO_ERROR);
        }

        // -- Resolver certificado por sistema registrado (nunca viaja al cliente) --
        if ((request.getPkcs12() == null || request.getPkcs12().isBlank())
                && !request.esCifrado()
                && request.getCertificadoSistema() != null
                && !request.getCertificadoSistema().isBlank()) {
            var certificado = certificadoRepo.findBySistema(request.getCertificadoSistema())
                    .orElse(null);
            if (certificado == null) {
                return buildResponse(request, request.getDocumentos().size(), 0,
                        "No se encontro certificado registrado para el sistema: " + request.getCertificadoSistema(),
                        ESTADO_ERROR);
            }
            request.setCedula(certificado.getCedula());
            request.setPkcs12(certificado.getCertificado());
            request.setPassword(Base64.getEncoder()
                    .encodeToString(certificado.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }

        // -- Desencriptar payload si viene cifrado -----------------------
        if (request.esCifrado()) {
            try {
                String[] descifrado = encryptionService.desencriptarPayload(
                        request.getPkcs12Cifrado(),
                        request.getPasswordCifrado(),
                        request.getClaveAesCifrada()
                );
                request.setPkcs12(descifrado[0]);
                request.setPassword(descifrado[1]);
                log.debug("FirLoteUseCase: payload cifrado desencriptado correctamente para lote {}", request.getIdLote());
            } catch (Exception e) {
                log.error("FirLoteUseCase: error al desencriptar payload: {}", e.getMessage());
                return buildResponse(request, 0, 0, "Error al desencriptar el payload: " + e.getMessage(), ESTADO_ERROR);
            }
        }

        try {
            // Registrar cedula --> idLote en el contexto
            loteContextHolder.registrar(request.getCedula(), request.getIdLote());

            // Capturar fecha de inicio del proceso
            java.time.LocalDateTime fechaInicio = java.time.LocalDateTime.now();

            // -- PASO 1: Subir documentos --> obtener tokenJwt ----------------
            String tokenJwt = obtenerToken(request, sistema, firmadigitalApiKey);
            log.info("FirLoteUseCase: tokenJwt obtenido para lote {}", request.getIdLote());

            // -- PASO 2: Firmar con appfirmardocumentotransversal --------------
            String base64Ctx = construirBase64Contexto(sistema, version);
            String jsonFirma = construirJsonFirma(tokenJwt, sistema, version, resolverParametros(request));
            String respuestaFirma = llamarFirmarTransversal(request.getPkcs12(), request.getPassword(), jsonFirma, base64Ctx, tokenJwt);
            log.info("FirLoteUseCase: solicitud de firma enviada para lote {}", request.getIdLote());

            // -- PASO 3: Polling ----------------------------------------------
            List<String> nombresEsperados = request.getDocumentos().stream()
                    .map(FirmarDocumentoItem::getNombre).toList();

            int firmados = esperarDocumentosFirmados(request.getCedula(), nombresEsperados, fechaInicio);

            int errores = request.getDocumentos().size() - firmados;
            String estado;
            if (errores == 0) {
                estado = "OK";
            } else if (firmados == 0) {
                estado = ESTADO_ERROR;
            } else {
                estado = "PARCIAL";
            }
            String mensaje;
            if (firmados > 0) {
                mensaje = firmados + " documento(s) firmado(s) exitosamente.";
            } else if (respuestaFirma != null && !respuestaFirma.isBlank()) {
                mensaje = "Error en WildFly: " + respuestaFirma;
            } else {
                mensaje = "No se recibio el callback de firma. Verifique que firmadigital-servicio este disponible en: " + firmadigitalUrl;
            }

            loteContextHolder.limpiar(request.getCedula());

            return buildResponse(request, request.getDocumentos().size(), firmados, mensaje, estado);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("FirLoteUseCase: polling interrumpido para lote {}: {}", request.getIdLote(), e.getMessage());
            loteContextHolder.limpiar(request.getCedula());
            return buildResponse(request, request.getDocumentos().size(), 0,
                    "Proceso interrumpido: " + e.getMessage(), ESTADO_ERROR);
        } catch (Exception e) {
            log.error("FirLoteUseCase: error en lote {}: {}", request.getIdLote(), e.getMessage());
            loteContextHolder.limpiar(request.getCedula());
            return buildResponse(request, request.getDocumentos().size(), 0,
                    "Error inesperado: " + e.getMessage(), ESTADO_ERROR);
        }
    }

    // -----------------------------------------------------------------------
    // Metodos privados
    // -----------------------------------------------------------------------

    private String obtenerToken(FirmarLoteRequest request, String sistema, String apiKey) {
        String url = firmadigitalUrl + "/servicio/documentos";
        log.info("FirLoteUseCase: llamando obtenerToken -> POST {}", url);
        StringBuilder docsJson = new StringBuilder("[");
        List<FirmarDocumentoItem> docs = request.getDocumentos();
        for (int i = 0; i < docs.size(); i++) {
            FirmarDocumentoItem doc = docs.get(i);
            docsJson.append("{\"nombre\":\"").append(doc.getNombre())
                    .append("\",\"documento\":\"").append(doc.getDocumento()).append("\"}");
            if (i < docs.size() - 1) docsJson.append(",");
        }
        docsJson.append("]");
        String body = "{\"cedula\":\"" + request.getCedula() + "\","
                + "\"sistema\":\"" + sistema + "\","
                + "\"documentos\":" + docsJson + "}";
        log.info("FirLoteUseCase: body tokenJwt (sin documentos base64): cedula={}, sistema={}, docs={}",
                request.getCedula(), sistema, docs.stream().map(FirmarDocumentoItem::getNombre).toList());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", apiKey);
        try {
            String respuesta = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            log.info("FirLoteUseCase: tokenJwt obtenido OK (longitud={})", respuesta != null ? respuesta.length() : 0);
            return respuesta;
        } catch (Exception e) {
            log.error("FirLoteUseCase: fallo en POST {} — {}", url, e.getMessage());
            throw e;
        }
    }

    private String llamarFirmarTransversal(String pkcs12, String password,
                                          String jsonFirma, String base64Ctx, String tokenJwt) {
        try {
            String url = firmadigitalUrl + "/api/appfirmardocumentotransversal";
            String body = "jwt="      + URLEncoder.encode(tokenJwt,  StandardCharsets.UTF_8)
                    + "&pkcs12="   + URLEncoder.encode(pkcs12,    StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(password,  StandardCharsets.UTF_8)
                    + "&json="     + URLEncoder.encode(jsonFirma, StandardCharsets.UTF_8)
                    + "&base64="   + URLEncoder.encode(base64Ctx, StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String respuesta = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            log.debug("FirLoteUseCase: respuesta firma: {}", respuesta);
            if (respuesta != null && respuesta.contains("error")) {
                log.error("FirLoteUseCase: WildFly devolvio error: {}", respuesta);
            }
            return respuesta;
        } catch (Exception e) {
            throw new RuntimeException("Error al llamar appfirmardocumentotransversal: " + e.getMessage(), e);
        }
    }

    private int esperarDocumentosFirmados(String cedula, List<String> nombres,
                                           java.time.LocalDateTime fechaInicio) throws InterruptedException {
        for (int i = 0; i < MAX_INTENTOS; i++) {
            Thread.sleep(ESPERA_MS);
            int encontrados = firmadosRepo
                    .findByCedulaAndNombreDocumentoInAndCreatedAtAfter(cedula, nombres, fechaInicio).size();
            log.debug("FirLoteUseCase: polling intento {} -- {}/{} docs", i + 1, encontrados, nombres.size());
            if (encontrados >= nombres.size()) return encontrados;
        }
        return firmadosRepo
                .findByCedulaAndNombreDocumentoInAndCreatedAtAfter(cedula, nombres, fechaInicio).size();
    }

    private String construirBase64Contexto(String sistema, String version) {
        String json = "{\"sistemaOperativo\":\"Linux\","
                + "\"aplicacion\":\"" + sistema + "\","
                + "\"versionApp\":\"" + version + "\","
                + "\"sistema\":\"" + sistema + "\"}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String construirJsonFirma(String tokenJwt, String sistema,
                                       String version, FirmarParametros params) {
        return "{\"sistema\":\"" + sistema + "\","
                + "\"operacion\":\"FIRMAR\","
                + "\"versionFirmaEC\":\"" + version + "\","
                + "\"tokenJwt\":\"" + tokenJwt + "\","
                + "\"formatoDocumento\":\"pdf\","
                + "\"llx\":\"" + params.getLlx() + "\","
                + "\"lly\":\"" + params.getLly() + "\","
                + "\"pagina\":\"" + params.getPagina() + "\","
                + "\"tipoEstampado\":\"" + params.getTipoEstampado() + "\","
                + "\"razon\":\"" + razon + "\","
                + "\"pre\":false,"
                + "\"des\":false}";
    }

    private FirmarParametros resolverParametros(FirmarLoteRequest request) {
        FirmarParametros p = request.getParametros();
        if (p == null) p = new FirmarParametros();
        if (p.getLlx() == null)           p.setLlx("100");
        if (p.getLly() == null)           p.setLly("100");
        if (p.getPagina() == null)        p.setPagina("1");
        if (p.getTipoEstampado() == null) p.setTipoEstampado("QR");
        return p;
    }

    private FirmarLoteResponse buildResponse(FirmarLoteRequest request,
                                              int enviados, int firmados,
                                              String mensaje, String estado) {
        return FirmarLoteResponse.builder()
                .idLote(request.getIdLote())
                .totalEnviados(enviados)
                .totalFirmados(firmados)
                .totalErrores(enviados - firmados)
                .mensaje(mensaje)
                .estado(estado)
                .build();
    }
}
