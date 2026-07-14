/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.mapper;

import ec.gob.iess.transversal.FirmaIessApi.model.FirDocfirmado;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.FirDocfirmadoEntity;
import ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa.FirDocfirmadoDetalleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FirDocfirmadoMapper {

    @Autowired
    private FirDocfirmadoDetalleMapper detalleMapper;

    public FirDocfirmadoEntity toEntity(FirDocfirmado domain) {
        if (domain == null) return null;
        FirDocfirmadoEntity entity = FirDocfirmadoEntity.builder()
                .idDoc(domain.getIdDoc())
                .cedula(domain.getCedula())
                .error(domain.getError())
                .firmasValidas(domain.isFirmasValidas())
                .integridadDocumento(domain.isIntegridadDocumento())
                .nombreDocumento(domain.getNombreDocumento())
                .status(domain.getStatus() != null ? domain.getStatus() : "A")
                .deletedBy(domain.getDeletedBy())
                .deletedAt(domain.getDeletedAt())
                .build();
        if (domain.getFirmas() != null && !domain.getFirmas().isEmpty()) {
            List<FirDocfirmadoDetalleEntity> firmas = domain.getFirmas().stream()
                    .map(d -> detalleMapper.toEntity(d, entity))
                    .collect(Collectors.toList());
            entity.setFirmas(firmas);
        }
        return entity;
    }

    public FirDocfirmado toDomain(FirDocfirmadoEntity entity) {
        if (entity == null) return null;
        return FirDocfirmado.builder()
                .idDoc(entity.getIdDoc())
                .cedula(entity.getCedula())
                .error(entity.getError())
                .firmasValidas(entity.isFirmasValidas())
                .integridadDocumento(entity.isIntegridadDocumento())
                .nombreDocumento(entity.getNombreDocumento())
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
