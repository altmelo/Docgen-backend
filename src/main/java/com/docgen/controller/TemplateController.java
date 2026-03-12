package com.docgen.controller;

import com.docgen.dto.TemplateDTO;
import com.docgen.entity.Template;
import com.docgen.exception.TemplateNotFoundException;
import com.docgen.repository.TemplateRepository;
import com.docgen.repository.UserRepository;
import com.docgen.service.DocumentGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateRepository templateRepository;
    private final DocumentGenerationService documentGenerationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<TemplateDTO>> getAllTemplates() {
        List<TemplateDTO> templates = templateRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/active")
    public ResponseEntity<List<TemplateDTO>> getActiveTemplates() {
        List<TemplateDTO> templates = templateRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateDTO> getTemplateById(@PathVariable UUID id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Modelo de documento não encontrado com id: " + id));
        var eTag = "\"" + template.getId() + ":" + template.getUpdatedAt() + "\"";
        var lastModified = template.getUpdatedAt() != null
                ? template.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : -1L;
        return ResponseEntity.ok()
                .eTag(eTag)
                .lastModified(lastModified)
                .body(mapToDTO(template));
    }

    @GetMapping(value = "/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewTemplate(@PathVariable UUID id, @RequestParam UUID clientId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(documentGenerationService.generateDocumentHtml(clientId, id));
    }

    @PostMapping
    public ResponseEntity<TemplateDTO> createTemplate(@RequestBody TemplateDTO templateDTO) {
        validateHtmlContent(templateDTO.getHtmlContent());
        Template template = mapToEntity(templateDTO);
        template.setCreatedBy(getCurrentUser());
        Template saved = templateRepository.save(template);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(mapToDTO(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateDTO> updateTemplate(@PathVariable UUID id, @RequestBody TemplateDTO templateDTO) {
        validateHtmlContent(templateDTO.getHtmlContent());
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Modelo de documento não encontrado com id: " + id));
        template.setName(templateDTO.getName());
        template.setHtmlContent(templateDTO.getHtmlContent());
        template.setPlaceholders(templateDTO.getPlaceholders());
        template.setIsActive(templateDTO.getIsActive());
        Template saved = templateRepository.save(template);
        return ResponseEntity.ok(mapToDTO(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        if (!templateRepository.existsById(id)) {
            throw new TemplateNotFoundException("Modelo de documento não encontrado com id: " + id);
        }
        templateRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private TemplateDTO mapToDTO(Template template) {
        TemplateDTO dto = new TemplateDTO();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setHtmlContent(template.getHtmlContent());
        dto.setPlaceholders(template.getPlaceholders());
        dto.setIsActive(template.getIsActive());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        return dto;
    }

    private Template mapToEntity(TemplateDTO dto) {
        Template template = new Template();
        template.setName(dto.getName() != null ? dto.getName() : "Modelo sem nome");
        template.setHtmlContent(dto.getHtmlContent());
        template.setPlaceholders(dto.getPlaceholders());
        template.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return template;
    }

    private void validateHtmlContent(String htmlContent) {
        if (htmlContent == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Conteúdo HTML é obrigatório");
        }
        if (htmlContent.length() > 1_000_000) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Conteúdo HTML excede o tamanho máximo");
        }
        if (htmlContent.toLowerCase().contains("<script")) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Conteúdo HTML contém scripts não permitidos");
        }
    }

    private com.docgen.entity.User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new com.docgen.auth.exception.AuthException("Usuário não autenticado");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new com.docgen.auth.exception.AuthException("Usuário não encontrado"));
    }
}
