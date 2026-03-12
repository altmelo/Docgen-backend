package com.docgen.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TemplateDTO {

    private UUID id;
    private String name;
    private String htmlContent;
    private JsonNode placeholders;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
