# Tela 04 — Navegação

Arquivo de referência: `docs/telas/04-navegacao.html`

## Status

- Critique: concluído
- P1 — recorte de menus: concluído
- Visual: aguardando o critique das demais telas
- Texto, cor e identidade: aguardando etapa visual

## Detector

- **P1:** container `.device` com `overflow: hidden` pode cortar menus posicionados.
- **P3:** borda fina combinada com sombra ampla em duas superfícies.
- **P2:** fontes comuns: `Inter` e `Instrument Serif`.

## Impressão geral

A navegação está bem pensada para web e celular, com adaptação clara entre header e barra inferior. A ação “Publicar” recebe destaque correto e os avisos têm contagem visível. O principal risco está na quantidade de destinos e na complexidade do menu de conta, além de um problema técnico potencial no recorte de menus.

## O que está funcionando

- A arquitetura desktop/mobile é explícita e coerente.
- “Publicar” tem peso de ação principal.
- O estado ativo usa cor, peso e indicador, não apenas cor.
- A barra inferior mantém alvos de toque confortáveis.
- O tema está nomeado como “Aparência”, mais claro que um ícone isolado.

## Prioridades

### [P0] Corrigir o recorte do menu de conta

`.device` usa `overflow: hidden` e envolve elementos posicionados. Isso pode cortar o menu desktop ou qualquer popover que precise ultrapassar os limites do dispositivo.

### [P1] Reduzir a carga do menu de conta

Perfil, configurações, ajuda, aparência e sair ficam no mesmo menu. A organização é lógica, mas pode exigir leitura demais para uma ação frequente.

### [P1] Validar a prioridade dos cinco itens mobile

Início, Salvos, Publicar, Avisos e Perfil cobrem o essencial, mas “Salvos” e “Avisos” podem disputar atenção com a ação de publicar. A ordem precisa refletir os hábitos reais do produto.

### [P2] Tornar a navegação mais ligada a comida e registro

A navegação é funcional, porém neutra. O diferencial de registrar e descobrir comida aparece pouco nos nomes e na apresentação dos destinos.

### [P2] Garantir comportamento completo de acessibilidade

O protótipo demonstra `aria-current`, `aria-expanded` e Escape, mas a implementação final deve garantir foco no menu, retorno de foco e fechamento correto para teclado e leitor de tela.

### [P3] Rever bordas, sombras e tipografia

O detector apontou padrões visuais genéricos. São alertas consultivos, não falhas críticas.

## Personas

- **Alex, usuário recorrente:** encontra rapidamente publicar e o feed, mas pode sentir que ações importantes estão escondidas no menu.
- **Jordan, primeira visita:** entende Início e Perfil, mas pode não entender imediatamente Salvos e Avisos.
- **Pessoa no celular:** recebe bons alvos de toque, mas precisa de um menu de conta que não cubra excessivamente o conteúdo.

## Próximas etapas

1. Corrigir o recorte do menu.
2. Validar a ordem e nomenclatura da navegação com os fluxos reais.
3. Revisar foco, teclado e comportamento do bottom sheet.
4. Fazer a etapa visual e depois revisar textos, cores secundárias e identidade.
