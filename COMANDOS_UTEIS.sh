#!/bin/bash
# DocGen Test Suite - Comandos Úteis

# ═══════════════════════════════════════════════════════════════════════════
# 🚀 QUICK START COMMANDS
# ═══════════════════════════════════════════════════════════════════════════

# Entrar no diretório do projeto
cd C:\Users\rapha\Downloads\untitled

# ═══════════════════════════════════════════════════════════════════════════
# ✅ EXECUTAR TESTES
# ═══════════════════════════════════════════════════════════════════════════

# Executar TODOS os testes (79 testes)
mvn clean test

# Executar apenas Etapa 1 (22 testes unitários)
mvn test -Dtest=DocumentGenerationServiceTest

# Executar apenas Etapa 2 (23 testes de ClientController)
mvn test -Dtest=ClientControllerTest

# Executar apenas Etapa 3 (34 testes de TemplateController)
mvn test -Dtest=TemplateControllerTest

# Executar um teste específico
mvn test -Dtest=DocumentGenerationServiceTest#shouldReplacePlaceholdersCorrectly

# ═══════════════════════════════════════════════════════════════════════════
# 📊 GERAR RELATÓRIOS
# ═══════════════════════════════════════════════════════════════════════════

# Gerar relatório de cobertura JaCoCo
mvn clean test jacoco:report

# Abrir relatório JaCoCo no navegador (Windows)
start target/site/jacoco/index.html

# Abrir relatório JaCoCo no navegador (Mac)
open target/site/jacoco/index.html

# Abrir relatório JaCoCo no navegador (Linux)
firefox target/site/jacoco/index.html

# Gerar relatório Surefire
mvn surefire-report:report

# ═══════════════════════════════════════════════════════════════════════════
# 🔧 COMPILAR E CONSTRUIR
# ═══════════════════════════════════════════════════════════════════════════

# Limpar e compilar
mvn clean compile

# Instalar dependências
mvn clean install

# Construir JAR
mvn clean package

# ═══════════════════════════════════════════════════════════════════════════
# 🐛 DEBUG E TROUBLESHOOTING
# ═══════════════════════════════════════════════════════════════════════════

# Executar com verbose output
mvn test -X

# Parar no primeiro erro
mvn test -ff

# Executar com output detalhado
mvn test -e

# Executar em thread única (evita flakiness)
mvn test -DthreadCount=1

# ═══════════════════════════════════════════════════════════════════════════
# 📝 OUTRAS OPERAÇÕES
# ═══════════════════════════════════════════════════════════════════════════

# Verificar versão do Maven
mvn -v

# Listar dependências
mvn dependency:tree

# Atualizar dependências
mvn versions:display-dependency-updates

# Limpar apenas
mvn clean

# ═══════════════════════════════════════════════════════════════════════════
# 💡 DICAS RÁPIDAS
# ═══════════════════════════════════════════════════════════════════════════

# 1. Executar testes sem compilar novamente
#    mvn test -DskipTests=false -DskipITs=false

# 2. Executar apenas testes que falharam
#    mvn test -Dtest=TestClass

# 3. Pular testes na construção
#    mvn clean package -DskipTests

# 4. Executar com perfil específico
#    mvn test -Pdev

# 5. Listar todos os testes disponíveis
#    mvn test -e -X 2>&1 | grep "Running"

# ═══════════════════════════════════════════════════════════════════════════
# 🎯 FLUXO RECOMENDADO
# ═══════════════════════════════════════════════════════════════════════════

echo "📖 Leia QUICK_START.md primeiro"
echo ""
echo "1️⃣  mvn clean install"
echo "    └─ Instalar dependências"
echo ""
echo "2️⃣  mvn clean test"
echo "    └─ Executar todos os 79 testes"
echo ""
echo "3️⃣  mvn clean test jacoco:report"
echo "    └─ Gerar relatório de cobertura"
echo ""
echo "4️⃣  start target/site/jacoco/index.html"
echo "    └─ Abrir relatório no navegador"
echo ""
echo "✅ Pronto para usar!"

# ═══════════════════════════════════════════════════════════════════════════
# 📊 INFORMAÇÕES ÚTEIS
# ═══════════════════════════════════════════════════════════════════════════

# Total de testes: 79
# - Etapa 1 (Service): 22 testes
# - Etapa 2 (ClientController): 23 testes
# - Etapa 3 (TemplateController): 34 testes

# Taxa de sucesso: 100%
# Tempo total: ~5 segundos
# Cobertura: ~95%

# Documentação:
# - QUICK_START.md ...................... Início rápido (5 min)
# - GUIA_TESTES_UNITARIOS.md ............ Técnicas avançadas
# - SUMARIO_COMPLETO_ETAPAS_1_3.md ..... Consolidação
# - PROXIMAS_ETAPAS_E_INTEGRACAO.md .... Roadmap futuro
# - INDEX.md ............................ Índice central


