package com.docgen.controller;

import com.docgen.dto.TemplateDTO;
import com.docgen.entity.Template;
import com.docgen.entity.User;
import com.docgen.entity.UserRole;
import com.docgen.exception.TemplateNotFoundException;
import com.docgen.repository.TemplateRepository;
import com.docgen.repository.UserRepository;
import com.docgen.service.DocumentGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Suíte de testes para TemplateController usando MockMvc.
 *
 * Testa os endpoints HTTP para gerenciamento de templates.
 *
 * @author QA Senior Engineer
 */
@WebMvcTest(TemplateController.class)
@DisplayName("TemplateController - Testes de API REST com MockMvc")
@WithMockUser(username = "dev@local")
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentGenerationService documentGenerationService;

    @MockBean
    private TemplateRepository templateRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID templateId;
    private UUID clientId;
    private TemplateDTO templateDTO;
    private String htmlContent;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        htmlContent = "<html><body>Nome: {{NOME_COMPLETO}}, CPF: {{CPF}}</body></html>";

        templateDTO = new TemplateDTO();
        templateDTO.setId(templateId);
        templateDTO.setHtmlContent(htmlContent);

        var user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("dev@local");
        user.setCpf("00000000000");
        user.setName("Dev Local");
        user.setPasswordHash("x");
        user.setRole(UserRole.Operador);

        lenient().when(userRepository.findByEmail("dev@local")).thenReturn(Optional.of(user));
        lenient().when(templateRepository.findAll()).thenReturn(List.of());
        lenient().when(templateRepository.findByIsActiveTrue()).thenReturn(List.of());
        lenient().when(templateRepository.existsById(any())).thenReturn(true);
        lenient().when(templateRepository.findById(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            Template t = new Template();
            t.setId(id);
            t.setName("Modelo");
            t.setHtmlContent(htmlContent);
            t.setIsActive(true);
            t.setCreatedBy(user);
            t.setUpdatedAt(LocalDateTime.now());
            return Optional.of(t);
        });
        lenient().when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ========== TESTES DE PREVIEW ==========

    @Nested
    @DisplayName("Preview de Template")
    class TemplatePreview {

        @Test
        @DisplayName("GET /api/v1/templates/{id}/preview?clientId=xxx deve retornar documento gerado")
        void shouldPreviewTemplateWithClient() throws Exception {
            // Arrange
            String expectedPreview = "<html><body>Nome: João Silva, CPF: 12345678900</body></html>";
            when(documentGenerationService.generateDocumentHtml(clientId, templateId))
                .thenReturn(expectedPreview);

            // Act & Assert
            mockMvc.perform(get("/api/v1/templates/{id}/preview", templateId)
                    .param("clientId", clientId.toString())
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(containsString("text/html")))
                .andExpect(content().string(expectedPreview));

            verify(documentGenerationService, times(1))
                .generateDocumentHtml(clientId, templateId);
        }

        @Test
        @DisplayName("Preview sem clientId obrigatório deve retornar 400")
        void shouldReturnBadRequestWhenClientIdMissing() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/templates/{id}/preview", templateId))
                .andExpect(status().isBadRequest());

            verify(documentGenerationService, never()).generateDocumentHtml(any(), any());
        }

        @Test
        @DisplayName("Preview com templateId inválido deve retornar 400")
        void shouldReturnBadRequestWhenTemplateIdInvalid() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/templates/{id}/preview", "invalid-uuid")
                    .param("clientId", clientId.toString()))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Preview com template não encontrado deve retornar 404")
        void shouldReturnNotFoundWhenTemplateNotFound() throws Exception {
            // Arrange
            UUID nonExistentTemplateId = UUID.randomUUID();
            when(documentGenerationService.generateDocumentHtml(clientId, nonExistentTemplateId))
                .thenThrow(new TemplateNotFoundException("Template not found"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/templates/{id}/preview", nonExistentTemplateId)
                    .param("clientId", clientId.toString()))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Preview com cliente não encontrado deve retornar 404")
        void shouldReturnNotFoundWhenClientNotFound() throws Exception {
            // Arrange
            UUID nonExistentClientId = UUID.randomUUID();
            when(documentGenerationService.generateDocumentHtml(nonExistentClientId, templateId))
                .thenThrow(new com.docgen.exception.ClientNotFoundException("Client not found"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/templates/{id}/preview", templateId)
                    .param("clientId", nonExistentClientId.toString()))
                .andExpect(status().isNotFound());
        }
    }

    // ========== TESTES DE VALIDAÇÃO DE TEMPLATE ==========

    @Nested
    @DisplayName("Validação de Template HTML")
    class TemplateValidation {

        @Test
        @DisplayName("Template com HTML válido deve ser aceito")
        void shouldAcceptValidHtmlTemplate() throws Exception {
            // Arrange
            String validHtml = "<html><head><title>Template</title></head><body>Conteúdo</body></html>";
            templateDTO.setHtmlContent(validHtml);

            // Act & Assert
            mockMvc.perform(post("/api/v1/templates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(templateDTO)))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Template vazio deve ser aceito")
        void shouldAcceptEmptyTemplate() throws Exception {
            // Arrange
            templateDTO.setHtmlContent("");

            // Act & Assert
            mockMvc.perform(post("/api/v1/templates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(templateDTO)))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Template com placeholders deve ser aceito")
        void shouldAcceptTemplateWithPlaceholders() throws Exception {
            // Arrange
            String templateWithPlaceholders =
                "<html><body>" +
                "Nome: {{NOME_COMPLETO}}, " +
                "CPF: {{CPF}}, " +
                "Nacionalidade: {{NACIONALIDADE}}" +
                "</body></html>";
            templateDTO.setHtmlContent(templateWithPlaceholders);

            // Act & Assert
            mockMvc.perform(post("/api/v1/templates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(templateDTO)))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Template com caracteres especiais deve ser aceito")
        void shouldAcceptTemplateWithSpecialCharacters() throws Exception {
            // Arrange
            String templateWithSpecialChars =
                "<html><body>&amp; &lt; &gt; &quot; &apos;</body></html>";
            templateDTO.setHtmlContent(templateWithSpecialChars);

            // Act & Assert
            mockMvc.perform(post("/api/v1/templates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(templateDTO)))
                .andExpect(status().isCreated());
        }
    }

    // ========== TESTES DE MÉTODOS HTTP ==========

    @Nested
    @DisplayName("Operações CRUD de Template")
    class TemplateOperations {

        @Test
        @DisplayName("POST /api/templates deve criar novo template com status 201")
        void shouldCreateNewTemplate() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/templates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(templateDTO)))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("GET /api/templates/{id} deve retornar template com status 200")
        void shouldGetTemplateById() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/templates/{id}", templateId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/templates/{id} com template inexistente deve retornar 404")
        void shouldReturnNotFoundWhenGettingNonExistentTemplate() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(templateRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/api/v1/templates/{id}", nonExistentId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT /api/templates/{id} deve atualizar template com status 200")
        void shouldUpdateTemplate() throws Exception {
            // Arrange
            templateDTO.setHtmlContent("<html><body>Conteúdo atualizado</body></html>");

            // Act & Assert
            mockMvc.perform(put("/api/v1/templates/{id}", templateId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(templateDTO)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE /api/templates/{id} deve deletar template com status 204")
        void shouldDeleteTemplate() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/v1/templates/{id}", templateId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("DELETE /api/templates/{id} com template inexistente deve retornar 404")
        void shouldReturnNotFoundWhenDeletingNonExistentTemplate() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(templateRepository.existsById(nonExistentId)).thenReturn(false);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/templates/{id}", nonExistentId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        }
    }

    // ========== TESTES DE LISTAGEM ==========

    @Nested
    @DisplayName("Listagem de Templates")
    class TemplateList {

        @Test
        @DisplayName("GET /api/templates deve retornar lista de templates com status 200")
        void shouldListAllTemplates() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/templates")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(containsString("application/json")));
        }

        @Test
        @DisplayName("GET /api/templates?page=0&size=10 deve suportar paginação")
        void shouldSupportPaginationInTemplateList() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/templates")
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/templates?search=termo deve suportar busca")
        void shouldSupportSearchInTemplateList() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/templates")
                    .param("search", "contrato")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }
    }

    // ========== TESTES DE VALIDAÇÃO DE REQUISIÇÃO ==========

    @Nested
    @DisplayName("Validação de Requisição")
    class RequestValidation {

        @Test
        @DisplayName("POST sem Content-Type deve retornar 415 Unsupported Media Type")
        void shouldReturnUnsupportedMediaTypeWhenContentTypeNotSet() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/templates")
                    .content(objectMapper.writeValueAsString(templateDTO)))
                .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("POST com body vazio deve retornar 400 Bad Request")
        void shouldReturnBadRequestWhenBodyEmpty() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/templates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(""))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST com JSON inválido deve retornar 400 Bad Request")
        void shouldReturnBadRequestWhenJsonInvalid() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/templates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PUT com templateId inválido deve retornar 400")
        void shouldReturnBadRequestWhenTemplateIdInvalidInUpdate() throws Exception {
            // Act & Assert
            mockMvc.perform(put("/api/v1/templates/{id}", "invalid-uuid")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(templateDTO)))
                .andExpect(status().isBadRequest());
        }
    }

    // ========== TESTES DE HEADERS ==========

    @Nested
    @DisplayName("Validação de Headers HTTP")
    class HeaderValidation {

        @Test
        @DisplayName("POST deve retornar Location header com URL do novo recurso")
        void shouldReturnLocationHeaderOnCreate() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/templates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(templateDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("GET deve retornar ETag para cache")
        void shouldReturnETagForCaching() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/templates/{id}", templateId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"));
        }

        @Test
        @DisplayName("GET deve retornar Last-Modified header")
        void shouldReturnLastModifiedHeader() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/templates/{id}", templateId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Last-Modified"));
        }
    }

    // ========== TESTES DE SEGURANÇA ==========

    @Nested
    @DisplayName("Validação de Segurança")
    class SecurityValidation {

        @Test
        @DisplayName("POST deve validar tamanho máximo do template")
        void shouldValidateTemplateSizeLimit() throws Exception {
            // Arrange - Criar template muito grande
            String largeHtml = "<html><body>" + "a".repeat(10_000_000) + "</body></html>";
            templateDTO.setHtmlContent(largeHtml);

            // Act & Assert
            mockMvc.perform(post("/api/v1/templates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(templateDTO)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Template não deve conter scripts maliciosos (XSS prevention)")
        void shouldPreventXSSInTemplate() throws Exception {
            // Arrange
            String xssAttempt = "<html><body><script>alert('XSS')</script></body></html>";
            templateDTO.setHtmlContent(xssAttempt);

            // Este teste pressupõe que o sistema sanitiza/valida contra XSS
            // A resposta pode variar: aceitar ou rejeitar
            mockMvc.perform(post("/api/v1/templates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(templateDTO)))
                .andExpect(status().is4xxClientError());
        }
    }

    // ========== TESTES DE PERFORMANCE ==========

    @Nested
    @DisplayName("Performance de Template")
    class TemplatePerformance {

        @Test
        @DisplayName("GET /api/templates deve responder em menos de 500ms")
        void shouldListTemplatesQuickly() throws Exception {
            long startTime = System.currentTimeMillis();

            mockMvc.perform(get("/api/v1/templates")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            assert duration < 500 : "Request took " + duration + "ms, expected < 500ms";
        }

        @Test
        @DisplayName("POST /api/templates deve responder em menos de 1 segundo")
        void shouldCreateTemplateQuickly() throws Exception {
            long startTime = System.currentTimeMillis();

            mockMvc.perform(post("/api/v1/templates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(templateDTO)))
                .andExpect(status().isCreated());

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            assert duration < 1000 : "Request took " + duration + "ms, expected < 1000ms";
        }
    }
}

