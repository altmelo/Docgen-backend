# Contribuindo

Obrigado por considerar contribuir com o DocGen Backend.

## Antes de começar

- Verifique se existe uma issue relacionada ao que você quer fazer.
- Para mudanças maiores, crie uma issue e descreva a proposta antes do pull request.

## Requisitos

- Java 17
- PostgreSQL 13+

## Setup local

1. Defina as variáveis necessárias (ver [README.md](file:///c:/Users/rapha/Downloads/untitled/README.md)).
2. Rode os testes:

```powershell
java "-Dmaven.multiModuleProjectDirectory=$PWD" -classpath .mvn/wrapper/maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain test
```

## Padrão de commits

Use Conventional Commits:

- `feat: ...` nova funcionalidade
- `fix: ...` correção de bug
- `chore: ...` manutenção, tooling, docs
- `test: ...` testes
- `refactor: ...` refatoração sem mudança de comportamento

Exemplos:

- `feat(auth): adicionar refresh token`
- `fix(templates): retornar 404 para template inexistente`
- `chore(ci): adicionar GitHub Actions`

## Branching e Pull Requests

- Crie branches a partir de `main`:
  - `feat/<descricao-curta>`
  - `fix/<descricao-curta>`
  - `chore/<descricao-curta>`
- Pull request deve incluir:
  - descrição objetiva do problema/solução
  - evidência de testes passando
  - impactos em API (se houver)

## Checklist do PR

- Testes locais passando
- Sem credenciais, tokens ou chaves no código
- Documentação atualizada (README e/ou exemplos de request)

## Segurança

Se encontrar vulnerabilidade, não abra issue pública. Envie detalhes por canal privado do time responsável.
