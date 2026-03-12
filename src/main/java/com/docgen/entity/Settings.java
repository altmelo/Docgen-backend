package com.docgen.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "configuracoes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_configuracoes_chave", columnNames = "chave")
})
@Data
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chave", unique = true, nullable = false, length = 100)
    private String key;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor", columnDefinition = "jsonb")
    private JsonNode value;

    @UpdateTimestamp
    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por", foreignKey = @ForeignKey(name = "fk_configuracoes_usuarios"))
    private User updatedBy;
}
