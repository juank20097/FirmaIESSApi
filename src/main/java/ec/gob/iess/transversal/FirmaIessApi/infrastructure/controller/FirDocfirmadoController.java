/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller;

import ec.gob.iess.transversal.FirmaIessApi.application.usecase.EmpaquetarLoteUseCase;
import ec.gob.iess.transversal.FirmaIessApi.application.usecase.EnlacesLoteUseCase;
import ec.gob.iess.transversal.FirmaIessApi.application.usecase.FirDocfirmadoUseCase;
import ec.gob.iess.transversal.FirmaIessApi.application.usecase.FirLoteUseCase;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.EmpaquetarResponse;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.EnlacesResponse;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.FirmarLoteRequest;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.controller.dto.FirmarLoteResponse;
import ec.gob.iess.transversal.FirmaIessApi.model.FirDocfirmado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/iess/firmaec")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FirDocfirmadoController {

    private final FirDocfirmadoUseCase useCase;
    private final FirLoteUseCase loteUseCase;
    private final EmpaquetarLoteUseCase empaquetarUseCase;
    private final EnlacesLoteUseCase enlacesUseCase;

    /** Callback de firmadigital-servicio -- recibe el documento firmado y lo persiste. */
    @PostMapping
    public String insertar(@RequestBody FirDocfirmado request) {
        useCase.crear(request);
        return "OK";
    }

    /** WS integrador -- orquesta el flujo completo de firma digital en lote. */
    @PostMapping("/firmar")
    public FirmarLoteResponse firmar(@RequestBody FirmarLoteRequest request) {
        return loteUseCase.firmar(request);
    }

    /** Empaqueta los PDFs firmados de un lote en ZIPs de maximo 2GB en MinIO. */
    @PostMapping("/empaquetar/{idLote}")
    public EmpaquetarResponse empaquetar(@PathVariable String idLote) {
        return empaquetarUseCase.empaquetar(idLote);
    }

    /** Devuelve los enlaces presignados de descarga para los documentos firmados del lote. */
    @GetMapping("/enlaces/{idLote}")
    public EnlacesResponse obtenerEnlaces(@PathVariable String idLote) {
        return enlacesUseCase.obtenerEnlaces(idLote);
    }
}
