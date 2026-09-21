# Tela 03 — Carregando

Arquivo de referência: `docs/telas/03-carregando.html`

## Status

- Critique: concluído
- Visual: aguardando o critique das demais telas
- Texto, cor e identidade: aguardando etapa visual

## Detector

- **P3:** borda fina combinada com sombra ampla.
- **P2:** fontes comuns: `Inter` e `Instrument Serif`.

## Impressão geral

A tela tem uma metáfora própria — a cafeteira aquecendo — e trata o tempo de espera com honestidade. Os estágios progressivos são uma boa decisão de produto. O ponto de atenção é garantir que a espera não pareça artificialmente longa ou que a animação substitua informação real do sistema.

## O que está funcionando

- A cafeteira cria personalidade e conversa diretamente com comida e bebida.
- A mensagem muda conforme o tempo, mantendo a pessoa informada.
- Não usa barra de progresso falsa.
- Depois de 25 segundos oferece uma ação de recuperação.
- O movimento reduzido e `prefers-reduced-motion` foram considerados.
- A falha final oferece tentar novamente ou voltar.

## Prioridades

### [P1] Diferenciar espera real de demonstração temporal

Os estágios são baseados em tempo, mas a interface precisa refletir o estado real da operação. Uma conexão que falha instantaneamente não deve esperar 45 segundos para informar o problema.

### [P1] Evitar prometer que a tela precisa ficar aberta

“Pode deixar a tela aberta” pode ser inadequado em dispositivos móveis ou quando a operação continua em segundo plano.

### [P2] O contador pode aumentar ansiedade

Mostrar `0s`, `10s`, `25s` e `45s` torna a demora muito explícita. Isso pode ser útil em diagnóstico, mas nem sempre ajuda a experiência comum.

### [P2] A ação de cancelar aparece tarde

“Voltar” só aparece no estágio final. Em operações longas, o usuário deveria ter uma saída antes, quando isso for tecnicamente seguro.

### [P2] A metáfora poderia reforçar memória e registro

A cafeteira comunica preparo, mas não comunica diretamente a ideia de guardar ou recuperar uma experiência culinária. A animação é boa, porém a mensagem pode aproximá-la mais do posicionamento do produto.

### [P3] Superfície visual genérica

O detector apontou borda fina com sombra ampla e fontes comuns. São alertas consultivos, não falhas críticas.

## Personas

- **Jordan, primeira visita:** entende a metáfora, mas pode não saber se está carregando uma página, uma publicação ou uma ação.
- **Sam, usuário recorrente:** beneficia-se das mensagens progressivas, mas pode querer cancelar antes de 45 segundos.
- **Pessoa em conexão lenta:** precisa de feedback realista, uma saída segura e garantia clara de que seus dados não serão perdidos.

## Próximas etapas

1. Concluir o critique das demais telas.
2. Revisar os estágios para depender do estado real da operação.
3. Definir quando mostrar contador, retry e voltar.
4. Revisar mensagens, metáfora, cores secundárias e acabamento visual.
