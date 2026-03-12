package com.docgen.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ClientDTO {

    private UUID id;
    private String cpf;
    private String fullName;
    private String nationality;
    private String maritalStatus;
    private JsonNode address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
