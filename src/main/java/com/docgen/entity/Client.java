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
@Table(name = "clientes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_clientes_cpf", columnNames = "cpf")
})
@Data
@SQLDelete(sql = "UPDATE clientes SET data_exclusao = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "data_exclusao IS NULL")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 11)
    private String cpf;

    @Column(name = "nome_completo", nullable = false, length = 255)
    private String fullName;

    @Column(name = "nacionalidade", nullable = false, length = 100)
    private String nationality;

    @Column(name = "estado_civil", nullable = false, length = 50)
    private String maritalStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "endereco", columnDefinition = "jsonb")
    private JsonNode address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por", nullable = false, foreignKey = @ForeignKey(name = "fk_clientes_usuarios"))
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
