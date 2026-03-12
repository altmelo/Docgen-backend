-- V1__Esquema_Inicial_PtBr.sql
-- Esquema inicial do banco de dados em Português Brasileiro

CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    cpf VARCHAR(11) UNIQUE NOT NULL,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    perfil VARCHAR(50) NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clientes (
    id UUID PRIMARY KEY,
    cpf VARCHAR(11) UNIQUE NOT NULL,
    nome_completo VARCHAR(255) NOT NULL,
    nacionalidade VARCHAR(100) NOT NULL,
    estado_civil VARCHAR(50) NOT NULL,
    endereco JSONB,
    criado_por UUID REFERENCES usuarios(id),
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_exclusao TIMESTAMP
);

CREATE TABLE modelos_documentos (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    conteudo_html TEXT NOT NULL,
    variaveis JSONB,
    ativo BOOLEAN DEFAULT TRUE,
    criado_por UUID REFERENCES usuarios(id),
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_exclusao TIMESTAMP
);

CREATE TABLE geracoes_pdf (
    id UUID PRIMARY KEY,
    cliente_id UUID REFERENCES clientes(id),
    modelo_id UUID REFERENCES modelos_documentos(id),
    gerado_por UUID REFERENCES usuarios(id),
    data_geracao TIMESTAMP NOT NULL,
    url_pdf VARCHAR(500),
    dados_snapshot JSONB,
    configuracoes_aplicadas JSONB
);

CREATE TABLE configuracoes (
    id UUID PRIMARY KEY,
    chave VARCHAR(100) UNIQUE NOT NULL,
    valor JSONB,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    atualizado_por UUID REFERENCES usuarios(id)
);
