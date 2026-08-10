# Rede Social de Comida — Regras de Negócio

> Documento vivo para definição funcional do produto antes da implementação.

## 1. Controle do documento

| Campo | Valor |
|---|---|
| Status | Em descoberta |
| Versão | 0.4 |
| Última atualização | 07/08/2026 |
| Responsáveis | Fagner e ChatGPT |

### Convenções

- **Definido:** decisão aprovada e tratada como regra do produto.
- **Proposta:** solução sugerida, ainda aguardando aprovação.
- **Em aberto:** decisão necessária antes ou durante o desenvolvimento.
- **Futuro:** ideia deliberadamente fora do MVP.

---

## 2. Visão do produto

Criar uma rede social focada em comida, na qual as pessoas possam compartilhar fotos de pratos e receitas em um ambiente positivo, sem a pressão e os conflitos comuns de redes sociais tradicionais.

### 2.1 Proposta de valor

- A comida é a protagonista do conteúdo.
- A interação deve estimular apreciação, inspiração e vontade de cozinhar.
- A pessoa deve se sentir confortável para publicar o que preparou ou comeu.
- O produto deve evitar mecanismos que incentivem conflito, julgamento pessoal ou competição vazia por popularidade.

### 2.2 Princípios do produto — definidos

1. Não haverá comentários em publicações.
2. Não haverá conversas entre usuários no MVP.
3. Não serão permitidas fotos de pessoas como conteúdo principal.
4. O conteúdo principal será relacionado a comida.
5. As interações com publicações ainda serão definidas, mas deverão respeitar a proposta de ambiente positivo.
6. Nutrição é uma possibilidade futura e não faz parte do escopo inicial.

### 2.3 Fora do propósito

- Ser uma rede social geral com comida como apenas uma categoria.
- Favorecer exposição da vida pessoal dos usuários.
- Criar espaços de discussão ou debate nas publicações.
- Oferecer aconselhamento nutricional no MVP.

---

## 3. Público e atores

### 3.1 Usuário visitante — definido

Pessoa sem autenticação que pode conhecer a proposta do produto e visualizar publicações marcadas como **Públicas**. Publicações **Internas** não serão exibidas para visitantes.

### 3.2 Usuário cadastrado — definido

Pessoa autenticada que pode publicar conteúdo, reagir, salvar receitas ou pratos e gerenciar suas próprias publicações.

### 3.3 Administrador — definido para o MVP

Usuário responsável por configurações gerais, análise de denúncias, remoção de conteúdo, restrição de contas e manutenção de cadastros auxiliares. Inicialmente esse papel será exercido somente pelo proprietário do produto.

### 3.4 Perfis de acesso — definido para o MVP

O sistema terá inicialmente apenas dois perfis de acesso:

- **USER:** perfil padrão criado para os usuários da rede.
- **ADMIN:** perfil administrativo criado para o proprietário do produto, acumulando também as funções de moderação.

Não haverá um perfil separado de moderador no MVP.

---

## 4. Conceitos do domínio

### 4.1 Publicação

Conteúdo criado por um usuário e exibido na rede. Deve estar relacionado a comida e conter ao menos uma imagem válida.

### 4.2 Prato — definido

Publicação voltada a compartilhar uma comida, sem obrigatoriedade de informar receita.

### 4.3 Receita — definido

Publicação que, além da foto e identificação do prato, contém ingredientes, modo de preparo e informações de rendimento.

### 4.4 Minha versão — definido

Publicação criada por alguém que preparou sua própria versão de uma receita já existente. Ela permanece vinculada à publicação de origem.

### 4.5 Reação — definido parcialmente

Interação estruturada e positiva com uma publicação. Os tipos iniciais foram definidos, mas o conceito ainda será revisitado após a primeira validação deste documento.

### 4.6 Item salvo — definido

Prato ou receita guardado privadamente pelo usuário para consultar ou preparar depois.

### 4.7 Visibilidade da publicação — definido

Determina quem pode visualizar uma publicação:

- **Pública:** visível para visitantes e usuários autenticados.
- **Interna:** visível apenas para usuários autenticados.

O termo **Privada** não será usado para conteúdo visível a todos os usuários autenticados. Ele fica reservado para uma possível visibilidade futura exclusiva ao próprio autor.

---

## 5. Regras de publicação

### RN-PUB-001 — Assunto textual opcional — definido

A publicação não precisa conter assunto, título ou texto descritivo. A obrigatoriedade de conteúdo de comida será garantida pela imagem.

### RN-PUB-002 — Imagem obrigatória — definido

Toda publicação deve conter ao menos uma imagem de comida.

