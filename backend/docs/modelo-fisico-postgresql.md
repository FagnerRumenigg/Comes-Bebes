# ComeSebebes — Modelo físico PostgreSQL

## Escopo

Modelo físico aprovado para PostgreSQL 17. O banco mantém integridade estrutural; autorização, transições de estado, limites temporais e fluxos transacionais ficam na aplicação.

## Diagrama ER

```mermaid
erDiagram
    users ||--o{ publications : creates
    publications ||--o{ publication_image_checks : validates
    publications ||--o| recipes : extends
    recipes ||--o{ recipe_ingredients : contains
    publications ||--o| publication_origins : derives
    recipes ||--o{ publication_origins : inspires
    reaction_types ||--o{ publication_reactions : classifies
    users ||--o{ publication_reactions : applies
    publications ||--o{ publication_reactions : receives
    users ||--o{ saved_publications : saves
    publications ||--o{ saved_publications : is_saved
    report_reasons ||--o{ reports : classifies
    users ||--o{ reports : submits
    publications ||--o{ reports : receives
    moderation_cases ||--o{ reports : groups
    publications ||--o{ moderation_cases : reviewed
    users ||--o{ moderation_cases : reviews
    users ||--o{ user_notifications : receives
    moderation_cases ||--o{ user_notifications : originates
```

## Decisões físicas

- Entidades principais usam `uuid`; catálogos usam `smallint identity`.
- Datas usam `timestamptz` e o valor padrão é `now()`.
- `recipes` usa somente `deleted_at` para desativação lógica.
- `publications.status` é a fonte principal do estado; `deleted_at` registra exclusão lógica.
- Não existem triggers, funções ou regras de autorização no banco.
- A origem de uma `MY_VERSION` usa `ON DELETE SET NULL`, preservando o snapshot do título.
- Imagens finais são WebP no GCS; o PostgreSQL guarda apenas a referência e os metadados.
- `Fiz também` não é catálogo de reação: é uma ação que cria uma origem de `MY_VERSION`.

## Migrations

1. `V1__create_users_and_app_config.sql`
2. `V2__create_publications_and_recipes.sql`
3. `V3__create_reactions_and_saved_publications.sql`
4. `V4__create_reports_and_moderation.sql`
5. `V5__create_indexes.sql`
6. `V6__seed_reaction_types_and_report_reasons.sql`
