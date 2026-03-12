package com.docgen.service;

import com.docgen.entity.Client;
import com.docgen.entity.Template;
import com.docgen.exception.ClientNotFoundException;
import com.docgen.exception.TemplateNotFoundException;
import com.docgen.repository.ClientRepository;
import com.docgen.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentGenerationService {

    private final ClientRepository clientRepository;
    private final TemplateRepository templateRepository;

    public String generateDocumentHtml(UUID clientId, UUID templateId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Cliente não encontrado com id: " + clientId));

        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Modelo de documento não encontrado com id: " + templateId));

        String html = template.getHtmlContent();

        // Replace placeholders with client data
        html = html.replace("{{NOME_COMPLETO}}", client.getFullName() != null ? client.getFullName() : "");
        html = html.replace("{{CPF}}", client.getCpf() != null ? client.getCpf() : "");
        html = html.replace("{{NACIONALIDADE}}", client.getNationality() != null ? client.getNationality() : "");
        html = html.replace("{{ESTADO_CIVIL}}", client.getMaritalStatus() != null ? client.getMaritalStatus() : "");
        // For address, perhaps serialize to string or handle separately
        // For now, skip or add basic

        return html;
    }
}
