package com.docgen.controller;

import com.docgen.dto.ClientDTO;
import com.docgen.repository.ClientRepository;
import com.docgen.repository.UserRepository;
import com.docgen.service.DocumentGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Suíte de testes para ClientController usando MockMvc.
 *
 * Testa os endpoints HTTP para geração de documentos.
 *
 * @author QA Senior Engineer
 */
@WebMvcTest(controllers = ClientController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@DisplayName("ClientController - Testes de API REST com MockMvc")
@WithMockUser(username = "dev@local")
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentGenerationService documentGenerationService;

    @MockBean
    private ClientRepository clientRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID clientId;
    private UUID templateId;
    private String expectedHtmlResponse;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        expectedHtmlResponse = "<html><body>Nome: João Silva, CPF: 12345678900</body></html>";
    }

    // ========== TESTES DE SUCESSO ==========

    @Nested
    @DisplayName("Geração de Documentos - Sucesso")
    class GenerateDocumentSuccess {

        @Test
        @DisplayName("GET /api/clients/{id}/documents deve retornar HTML gerado com status 200")
        void shouldGenerateDocumentSuccessfully() throws Exception {
            // Arrange
            when(documentGenerationService.generateDocumentHtml(clientId, templateId))
                .thenReturn(expectedHtmlResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString())
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(expectedHtmlResponse));

            // Verify
            verify(documentGenerationService, times(1))
                .generateDocumentHtml(clientId, templateId);
        }

        @Test
        @DisplayName("Deve retornar Content-Type correto (text/html)")
        void shouldReturnCorrectContentType() throws Exception {
            // Arrange
            when(documentGenerationService.generateDocumentHtml(any(), any()))
                .thenReturn(expectedHtmlResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/html")));
        }

        @Test
        @DisplayName("Deve retornar documento com múltiplos placeholders substituídos")
        void shouldReturnDocumentWithMultiplePlaceholders() throws Exception {
            // Arrange
            String complexHtml = "<html><body>" +
                    "<h1>João Silva</h1>" +
                    "<p>CPF: 12345678900</p>" +
                    "<p>Nacionalidade: Brasileiro</p>" +
                    "<p>Estado Civil: Solteiro</p>" +
                    "</body></html>";

            when(documentGenerationService.generateDocumentHtml(clientId, templateId))
                .thenReturn(complexHtml);

            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("João Silva")))
                .andExpect(content().string(containsString("12345678900")))
                .andExpect(content().string(containsString("Brasileiro")))
                .andExpect(content().string(containsString("Solteiro")));
        }
    }

    // ========== TESTES DE ERRO - BAD REQUEST ==========

    @Nested
    @DisplayName("Validação de Parâmetros - Bad Request")
    class ParameterValidation {

        @Test
        @DisplayName("GET sem templateId obrigatório deve retornar 400 Bad Request")
        void shouldReturnBadRequestWhenTemplateIdMissing() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

            // Verify que o serviço NÃO foi chamado
            verify(documentGenerationService, never()).generateDocumentHtml(any(), any());
        }

        @Test
        @DisplayName("GET com templateId inválido deve retornar 400")
        void shouldReturnBadRequestWhenTemplateIdInvalid() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", "invalid-uuid"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET com clientId inválido deve retornar 400")
        void shouldReturnBadRequestWhenClientIdInvalid() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", "invalid-uuid")
                    .param("templateId", templateId.toString()))
                .andExpect(status().isBadRequest());
        }
    }

    // ========== TESTES DE ERRO - NOT FOUND ==========

    @Nested
    @DisplayName("Recurso Não Encontrado - 404")
    class ResourceNotFound {

        @Test
        @DisplayName("GET com clientId inexistente deve retornar 404")
        void shouldReturnNotFoundWhenClientNotFound() throws Exception {
            // Arrange
            UUID nonExistentClientId = UUID.randomUUID();
            when(documentGenerationService.generateDocumentHtml(nonExistentClientId, templateId))
                .thenThrow(new com.docgen.exception.ClientNotFoundException(
                    "Client not found with id: " + nonExistentClientId));

            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", nonExistentClientId)
                    .param("templateId", templateId.toString()))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET com templateId inexistente deve retornar 404")
        void shouldReturnNotFoundWhenTemplateNotFound() throws Exception {
            // Arrange
            UUID nonExistentTemplateId = UUID.randomUUID();
            when(documentGenerationService.generateDocumentHtml(clientId, nonExistentTemplateId))
                .thenThrow(new com.docgen.exception.TemplateNotFoundException(
                    "Template not found with id: " + nonExistentTemplateId));

            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", nonExistentTemplateId.toString()))
                .andExpect(status().isNotFound());
        }
    }

    // ========== TESTES DE ERRO - SERVIDOR ==========

    @Nested
    @DisplayName("Erro do Servidor - 500")
    class ServerError {

        @Test
        @DisplayName("GET com erro inesperado deve retornar 500")
        void shouldReturnInternalServerErrorOnUnexpectedException() throws Exception {
            // Arrange
            when(documentGenerationService.generateDocumentHtml(clientId, templateId))
                .thenThrow(new RuntimeException("Unexpected error during document generation"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString()))
                .andExpect(status().isInternalServerError());
        }
    }

    // ========== TESTES DE HEADERS ==========

    @Nested
    @DisplayName("Validação de Headers HTTP")
    class HeaderValidation {

        @Test
        @DisplayName("Resposta deve incluir Content-Length")
        void shouldIncludeContentLengthHeader() throws Exception {
            // Arrange
            when(documentGenerationService.generateDocumentHtml(clientId, templateId))
                .thenReturn(expectedHtmlResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Length"));
        }

        @Test
        @DisplayName("Resposta deve incluir Content-Type com charset")
        void shouldIncludeCharsetInContentType() throws Exception {
            // Arrange
            when(documentGenerationService.generateDocumentHtml(clientId, templateId))
                .thenReturn(expectedHtmlResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("charset")));
        }
    }

    // ========== TESTES DE MÉTODOS HTTP ==========

    @Nested
    @DisplayName("Métodos HTTP - Validação")
    class HttpMethods {

        @Test
        @DisplayName("POST não deve ser permitido (405 Method Not Allowed)")
        void shouldReturnMethodNotAllowedForPost() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString())
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("DELETE não deve ser permitido (405 Method Not Allowed)")
        void shouldReturnMethodNotAllowedForDelete() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString()))
                .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("PUT não deve ser permitido (405 Method Not Allowed)")
        void shouldReturnMethodNotAllowedForPut() throws Exception {
            // Act & Assert
            mockMvc.perform(put("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        }
    }

    // ========== TESTES DE PERFORMANCE ==========

    @Nested
    @DisplayName("Performance e Timing")
    class Performance {

        @Test
        @DisplayName("Requisição deve completar em menos de 1 segundo")
        void shouldCompleteRequestInLessThanOneSecond() throws Exception {
            // Arrange
            when(documentGenerationService.generateDocumentHtml(clientId, templateId))
                .thenReturn(expectedHtmlResponse);

            long startTime = System.currentTimeMillis();

            // Act
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString()))
                .andExpect(status().isOk());

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Assert
            assert duration < 1000 : "Request took " + duration + "ms, expected < 1000ms";
        }
    }

    // ========== TESTES DE CONTEÚDO ==========

    @Nested
    @DisplayName("Validação de Conteúdo da Resposta")
    class ResponseContent {

        @Test
        @DisplayName("Resposta deve conter HTML válido (tags abertas e fechadas)")
        void shouldReturnValidHtmlContent() throws Exception {
            // Arrange
            String validHtml = "<html><body><p>Conteúdo</p></body></html>";
            when(documentGenerationService.generateDocumentHtml(clientId, templateId))
                .thenReturn(validHtml);

            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html>")))
                .andExpect(content().string(containsString("</html>")))
                .andExpect(content().string(containsString("<body>")))
                .andExpect(content().string(containsString("</body>")));
        }

        @Test
        @DisplayName("Resposta pode ser documento vazio")
        void shouldAllowEmptyDocument() throws Exception {
            // Arrange
            when(documentGenerationService.generateDocumentHtml(clientId, templateId))
                .thenReturn("");

            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        }

        @Test
        @DisplayName("Resposta pode conter caracteres especiais")
        void shouldHandleSpecialCharactersInResponse() throws Exception {
            // Arrange
            String htmlWithSpecialChars = "<html><body>João Silva & Cia. <Ltd.></body></html>";
            when(documentGenerationService.generateDocumentHtml(clientId, templateId))
                .thenReturn(htmlWithSpecialChars);

            // Act & Assert
            mockMvc.perform(get("/api/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&")))
                .andExpect(content().string(containsString("<")))
                .andExpect(content().string(containsString(">")));
        }
    }

    // ========== TESTES DE FLUXO ==========

    @Nested
    @DisplayName("Fluxo de Requisição")
    class RequestFlow {

        @Test
        @DisplayName("GET deve aceitar UUID válido no path")
        void shouldAcceptValidUuidInPath() throws Exception {
            // Arrange
            when(documentGenerationService.generateDocumentHtml(clientId, templateId))
                .thenReturn(expectedHtmlResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId.toString())
                    .param("templateId", templateId.toString()))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET deve aceitar UUID válido no query param")
        void shouldAcceptValidUuidInQueryParam() throws Exception {
            // Arrange
            when(documentGenerationService.generateDocumentHtml(clientId, templateId))
                .thenReturn(expectedHtmlResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString()))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Serviço deve ser chamado exatamente uma vez")
        void shouldCallServiceExactlyOnce() throws Exception {
            // Arrange
            when(documentGenerationService.generateDocumentHtml(clientId, templateId))
                .thenReturn(expectedHtmlResponse);

            // Act
            mockMvc.perform(get("/api/v1/clients/{id}/documents", clientId)
                    .param("templateId", templateId.toString()))
                .andExpect(status().isOk());

            // Assert
            verify(documentGenerationService, times(1))
                .generateDocumentHtml(clientId, templateId);
            verifyNoMoreInteractions(documentGenerationService);
        }
    }
}

