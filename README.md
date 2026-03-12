# DocGen Backend

Backend em Spring Boot para gerenciamento de clientes, modelos de documentos e geração de HTML a partir de placeholders.

## Requisitos

- Java 17+ (recomendado: 17).  
- PostgreSQL 13+.

## Variáveis de ambiente

- `SERVER_PORT` (default: `8080`)
- `DB_URL` (default: `jdbc:postgresql://localhost:5432/docgen_db`)
- `DB_USERNAME` (default: `docgen_user`)
- `DB_PASSWORD` (default: `password`)
- `CORS_ALLOWED_ORIGINS` (default: `http://localhost:3000,http://localhost:5173`)
- `JWT_SECRET` (obrigatória; Base64 com pelo menos 256 bits)
- `JWT_ACCESS_TOKEN_EXPIRATION_MS` (default: `900000`)
- `JWT_REFRESH_TOKEN_EXPIRATION_MS` (default: `604800000`)

Gerar um `JWT_SECRET` (PowerShell):

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

## Banco de dados

Migrações via Flyway em [db/migration](file:///c:/Users/rapha/Downloads/untitled/src/main/resources/db/migration).

Tabelas principais (V1):

- `usuarios`: `id`, `cpf`, `nome`, `email`, `perfil`, `data_criacao`, `data_atualizacao`
- `clientes`: `id`, `cpf`, `nome_completo`, `nacionalidade`, `estado_civil`, `endereco`, `criado_por`, `data_criacao`, `data_atualizacao`, `data_exclusao`
- `modelos_documentos`: `id`, `nome`, `conteudo_html`, `variaveis`, `ativo`, `criado_por`, `data_criacao`, `data_atualizacao`, `data_exclusao`
- `geracoes_pdf`: `id`, `cliente_id`, `modelo_id`, `gerado_por`, `data_geracao`, `url_pdf`, `dados_snapshot`, `configuracoes_aplicadas`
- `configuracoes`: `id`, `chave`, `valor`, `data_atualizacao`, `atualizado_por`

Atualizações (V2):

- `usuarios.senha_hash` (não-nulo; default vazio)
- constraint `uk_usuarios_email` (email único)

## Executar localmente

1. Suba um PostgreSQL local e crie o banco/usuário conforme as variáveis acima.
2. Defina `JWT_SECRET` e (opcionalmente) `DB_*`.
3. Suba a aplicação.

### Rodar com Maven Wrapper (PowerShell)

Se não tiver `mvn` instalado, execute o wrapper via Java:

```powershell
java "-Dmaven.multiModuleProjectDirectory=$PWD" -classpath .mvn/wrapper/maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain spring-boot:run
```

### Rodar testes

```powershell
java "-Dmaven.multiModuleProjectDirectory=$PWD" -classpath .mvn/wrapper/maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain test
```

## Autenticação

Fluxo:

1. `POST /api/auth/register` cria usuário e retorna `accessToken` e `refreshToken`.
2. `POST /api/auth/login` autentica e retorna tokens.
3. `POST /api/auth/refresh` recebe `{ "refreshToken": "..." }` e retorna novos tokens.

Header para rotas protegidas:

- `Authorization: Bearer <accessToken>`

Rotas públicas:

- `/api/auth/**`

Demais rotas exigem autenticação (ver [SecurityConfig](file:///c:/Users/rapha/Downloads/untitled/src/main/java/com/docgen/auth/config/SecurityConfig.java)).

## Endpoints da API (principais)

Base URL padrão: `http://localhost:8080`

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`

Exemplos:

```json
POST /api/auth/register
{
  "name": "João Silva",
  "email": "joao@exemplo.com",
  "cpf": "12345678900",
  "password": "SenhaForte@123",
  "role": "Operador"
}
```

```json
POST /api/auth/login
{
  "email": "joao@exemplo.com",
  "password": "SenhaForte@123"
}
```

```json
POST /api/auth/refresh
{
  "refreshToken": "..."
}
```

### Clientes (`/api/v1/clients`)

- `GET /api/v1/clients`
- `GET /api/v1/clients/{id}`
- `POST /api/v1/clients`
- `PUT /api/v1/clients/{id}`
- `DELETE /api/v1/clients/{id}`
- `GET /api/v1/clients/{id}/documents?templateId={templateId}` (retorna HTML)

Exemplo (criar cliente):

```json
POST /api/v1/clients
{
  "cpf": "12345678900",
  "fullName": "João Silva",
  "nationality": "Brasileiro",
  "maritalStatus": "Solteiro",
  "address": {
    "logradouro": "Rua A",
    "numero": "123",
    "bairro": "Centro",
    "cidade": "São Paulo",
    "uf": "SP",
    "cep": "01000-000"
  }
}
```

### Templates (`/api/v1/templates`)

- `GET /api/v1/templates`
- `GET /api/v1/templates/active`
- `GET /api/v1/templates/{id}`
- `POST /api/v1/templates`
- `PUT /api/v1/templates/{id}`
- `DELETE /api/v1/templates/{id}`
- `GET /api/v1/templates/{id}/preview?clientId={clientId}` (retorna HTML)

Exemplo (criar template):

```json
POST /api/v1/templates
{
  "name": "Contrato Padrão",
  "htmlContent": "<html><body>Nome: {{NOME_COMPLETO}}, CPF: {{CPF}}</body></html>",
  "isActive": true
}
```

## Segurança e compartilhamento com frontend

- Não coloque tokens, senhas ou secrets no repositório.
- Compartilhe credenciais somente por canal seguro (ex.: gerenciador de senhas) e prefira credenciais temporárias de desenvolvimento.
- Antes de fazer push, revise histórico e arquivos locais por `.env`, chaves (`*.pem`, `*.p12`) e configs de produção.
