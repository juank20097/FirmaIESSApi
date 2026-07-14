/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.mapper;

import ec.gob.iess.transversal.FirmaIessApi.model.FirDocfirmadoDetalle;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.FirDocfirmadoDetalleEntity;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.FirDocfirmadoEntity;
import org.springframework.stereotype.Component;

@Component
public class FirDocfirmadoDetalleMapper {

    public FirDocfirmadoDetalleEntity toEntity(FirDocfirmadoDetalle domain, FirDocfirmadoEntity documento) {
        if (domain == null) return null;
        return FirDocfirmadoDetalleEntity.builder()
                .idDocDet(domain.getIdDocDet())
                .firmados(documento)
                .apellido(domain.getApellido())
                .cargo(domain.getCargo())
                .cedula(domain.getCedula())
                .certificadoDigitalValido(domain.isCertificadoDigitalValido())
                .certificadoVigente(domain.isCertificadoVigente())
                .clavesUso(domain.getClavesUso())
                .emitidoPara(domain.getEmitidoPara())
                .emitidoPor(domain.getEmitidoPor())
                .entidadCertificadora(domain.getEntidadCertificadora())
                .fechaFirma(domain.getFechaFirma())
                .fechaRevocado(domain.getFechaRevocado())
                .fechaSellotiempo(domain.getFechaSellotiempo())
                .institucion(domain.getInstitucion())
                .integridadFirma(domain.isIntegridadFirma())
                .localizacion(domain.getLocalizacion())
                .nombre(domain.getNombre())
                .razonFirma(domain.getRazonFirma())
                .selladoTiempo(domain.isSelladoTiempo())
                .serial(domain.getSerial())
                .validoDesde(domain.getValidoDesde())
                .validoHasta(domain.getValidoHasta())
                .status(domain.getStatus() != null ? domain.getStatus() : "A")
                .deletedBy(domain.getDeletedBy())
                .deletedAt(domain.getDeletedAt())
                .build();
    }

    public FirDocfirmadoDetalle toDomain(FirDocfirmadoDetalleEntity entity) {
        if (entity == null) return null;
        return FirDocfirmadoDetalle.builder()
                .idDocDet(entity.getIdDocDet())
                .idDoc(entity.getFirmados() != null ? entity.getFirmados().getIdDoc() : null)
                .apellido(entity.getApellido())
                .cargo(entity.getCargo())
                .cedula(entity.getCedula())
                .certificadoDigitalValido(entity.isCertificadoDigitalValido())
                .certificadoVigente(entity.isCertificadoVigente())
                .clavesUso(entity.getClavesUso())
                .emitidoPara(entity.getEmitidoPara())
                .emitidoPor(entity.getEmitidoPor())
                .entidadCertificadora(entity.getEntidadCertificadora())
                .fechaFirma(entity.getFechaFirma())
                .fechaRevocado(entity.getFechaRevocado())
                .fechaSellotiempo(entity.getFechaSellotiempo())
                .institucion(entity.getInstitucion())
                .integridadFirma(entity.isIntegridadFirma())
                .localizacion(entity.getLocalizacion())
                .nombre(entity.getNombre())
                .razonFirma(entity.getRazonFirma())
                .selladoTiempo(entity.isSelladoTiempo())
                .serial(entity.getSerial())
                .validoDesde(entity.getValidoDesde())
                .validoHasta(entity.getValidoHasta())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .deletedBy(entity.getDeletedBy())
                .deletedAt(entity.getDeletedAt())
                .build();
    }
}
