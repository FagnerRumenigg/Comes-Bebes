# Comes&Bebes

O Comes&Bebes é uma rede social voltada ao compartilhamento de comidas e receitas. A aplicação permite publicar receitas, explorar o feed, buscar publicações, reagir, salvar conteúdos, acompanhar perfis e realizar a moderação da comunidade.

O projeto está organizado como um monorepo, reunindo a aplicação web, a API e um serviço responsável pela validação de imagens.

## Estrutura

```text
Comes-Bebes/
├── frontend/   # Aplicação web
└── backend/    # API, banco de dados e validador de imagens
```

## Tecnologias

### Frontend

- Vue 3 e TypeScript;
- Vite;
- Vue Router e Pinia;
- TanStack Vue Query e Axios;
- Orval para geração do cliente a partir do contrato OpenAPI;
- Vitest, Vue Test Utils e Playwright para testes;
- MSW para simulação da API durante o desenvolvimento.

### Backend

- Java 25 e Spring Boot 4;
- Spring Web, Spring Data JPA e Spring Security;
- autenticação com JWT;
- PostgreSQL e Flyway;
- OpenAPI/Swagger;
- Maven;
- Docker e Docker Compose.

### Validação de imagens

- Python 3.12;
- FastAPI;
- PyTorch, Transformers e modelos do Hugging Face;
- Pillow para processamento de imagens.

## Documentação

As instruções de configuração e execução ficam nos arquivos `README.md` de cada componente. Documentos de planejamento e controle do desenvolvimento permanecem somente no ambiente local.

> As credenciais e configurações locais devem ser mantidas em arquivos `.env`, que não são versionados no Git.
