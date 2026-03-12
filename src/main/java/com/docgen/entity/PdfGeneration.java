package com.docgen.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "geracoes_pdf")
@Data
public class PdfGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_geracoes_clientes"))
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modelo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_geracoes_modelos"))
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerado_por", nullable = false, foreignKey = @ForeignKey(name = "fk_geracoes_usuarios"))
    private User generatedBy;

    @Column(name = "data_geracao", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "url_pdf", length = 500)
    private String pdfUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_snapshot", columnDefinition = "jsonb")
    private JsonNode snapshotData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuracoes_aplicadas", columnDefinition = "jsonb")
    private JsonNode settingsApplied;
}
