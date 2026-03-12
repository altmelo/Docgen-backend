package com.docgen.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "modelos_documentos")
@Data
@SQLDelete(sql = "UPDATE modelos_documentos SET data_exclusao = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "data_exclusao IS NULL")
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 255)
    private String name;

    @Column(name = "conteudo_html", columnDefinition = "TEXT", nullable = false)
    private String htmlContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variaveis", columnDefinition = "jsonb")
    private JsonNode placeholders;

    @Column(name = "ativo", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por", nullable = false, foreignKey = @ForeignKey(name = "fk_modelos_usuarios"))
    private User createdBy;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "data_exclusao")
    private LocalDateTime deletedAt;
}
