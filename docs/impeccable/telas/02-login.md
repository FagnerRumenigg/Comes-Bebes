# Tela 02 — Login

Arquivos analisados: `frontend/src/views/LoginView.vue` e `frontend/src/app/layouts/AuthLayout.vue`

## Status

- Critique: concluído
- Visual: aguardando o critique das demais telas
- Texto, cor e identidade: aguardando etapa visual

## Direção do produto

- Preservar as cores primárias existentes.
- Reforçar a ideia de comida como conteúdo, com interação social mínima.
- A marca deve comunicar principalmente memória e registro.

## Detector

Nenhum alerta automatizado encontrado no componente Vue analisado.

## Impressão geral

A tela é limpa, acessível e funcional. O fluxo de autenticação está bem coberto, incluindo erros, carregamento, biometria, recuperação de senha e cadastro. Visualmente, porém, ela poderia comunicar mais singularidade do Comes&Bebes e depender menos de uma apresentação genérica de formulário.

## O que está funcionando

- Hierarquia clara entre saudação, campos e ação principal.
- Campos com autocomplete, labels e mensagens de erro.
- Biometria aparece apenas quando há suporte e contexto adequado no celular.
- O erro geral não revela se o e-mail ou a senha estão incorretos, preservando segurança.
- O link de recuperação e o cadastro estão próximos do fluxo principal.

## Prioridades

### [P1] A proposta do produto aparece pouco

“Continue compartilhando suas descobertas culinárias” é correto, mas genérico. A tela de entrada deveria conectar o login à memória, ao registro e à descoberta de comida.

### [P1] A composição editorial e o formulário parecem mundos separados

O `AuthLayout` apresenta “A arte de saborear histórias”, enquanto o formulário usa linguagem funcional. Falta uma ponte mais forte entre memória culinária e ação de entrar.

### [P2] Muitos caminhos secundários competem com o login

Biometria, usar senha, lembrar dispositivo, recuperar senha, credenciais de mock e cadastro podem aumentar a carga visual, especialmente em telas menores.

### [P2] O bloco de credenciais de mock precisa ser inequivocamente provisório

Mesmo restrito a desenvolvimento, ele pode parecer parte do produto. Deve ficar visualmente separado ou ser removido em capturas e revisões de interface.

### [P2] O login poderia ter mais personalidade sem perder sobriedade

A estrutura é segura, mas o formulário poderia usar detalhes relacionados a arquivo, caderno de receitas ou registro de experiências, alinhados à direção da marca.

## Personas

- **Jordan, primeira visita:** entende a ação principal, mas pode não compreender imediatamente o que torna o Comes&Bebes diferente.
- **Sam, usuário recorrente:** encontra rapidamente os campos e recuperação de senha; a biometria é útil quando disponível.
- **Pessoa que gosta de cozinhar:** recebe uma promessa culinária, mas ainda não sente com clareza que está voltando ao seu registro de comidas e descobertas.

## Próximas etapas

1. Concluir critique das demais telas.
2. Revisar composição e conexão entre painel editorial e formulário.
3. Reduzir ou reorganizar caminhos secundários.
4. Revisar textos, cores secundárias e detalhes de marca na etapa final.
