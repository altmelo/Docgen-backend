package com.docgen.controller;

import com.docgen.dto.ClientDTO;
import com.docgen.entity.Client;
import com.docgen.exception.ClientNotFoundException;
import com.docgen.repository.ClientRepository;
import com.docgen.repository.UserRepository;
import com.docgen.service.DocumentGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientRepository clientRepository;
    private final DocumentGenerationService documentGenerationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ClientDTO>> getAllClients() {
        List<ClientDTO> clients = clientRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getClientById(@PathVariable UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Cliente não encontrado com id: " + id));
        return ResponseEntity.ok(mapToDTO(client));
    }

    @GetMapping(value = "/{id}/documents", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> generateDocument(@PathVariable UUID id, @RequestParam UUID templateId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(documentGenerationService.generateDocumentHtml(id, templateId));
    }

    @PostMapping
    public ResponseEntity<ClientDTO> createClient(@RequestBody ClientDTO clientDTO) {
        Client client = mapToEntity(clientDTO);
        client.setCreatedBy(getCurrentUser());
        Client saved = clientRepository.save(client);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDTO(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO> updateClient(@PathVariable UUID id, @RequestBody ClientDTO clientDTO) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Cliente não encontrado com id: " + id));
        client.setCpf(clientDTO.getCpf());
        client.setFullName(clientDTO.getFullName());
        client.setNationality(clientDTO.getNationality());
        client.setMaritalStatus(clientDTO.getMaritalStatus());
        client.setAddress(clientDTO.getAddress());
        Client saved = clientRepository.save(client);
        return ResponseEntity.ok(mapToDTO(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable UUID id) {
        clientRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ClientDTO mapToDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setCpf(client.getCpf());
        dto.setFullName(client.getFullName());
        dto.setNationality(client.getNationality());
        dto.setMaritalStatus(client.getMaritalStatus());
        dto.setAddress(client.getAddress());
        dto.setCreatedAt(client.getCreatedAt());
        dto.setUpdatedAt(client.getUpdatedAt());
        return dto;
    }

    private Client mapToEntity(ClientDTO dto) {
        Client client = new Client();
        client.setCpf(dto.getCpf());
        client.setFullName(dto.getFullName());
        client.setNationality(dto.getNationality());
        client.setMaritalStatus(dto.getMaritalStatus());
        client.setAddress(dto.getAddress());
        return client;
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
