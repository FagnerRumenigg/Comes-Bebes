# Validador de imagens

Serviço Python separado do backend Java para validar imagens de publicações sem depender de armazenamento em nuvem.

## Ambiente

O projeto usa Python 3.12 em `.venv-validator`.

No PowerShell:

```powershell
.\.venv-validator\Scripts\Activate.ps1
python -m pip install -r validator/requirements.txt
```

As dependências de modelo ficam separadas até a escolha do classificador:

```powershell
python -m pip install -r validator/requirements-model.txt
```

Para baixar ou preparar datasets locais, use as dependências de dados:

```powershell
python -m pip install -r validator/requirements-data.txt
```

## Direção do serviço

O validador recebe uma imagem em quarentena e retorna uma decisão síncrona:

- `FOOD`
- `NOT_FOOD`
- `UNCERTAIN`

O arquivo não deve ser publicado nem servido enquanto a decisão não for aprovada.

O endpoint `POST /validate` executa primeiro a validação técnica e depois o `csatv2-v1` no mesmo request. O limiar padrão é `0.5` e pode ser ajustado no código antes de expor uma configuração externa. A resposta retorna `APPROVED` ou `REJECTED`, além da decisão e pontuação do classificador.

## Classificador local

O primeiro baseline usa `openai/clip-vit-base-patch32` em modo zero-shot. Ele compara a imagem com prompts de comida e não-comida. Os limiares atuais são provisórios e precisam ser calibrados com a amostra local:

```powershell
python -m validator.evaluate_classifier
```

Na amostra atual de 5.280 imagens, o limiar `0.80` reconheceu `86,6%` das imagens de comida, mas classificou `27,0%` das imagens negativas como comida. Portanto, este baseline ainda não deve aprovar publicações automaticamente; precisamos trocar ou especializar o modelo e usar `UNCERTAIN` de forma conservadora.

## Modelo binário especializado

Também foi testado `mrdbourke/food-not-food-classifier-csatv2-v1`, um classificador binário de 10,7 milhões de parâmetros. Na amostra local, ele obteve:

- Limiar `0.50`: acurácia `99,6%`, recall de comida `99,6%` e falso positivo `0,5%`.
- Limiar `0.60`: acurácia `98,8%`, recall de comida `98,0%` e falso positivo `0,2%`.
- CPU: aproximadamente `49 ms` por imagem individual e `26,7 imagens/s` em lotes de 16.

O modelo informa que seus scores são comprimidos pela destilação; portanto, os limiares precisam ser validados novamente com imagens reais do MVP. O código de avaliação está em `validator/evaluate_csat.py`.
