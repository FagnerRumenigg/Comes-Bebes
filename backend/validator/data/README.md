# Dados locais de validação

As imagens desta pasta são dados locais para experimentação e não devem ser versionadas.

## `food/`

Amostra parcial do Food-101, obtida por streaming do dataset `ethz/food101` no Hugging Face. As imagens foram salvas como JPEG para os testes locais. O dataset original contém imagens coletadas do Foodspotting; a licença alerta que os direitos das imagens pertencem aos respectivos autores. Usar somente para experimentação e respeitar os termos originais.

Fonte: https://vision.ee.ethz.ch/datsets.html

## `not_food/`

Amostra do Imagenette 160, com classes de objetos e animais, usada como conjunto negativo. O dataset é mantido pelo projeto fastai e possui licença Apache-2.0 para o projeto, mas as imagens são derivadas do ImageNet; verificar os termos aplicáveis antes de qualquer distribuição ou uso comercial.

Fonte: https://github.com/fastai/imagenette

## Uso

Os arquivos servem para avaliar o validador e calibrar um modelo local. Não incluir essas imagens em releases, containers ou treinamento distribuído sem revisar as licenças.

## Amostra atual

- `food/`: 3.030 imagens, incluindo 20 imagens por cada uma das 101 classes do Food-101.
- `not_food/`: 2.250 imagens, distribuídas entre as classes do Imagenette e a amostra inicial.
- Tamanho local aproximado: 170 MB.