### RN-PUB-003 — Ausência de pessoas — definido

Publicações não poderão conter pessoas, inclusive de forma incidental ao fundo. Imagens com pessoas deverão ser bloqueadas. A forma técnica de detecção e bloqueio será definida posteriormente.

### RN-PUB-004 — Tipos de publicação — definido

Uma publicação pode ser classificada como **Prato**, **Receita** ou **Minha versão**.

### RN-PUB-005 — Receita opcional — definido

O usuário pode publicar uma foto de comida sem cadastrar uma receita completa.

### RN-PUB-006 — Autoria — definido

Somente o autor pode editar ou excluir sua publicação, exceto em ações de moderação.

### RN-PUB-007 — Edição e histórico — definido para o MVP

O autor poderá editar os dados textuais e os dados da receita, mas não poderá substituir a imagem da publicação. O MVP não manterá histórico de edições. Publicações derivadas apontarão para o estado atual da receita de origem.

### RN-PUB-008 — Quantidade de imagens — definido para o MVP

Cada publicação aceitará somente uma imagem.

### RN-PUB-009 — Conteúdo comercial — definido por fase

Conteúdo de restaurantes, marcas, criadores profissionais e publicações patrocinadas não fará parte do MVP. Esse conteúdo poderá ser permitido futuramente e é uma das possibilidades de monetização do produto.

### RN-PUB-010 — Escolha de visibilidade — definido

Ao publicar, o autor deverá escolher entre visibilidade **Pública** e **Interna**. A interface deverá explicar que a diferença é o acesso por pessoas sem cadastro: usuários autenticados poderão visualizar ambos os tipos.

---

## 6. Regras de receita

### RN-REC-001 — Dados mínimos — definido

Uma receita deve conter título, ingredientes, modo de preparo e ao menos uma imagem.

### RN-REC-002 — Ingredientes estruturados — definido

Cada ingrediente deve permitir nome, quantidade, unidade e observação. A estrutura deve possibilitar cálculo nutricional futuro sem exigir esse recurso no MVP.

### RN-REC-003 — Rendimento — definido

A receita pode informar quantidade de porções ou unidades produzidas.

### RN-REC-004 — Autoria declarada — definido para o MVP

No MVP, toda receita publicada será declarada pelo usuário como criação própria. Receitas tradicionais, adaptações, cópias e mecanismos de atribuição à fonte original não terão tratamento específico nesta fase e deverão ser discutidos futuramente.

### RN-REC-005 — Alterações por terceiros — definido

Um usuário não edita a receita de outro. Para registrar alterações, cria uma publicação do tipo **Minha versão**.

### RN-REC-006 — Vínculo entre versões — definido

Uma publicação **Minha versão** deve referenciar sua receita de origem e pode informar substituições ou mudanças de forma estruturada.

---

## 7. Interações sociais

### RN-SOC-001 — Comentários — definido

Publicações não aceitam comentários.

### RN-SOC-002 — Conversas privadas — definido para o MVP

O MVP não terá mensagens diretas nem chat entre usuários.

### RN-SOC-003 — Reações positivas — definido

Somente reações coerentes com um ambiente positivo serão oferecidas. Não haverá reação negativa ou ambígua que possa ser usada para constranger o autor.

### RN-SOC-004 — Reações distintas por usuário — definido

Um usuário pode aplicar várias reações diferentes à mesma publicação, mas não pode aplicar duas vezes a mesma reação. A combinação entre usuário, publicação e tipo de reação deve ser única.

### RN-SOC-005 — Exibição das contagens — definido

O usuário terá uma configuração para permitir ou ocultar a exibição pública das quantidades de reações em suas publicações. As quantidades serão agrupadas por tipo de reação. Por padrão, a exibição das contagens estará habilitada.

### RN-SOC-006 — Identidade de quem reagiu — definido

Não será possível consultar a identidade dos usuários que reagiram. Quando a exibição estiver habilitada pelo autor, somente os totais por tipo serão apresentados.

### RN-SOC-007 — Reações iniciais — definido para o MVP

- **Eu comeria:** apreciação do prato.
- **Quero fazer:** intenção de preparar; pode também salvar a receita.
- **Fiz também:** inicia uma publicação vinculada à receita original.
- **Comida afetiva:** o prato desperta memória ou sensação de conforto.

### RN-SOC-008 — Publicação derivada — definido

A ação **Fiz também** deve gerar conteúdo próprio e não apenas incrementar um contador.

### RN-SOC-009 — Seguidores — definido para o MVP

Não haverá relacionamento de seguidores no MVP. Um recurso futuro poderá permitir acompanhar temas, ingredientes ou tipos de prato, sem decisão tomada neste momento.

