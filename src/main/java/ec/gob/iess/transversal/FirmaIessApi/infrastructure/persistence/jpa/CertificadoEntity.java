/**
 * Copyright 2026 INSTITUTO ECUATORIANO DE SEGURIDAD SOCIAL - ECUADOR.
 * Todos los derechos reservados.
 */
package ec.gob.iess.transversal.FirmaIessApi.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "certificados", schema = "iess")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sistema", nullable = false, unique = true, length = 50)
    private String sistema;

    @Column(name = "cedula", nullable = false, length = 13)
    private String cedula;

    @Column(name = "certificado", nullable = false, columnDefinition = "TEXT")
    private String certificado;

    @Column(name = "password", nullable = false, length = 500)
    private String password;
}
