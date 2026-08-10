# Contexto de desenvolvimento — ComeSebebes

Este arquivo reúne as convenções de desenvolvimento definidas para o projeto.

## Comandos de terminal

- Sempre que um comando precisar ser executado pelo usuário, ele deverá ser fornecido no formato compatível com Git Bash.

## Validação

- O projeto terá testes unitários.
- Cada service deverá possuir testes unitários correspondentes.
- Cada controller deverá possuir testes unitários correspondentes.
- Não é necessário executar os testes unitários ao final de toda alteração ou compilação.
- Os testes serão executados antes do deploy, sob responsabilidade do fluxo de validação do projeto.

## API

- A API seguirá o padrão RESTful.
- Cada tabela terá sua própria entidade, repositório e service.
- As controllers serão organizadas por contexto de negócio; não é obrigatório criar uma controller para cada tabela.

## Conta e autenticação

- No MVP, o cadastro e o login usam `username` e senha.
- O campo `email` permanece no banco, mas é opcional e não participa da autenticação.
- Não haverá validação, confirmação ou recuperação de e-mail no MVP.
- Usuários autenticados poderão alterar a senha informando a senha atual e a nova senha.
- O rate limiting do login deverá permanecer configurável por variáveis de ambiente.

## Swagger / OpenAPI

- O projeto utilizará Swagger para disponibilizar a documentação OpenAPI da API.
- Todo endpoint novo deverá ser documentado no momento da implementação.
- A documentação deverá explicar o objetivo do endpoint, o método HTTP, a rota, os parâmetros, o request, o response, os possíveis códigos HTTP e os erros esperados.
- Requests, responses e seus fields importantes deverão possuir descrição clara.
- Sempre que possível, requests, responses e fields deverão incluir exemplos práticos.
- Os exemplos devem permitir que uma pessoa entenda e teste o endpoint sem precisar solicitar explicações adicionais.
- A documentação deve permanecer consistente com o comportamento real da API.
- O Swagger UI deverá abrir com as controllers recolhidas por padrão.

## Packages

Os packages principais seguirão esta estrutura:

- `config`: configurações da aplicação.
- `controller`: controllers organizadas por contexto.
  - `request`: classes de entrada da API.
  - `response`: classes de saída da API.
- `service`: regras de aplicação e casos de uso.
- `repository`: repositórios de persistência.
- `model`: entidades e demais modelos centrais do domínio.
- `dto`: DTOs que não sejam específicos de request ou response.
- `util`: componentes utilitários reutilizáveis e específicos, como normalização de texto e conversão de datas.

## Records

- Classes de entrada em `controller.*.request` serão `record`.
- Classes de saída em `controller.*.response` serão `record`.
- DTOs em `dto` serão `record`.
- Sempre que fizer sentido, `record` e entity terão um método `of` para converter um objeto no outro.

## Lombok

- O projeto utilizará Lombok para reduzir código repetitivo.
- O padrão `@Builder` do Lombok será utilizado nas classes que precisarem construir objetos, inclusive como apoio aos métodos `of`.
- O uso de Lombok deve permanecer consistente com o tipo da classe e com as necessidades do JPA.

## Datas e timezone

- O timezone regional da aplicação será `America/Sao_Paulo`.
- Datas persistidas devem ser tratadas em UTC, usando `timestamptz` no PostgreSQL.
- A conversão para o timezone da aplicação acontece na apresentação da API.
- O projeto deve usar um `Clock` centralizado para facilitar testes determinísticos.
- Não devem ser usados offsets fixos como `UTC-3` no lugar do identificador regional.

## Organização por contexto

- A organização das controllers poderá agrupar operações relacionadas ao mesmo contexto de negócio.
- A separação entre request, response, DTO, entity, repository e service deve continuar clara mesmo quando uma controller atender mais de uma tabela.

## Ordem de implementação

- Quando o comando `prossiga` for usado, o bloco atual deverá ser concluído antes de avançar para o próximo.
- A integração com Spring Security deverá ser implementada antes da integração com GCS.
- GCS será o último grande bloco de infraestrutura da aplicação.

## Autenticação

- O cadastro pertence ao contexto de autenticação e é exposto em `POST /auth/register`.
- O login e o cadastro usam `username` e senha; o e-mail permanece apenas no banco e não participa dos contratos da API no MVP.
- O login emite um access token JWT e um refresh token opaco persistido somente como hash.
- Refresh tokens são rotacionados: o token usado em `POST /auth/refresh` é revogado antes da emissão do próximo par.
- `POST /auth/logout` revoga o refresh token informado.

## Repositories e queries

- As consultas dos repositories deverão usar primeiro o padrão de métodos derivados do Spring Data, como `findBy...`, `existsBy...` e `countBy...`.
- Quando uma consulta ficar muito complexa para métodos derivados, será permitido usar JPQL ou recursos JPA equivalentes.
- A escolha deve priorizar legibilidade, manutenção e clareza da intenção da consulta.
- Queries nativas SQL só deverão ser utilizadas quando houver uma necessidade técnica clara que não seja bem atendida pelo JPA.