---

## 8. Perfil e identidade

### RN-PER-001 — Imagem de perfil — definido para o MVP

O perfil não terá foto, avatar ou ilustração. A identificação visual será feita somente pelo username.

### RN-PER-002 — Informações públicas — definido para o MVP

O perfil exibirá publicamente o nome, o username/apelido e as publicações do usuário. Não haverá biografia, localização, especialidades ou estatísticas no MVP.

### RN-PER-003 — Métricas pessoais — definido para o MVP

O perfil não exibirá métricas pessoais, totais de publicações, reações recebidas, popularidade ou quaisquer estatísticas agregadas.

---

## 9. Descoberta e feed

### RN-FEE-001 — Critério inicial do feed — definido para o MVP

O feed do MVP será cronológico, ordenado das publicações mais recentes para as mais antigas. Uma estratégia mais sofisticada de recomendação poderá substituir ou complementar esse modelo futuramente.

### RN-FEE-002 — Preferências — fora do MVP

O MVP não terá preferências por culinárias, ingredientes, restrições alimentares ou tipos de refeição, pois temas e categorias ainda não serão implementados.

### RN-FEE-003 — Segurança alimentar — fora do MVP

O MVP não terá filtros ou alertas específicos para alergias e restrições alimentares. O tema poderá ser avaliado em uma fase futura.

### RN-FEE-004 — Busca — definido para o MVP

O usuário poderá buscar publicações por título e por ingrediente. Categorias e marcadores não existirão no MVP.

### RN-FEE-005 — Ranking — fora do MVP

O MVP não terá ranking, seção de conteúdo em alta ou ordenação por popularidade.

---

## 10. Moderação e segurança

### RN-MOD-001 — Denúncia — definido

Usuários autenticados poderão denunciar publicações incompatíveis com as regras.

### RN-MOD-002 — Motivos de denúncia — definido inicialmente

- Não é conteúdo de comida.
- Contém pessoa identificável.
- Conteúdo ofensivo ou discriminatório.
- Spam ou propaganda não autorizada.
- Violação de autoria.
- Conteúdo perigoso ou ilegal.

### RN-MOD-003 — Fila de moderação por limite de denúncias — definido parcialmente

As denúncias serão registradas e contabilizadas. Uma publicação somente entrará na fila de validação do administrador ao alcançar um limite mínimo de denúncias. O valor desse limite ainda será definido e deverá ser configurável.

Alcançar o limite não removerá nem ocultará automaticamente a publicação. Após a análise, o administrador poderá manter, ocultar ou remover o conteúdo, além de advertir ou restringir a conta responsável.

### RN-MOD-004 — Alimentos controversos — definido para o MVP

O MVP não bloqueará preventivamente bebidas alcoólicas, caça, abate, animais inteiros, alimentos visualmente sensíveis ou outras comidas controversas. O conteúdo poderá ser denunciado e será avaliado pelo administrador conforme os motivos de denúncia aplicáveis.

### RN-MOD-005 — Conteúdo gerado por IA — definido parcialmente

Imagens de comida geradas por inteligência artificial não serão permitidas. A estratégia técnica de detecção e validação permanece em aberto.

### RN-MOD-006 — Unicidade da denúncia — proposta

Para impedir manipulação do limite de moderação, cada usuário poderá manter no máximo uma denúncia ativa por publicação. Esta regra ainda aguarda validação.

---

## 11. Nutrição — adiado

Nutrição não será tratada nesta etapa do planejamento nem fará parte do MVP. As regras relacionadas ao tema serão elaboradas somente quando esse escopo for retomado.

---

## 12. Escopo preliminar do MVP — proposta

### Incluído

- Cadastro e autenticação.
- Perfil básico sem foto pessoal.
- Publicação de uma foto de comida.
- Publicações dos tipos **Prato** e **Receita**.
- Escolha entre visibilidade **Pública** e **Interna**.
- Ingredientes e modo de preparo para receitas.
- Reações positivas estruturadas.
- Salvar pratos e receitas.
- Feed cronológico inicial.
- Busca básica.
- Denúncia e moderação manual.

### Não incluído

- Comentários.
- Mensagens diretas.
- Cálculo nutricional.
- Contas profissionais verificadas.
- Publicidade e monetização.
- Recomendação avançada por algoritmo.
- Reconhecimento automático de comida ou pessoas em imagens.
- Detecção automática de imagens geradas por inteligência artificial.
- Contas separadas de moderador.
- Seguidores de usuários.
- Conteúdo comercial ou patrocinado.

---

## 13. Decisões prioritárias

Estas decisões devem ser tomadas antes da modelagem definitiva do domínio:

