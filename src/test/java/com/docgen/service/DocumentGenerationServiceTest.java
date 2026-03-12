package com.docgen.service;

import com.docgen.entity.Client;
import com.docgen.entity.Template;
import com.docgen.exception.ClientNotFoundException;
import com.docgen.exception.TemplateNotFoundException;
import com.docgen.repository.ClientRepository;
import com.docgen.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Suíte de testes para a classe DocumentGenerationService.
 *
 * Padrão: Arrange-Act-Assert (Given-When-Then)
 *
 * Cobertura:
 * - ✅ Caminho feliz: substituição correta de placeholders
 * - ✅ Tratamento de exceções: Cliente não encontrado
 * - ✅ Tratamento de exceções: Template não encontrado
 * - ✅ Placeholders incompletos: dados nulos/vazios
 * - ✅ Casos edge: múltiplos placeholders repetidos, HTML vazio, cliente vazio
 *
 * @author QA Senior Engineer
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentGenerationService - Testes Unitários")
class DocumentGenerationServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private TemplateRepository templateRepository;

    @InjectMocks
    private DocumentGenerationService documentGenerationService;

    private UUID clientId;
    private UUID templateId;
    private Client validClient;
    private Template validTemplate;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        templateId = UUID.randomUUID();

        // Arranjar cliente padrão válido
        validClient = new Client();
        validClient.setId(clientId);
        validClient.setFullName("João Silva");
        validClient.setCpf("12345678900");
        validClient.setNationality("Brasileiro");
        validClient.setMaritalStatus("Solteiro");

        // Arranjar template padrão válido
        validTemplate = new Template();
        validTemplate.setId(templateId);
        validTemplate.setHtmlContent(
            "<html><body>Nome: {{NOME_COMPLETO}}, CPF: {{CPF}}, " +
            "Nacionalidade: {{NACIONALIDADE}}, Estado Civil: {{ESTADO_CIVIL}}</body></html>"
        );
    }

    // ========== TESTES DE SUCESSO (CAMINHO FELIZ) ==========

    @Nested
    @DisplayName("Caminho Feliz - Substituição de Placeholders")
    class HappyPath {

        @Test
        @DisplayName("Deve substituir todos os placeholders corretamente com dados válidos")
        void generateDocumentHtml_shouldReplacePlaceholdersCorrectly() {
            // Arrange
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplate));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            String expected = "<html><body>Nome: João Silva, CPF: 12345678900, " +
                            "Nacionalidade: Brasileiro, Estado Civil: Solteiro</body></html>";
            assertEquals(expected, result);

            // Verify que os repositórios foram chamados exatamente uma vez
            verify(clientRepository, times(1)).findById(clientId);
            verify(templateRepository, times(1)).findById(templateId);
        }

        @Test
        @DisplayName("Deve substituir placeholders repetidos múltiplas vezes no template")
        void generateDocumentHtml_shouldReplaceRepeatedPlaceholders() {
            // Arrange
            Template templateWithRepeatedPlaceholders = new Template();
            templateWithRepeatedPlaceholders.setHtmlContent(
                "<html><body>" +
                "<p>Primeiro nome: {{NOME_COMPLETO}}</p>" +
                "<p>Segundo nome: {{NOME_COMPLETO}}</p>" +
                "<p>Terceiro nome: {{NOME_COMPLETO}}</p>" +
                "</body></html>"
            );

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(templateWithRepeatedPlaceholders));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            String expected = "<html><body>" +
                            "<p>Primeiro nome: João Silva</p>" +
                            "<p>Segundo nome: João Silva</p>" +
                            "<p>Terceiro nome: João Silva</p>" +
                            "</body></html>";
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Deve processar template com apenas um placeholder")
        void generateDocumentHtml_shouldProcessSinglePlaceholder() {
            // Arrange
            Template simpleTemplate = new Template();
            simpleTemplate.setHtmlContent("<h1>Bem-vindo, {{NOME_COMPLETO}}!</h1>");

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(simpleTemplate));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            String expected = "<h1>Bem-vindo, João Silva!</h1>";
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Deve processar template com placeholders em diferentes contextos HTML")
        void generateDocumentHtml_shouldProcessPlaceholdersInDifferentHtmlContexts() {
            // Arrange
            Template complexTemplate = new Template();
            complexTemplate.setHtmlContent(
                "<html><head><title>Documento de {{NOME_COMPLETO}}</title></head>" +
                "<body><h1>{{NOME_COMPLETO}}</h1>" +
                "<p>CPF: <strong>{{CPF}}</strong></p>" +
                "<footer>Emitido para {{NOME_COMPLETO}}</footer></body></html>"
            );

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(complexTemplate));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            assertTrue(result.contains("Documento de João Silva"));
            assertTrue(result.contains("<h1>João Silva</h1>"));
            assertTrue(result.contains("CPF: <strong>12345678900</strong>"));
            assertTrue(result.contains("Emitido para João Silva"));
        }
    }

    // ========== TESTES DE ERRO - EXCEÇÕES ==========

    @Nested
    @DisplayName("Tratamento de Exceções - Cliente não encontrado")
    class ClientNotFoundExceptions {

        @Test
        @DisplayName("Deve lançar ClientNotFoundException quando cliente não existe")
        void generateDocumentHtml_shouldThrowClientNotFoundException_whenClientNotFound() {
            // Arrange
            when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

            // Act & Assert
            ClientNotFoundException exception = assertThrows(ClientNotFoundException.class,
                () -> documentGenerationService.generateDocumentHtml(clientId, templateId));

            assertEquals("Cliente não encontrado com id: " + clientId, exception.getMessage());

            // Verify que o template repository não foi chamado
            verify(clientRepository, times(1)).findById(clientId);
            verify(templateRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lançar ClientNotFoundException com ID correto na mensagem")
        void generateDocumentHtml_shouldThrowClientNotFoundException_withCorrectId() {
            // Arrange
            UUID specificClientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            when(clientRepository.findById(specificClientId)).thenReturn(Optional.empty());

            // Act & Assert
            ClientNotFoundException exception = assertThrows(ClientNotFoundException.class,
                () -> documentGenerationService.generateDocumentHtml(specificClientId, templateId));

            assertAll("Verificações de exceção",
                () -> assertNotNull(exception),
                () -> assertTrue(exception.getMessage().contains("550e8400-e29b-41d4-a716-446655440000")),
                () -> assertTrue(exception.getMessage().contains("Cliente não encontrado"))
            );
        }
    }

    @Nested
    @DisplayName("Tratamento de Exceções - Template não encontrado")
    class TemplateNotFoundExceptions {

        @Test
        @DisplayName("Deve lançar TemplateNotFoundException quando template não existe")
        void generateDocumentHtml_shouldThrowTemplateNotFoundException_whenTemplateNotFound() {
            // Arrange
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

            // Act & Assert
            TemplateNotFoundException exception = assertThrows(TemplateNotFoundException.class,
                () -> documentGenerationService.generateDocumentHtml(clientId, templateId));

            assertEquals("Modelo de documento não encontrado com id: " + templateId, exception.getMessage());

            // Verify chamadas corretas
            verify(clientRepository, times(1)).findById(clientId);
            verify(templateRepository, times(1)).findById(templateId);
        }

        @Test
        @DisplayName("Deve lançar TemplateNotFoundException com ID correto na mensagem")
        void generateDocumentHtml_shouldThrowTemplateNotFoundException_withCorrectId() {
            // Arrange
            UUID specificTemplateId = UUID.fromString("660f9500-f30c-52e5-b827-557766551111");
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(templateRepository.findById(specificTemplateId)).thenReturn(Optional.empty());

            // Act & Assert
            TemplateNotFoundException exception = assertThrows(TemplateNotFoundException.class,
                () -> documentGenerationService.generateDocumentHtml(clientId, specificTemplateId));

            assertAll("Verificações de exceção",
                () -> assertNotNull(exception),
                () -> assertTrue(exception.getMessage().contains("660f9500-f30c-52e5-b827-557766551111")),
                () -> assertTrue(exception.getMessage().contains("Modelo de documento não encontrado"))
            );
        }
    }

    // ========== TESTES DE PLACEHOLDERS INCOMPLETOS ==========

    @Nested
    @DisplayName("Placeholders Incompletos - Dados Nulos/Vazios")
    class IncompletePlaceholders {

        @Test
        @DisplayName("Deve substituir placeholders nulos com string vazia")
        void generateDocumentHtml_shouldHandleNullClientFields() {
            // Arrange
            Client clientWithNulls = new Client();
            clientWithNulls.setFullName("João Silva");
            clientWithNulls.setCpf(null); // Campo nulo
            clientWithNulls.setNationality("Brasileiro");
            clientWithNulls.setMaritalStatus(null); // Campo nulo

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(clientWithNulls));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplate));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            String expected = "<html><body>Nome: João Silva, CPF: , " +
                            "Nacionalidade: Brasileiro, Estado Civil: </body></html>";
            assertEquals(expected, result);

            // Verify que não há null pointer exception
            assertNotNull(result);
            assertTrue(result.contains("Nome: João Silva"));
        }

        @Test
        @DisplayName("Deve substituir todos os placeholders com strings vazias quando cliente tem todos campos nulos")
        void generateDocumentHtml_shouldHandleAllNullClientFields() {
            // Arrange
            Client completelyNullClient = new Client();
            completelyNullClient.setFullName(null);
            completelyNullClient.setCpf(null);
            completelyNullClient.setNationality(null);
            completelyNullClient.setMaritalStatus(null);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(completelyNullClient));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplate));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            String expected = "<html><body>Nome: , CPF: , " +
                            "Nacionalidade: , Estado Civil: </body></html>";
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Deve substituir placeholders vazios com string vazia")
        void generateDocumentHtml_shouldHandleEmptyClientFields() {
            // Arrange
            Client clientWithEmptyFields = new Client();
            clientWithEmptyFields.setFullName("João Silva");
            clientWithEmptyFields.setCpf(""); // Campo vazio
            clientWithEmptyFields.setNationality("");
            clientWithEmptyFields.setMaritalStatus("Solteiro");

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(clientWithEmptyFields));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplate));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            String expected = "<html><body>Nome: João Silva, CPF: , " +
                            "Nacionalidade: , Estado Civil: Solteiro</body></html>";
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Deve substituir placeholders com valores contendo espaços em branco")
        void generateDocumentHtml_shouldHandleWhitespaceInFields() {
            // Arrange
            Client clientWithWhitespace = new Client();
            clientWithWhitespace.setFullName("   João Silva   ");
            clientWithWhitespace.setCpf("  12345678900  ");
            clientWithWhitespace.setNationality("  Brasileiro  ");
            clientWithWhitespace.setMaritalStatus(" Solteiro ");

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(clientWithWhitespace));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplate));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            // O serviço mantém os espaços como estão nos dados
            assertTrue(result.contains("Nome:    João Silva   "));
        }
    }

    // ========== TESTES DE CASOS EDGE ==========

    @Nested
    @DisplayName("Casos Edge - Situações Extremas")
    class EdgeCases {

        @Test
        @DisplayName("Deve processar template vazio corretamente")
        void generateDocumentHtml_shouldProcessEmptyTemplate() {
            // Arrange
            Template emptyTemplate = new Template();
            emptyTemplate.setHtmlContent("");

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(emptyTemplate));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            assertEquals("", result);
        }

        @Test
        @DisplayName("Deve processar template com apenas texto (sem placeholders)")
        void generateDocumentHtml_shouldProcessTemplateWithoutPlaceholders() {
            // Arrange
            Template noPlaceholderTemplate = new Template();
            noPlaceholderTemplate.setHtmlContent("<html><body><p>Este é um documento sem placeholders</p></body></html>");

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(noPlaceholderTemplate));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            assertEquals("<html><body><p>Este é um documento sem placeholders</p></body></html>", result);
        }

        @Test
        @DisplayName("Deve processar template com placeholders que parecem ser mas não são (typos)")
        void generateDocumentHtml_shouldNotReplaceMisspelledPlaceholders() {
            // Arrange
            Template typoTemplate = new Template();
            typoTemplate.setHtmlContent(
                "<html><body>{{NOME_COMPLETO_ERRADO}} e {{NOME_COMPLETO}}</body></html>"
            );

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(typoTemplate));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            // Apenas o placeholder correto é substituído
            assertEquals("<html><body>{{NOME_COMPLETO_ERRADO}} e João Silva</body></html>", result);
        }

        @Test
        @DisplayName("Deve processar template com caracteres especiais HTML nos dados")
        void generateDocumentHtml_shouldProcessSpecialHtmlCharactersInData() {
            // Arrange
            Client clientWithSpecialChars = new Client();
            clientWithSpecialChars.setFullName("João Silva & Cia. <Ltd.>");
            clientWithSpecialChars.setCpf("123.456.789-00");
            clientWithSpecialChars.setNationality("Brasileiro");
            clientWithSpecialChars.setMaritalStatus("Casado/a");

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(clientWithSpecialChars));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplate));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            // Os caracteres especiais são mantidos como estão
            assertTrue(result.contains("João Silva & Cia. <Ltd.>"));
            assertTrue(result.contains("123.456.789-00"));
        }

        @Test
        @DisplayName("Deve processar template com quebras de linha e caracteres especiais")
        void generateDocumentHtml_shouldProcessTemplateWithLineBreaksAndSpecialChars() {
            // Arrange
            Template templateWithLineBreaks = new Template();
            templateWithLineBreaks.setHtmlContent(
                "<html>\n<body>\n" +
                "Nome: {{NOME_COMPLETO}}\n" +
                "CPF: {{CPF}}\n" +
                "</body>\n</html>"
            );

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(templateWithLineBreaks));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            assertTrue(result.contains("Nome: João Silva"));
            assertTrue(result.contains("CPF: 12345678900"));
        }

        @Test
        @DisplayName("Deve processar template com placeholders case-sensitive")
        void generateDocumentHtml_shouldBeCaseSensitiveForPlaceholders() {
            // Arrange
            Template caseSensitiveTemplate = new Template();
            caseSensitiveTemplate.setHtmlContent(
                "<html><body>" +
                "{{nome_completo}} vs {{NOME_COMPLETO}} vs {{Nome_Completo}}" +
                "</body></html>"
            );

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(caseSensitiveTemplate));

            // Act
            String result = documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            // Apenas o placeholder com case correto é substituído
            assertEquals(
                "<html><body>" +
                "{{nome_completo}} vs João Silva vs {{Nome_Completo}}" +
                "</body></html>",
                result
            );
        }
    }

    // ========== TESTES DE INTEGRIDADE ==========

    @Nested
    @DisplayName("Integridade e Validações")
    class Integrity {

        @Test
        @DisplayName("Não deve modificar o objeto Client durante a geração")
        void generateDocumentHtml_shouldNotModifyClientObject() {
            // Arrange
            String originalFullName = validClient.getFullName();
            String originalCpf = validClient.getCpf();

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplate));

            // Act
            documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            assertEquals(originalFullName, validClient.getFullName());
            assertEquals(originalCpf, validClient.getCpf());
        }

        @Test
        @DisplayName("Não deve modificar o objeto Template durante a geração")
        void generateDocumentHtml_shouldNotModifyTemplateObject() {
            // Arrange
            String originalContent = validTemplate.getHtmlContent();

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplate));

            // Act
            documentGenerationService.generateDocumentHtml(clientId, templateId);

            // Assert
            assertEquals(originalContent, validTemplate.getHtmlContent());
        }

        @Test
        @DisplayName("Deve retornar resultado diferente para clientes diferentes")
        void generateDocumentHtml_shouldReturnDifferentResultsForDifferentClients() {
            // Arrange
            UUID clientId2 = UUID.randomUUID();
            Client client2 = new Client();
            client2.setFullName("Maria Santos");
            client2.setCpf("98765432100");
            client2.setNationality("Brasileira");
            client2.setMaritalStatus("Casada");

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(validClient));
            when(clientRepository.findById(clientId2)).thenReturn(Optional.of(client2));
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplate));

            // Act
            String result1 = documentGenerationService.generateDocumentHtml(clientId, templateId);
            String result2 = documentGenerationService.generateDocumentHtml(clientId2, templateId);

            // Assert
            assertNotEquals(result1, result2);
            assertTrue(result1.contains("João Silva"));
            assertTrue(result2.contains("Maria Santos"));
        }
    }
}