1. ~~Receita será opcional ou obrigatória em toda publicação?~~ **Resolvido:** opcional.
2. ~~Existirão os três tipos **Prato**, **Receita** e **Minha versão**?~~ **Resolvido:** sim.
3. ~~Quais reações existirão no MVP?~~ **Resolvido inicialmente:** Eu comeria, Quero fazer, Fiz também e Comida afetiva.
4. ~~As quantidades e identidades das reações serão públicas?~~ **Resolvido:** somente totais, exibidos por padrão e configuráveis pelo autor.
5. ~~Haverá seguidores de usuários ou apenas descoberta por interesses?~~ **Resolvido para o MVP:** não haverá seguidores.
6. ~~O perfil usará avatar não pessoal ou ficará sem imagem?~~ **Resolvido:** ficará sem imagem e usará somente o username como identificação visual.
7. ~~Como tratar pessoas incidentais no fundo das fotos?~~ **Resolvido:** a imagem será bloqueada.
8. ~~O MVP aceitará conteúdo de restaurantes e marcas?~~ **Resolvido:** não; possibilidade futura de monetização.
9. ~~Uma publicação poderá ter uma ou várias imagens?~~ **Resolvido para o MVP:** uma imagem.
10. ~~O conteúdo será visível sem autenticação?~~ **Resolvido:** apenas publicações **Públicas**.

---

## 14. Registro de decisões

| ID | Data | Decisão | Motivo | Status |
|---|---|---|---|---|
| DEC-001 | 07/08/2026 | Não permitir comentários | Preservar um ambiente positivo e reduzir conflitos | Definido |
| DEC-002 | 07/08/2026 | Não oferecer conversas no MVP | Manter o foco no conteúdo de comida | Definido |
| DEC-003 | 07/08/2026 | Bloquear publicações que contenham pessoas, inclusive ao fundo | Manter a comida como protagonista | Definido |
| DEC-004 | 07/08/2026 | Deixar nutrição para uma fase futura | Reduzir o escopo inicial | Definido |
| DEC-005 | 07/08/2026 | Usar apenas os perfis USER e ADMIN no MVP | O administrador acumulará a moderação inicialmente | Definido |
| DEC-006 | 07/08/2026 | Adotar visibilidades Pública e Interna | Diferenciar o acesso de visitantes e usuários autenticados sem chamar conteúdo interno de privado | Definido |
| DEC-007 | 07/08/2026 | Permitir uma imagem por publicação no MVP | Controlar escopo e custo inicial | Definido |
| DEC-008 | 07/08/2026 | Permitir várias reações distintas, sem repetição do mesmo tipo | Viabilizar expressão positiva sem duplicação artificial | Definido |
| DEC-009 | 07/08/2026 | Exibir por padrão apenas totais de reações, com opção de ocultação pelo autor | Preservar privacidade e reduzir competição | Definido |
| DEC-010 | 07/08/2026 | Não implementar seguidores no MVP | Manter o foco inicial no conteúdo | Definido |
| DEC-011 | 07/08/2026 | Permitir edição de dados, mas não a substituição da foto | Preservar a identidade visual da publicação sem manter histórico no MVP | Definido |
| DEC-012 | 07/08/2026 | Usar feed cronológico no MVP | Entregar descoberta funcional antes de um algoritmo de recomendação | Definido |
| DEC-013 | 07/08/2026 | Limitar a busca a título e ingrediente | Categorias, temas e marcadores ficarão fora do MVP | Definido |
| DEC-014 | 07/08/2026 | Encaminhar conteúdo à fila administrativa após um limite de denúncias | Reduzir ruído de moderação sem remover conteúdo automaticamente | Definido parcialmente |
| DEC-015 | 07/08/2026 | Proibir imagens geradas por inteligência artificial | Manter o foco em comidas reais compartilhadas pelos usuários | Definido parcialmente |
| DEC-016 | 07/08/2026 | Adiar todas as regras de nutrição | Manter o planejamento atual concentrado no núcleo social e culinário | Definido |
| DEC-017 | 07/08/2026 | Manter o perfil sem imagem, biografia, localização, especialidades ou estatísticas | Criar uma identidade mínima e manter o foco nas publicações de comida | Definido |

---

## 15. Glossário em construção

| Termo provisório | Significado |
|---|---|
| Prato | Publicação de comida sem receita obrigatória |
| Receita | Publicação com ingredientes e preparo |
| Minha versão | Resultado preparado a partir de outra receita |
| Reação | Interação estruturada sem comentário |

> Os nomes dos conceitos são provisórios e podem ser substituídos pela linguagem própria da marca.
