[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet('api', 'validator')]
    [string]$Service,

    [Parameter(Mandatory)]
    [string]$Version,

    [string]$EnvFile = (Join-Path $PSScriptRoot '.env')
)

$ErrorActionPreference = 'Stop'

$infraDirectory = $PSScriptRoot
$composeFile = Join-Path $infraDirectory 'compose.yml'
$versionKey = if ($Service -eq 'api') { 'API_VERSION' } else { 'VALIDATOR_VERSION' }

if (-not (Test-Path -LiteralPath $EnvFile)) {
    throw "Arquivo de ambiente nao encontrado: $EnvFile"
}

$envLines = Get-Content -LiteralPath $EnvFile
$pattern = "^$versionKey="
$hasKey = $envLines -match $pattern

if (-not $hasKey) {
    throw "$versionKey nao encontrada em $EnvFile."
}

$currentVersion = ($envLines -match $pattern)[0] -replace $pattern, ''
Write-Output "Atualizando $versionKey de '$currentVersion' para '$Version' em $EnvFile"

$updatedLines = $envLines | ForEach-Object {
    if ($_ -match $pattern) { "$versionKey=$Version" } else { $_ }
}
[IO.File]::WriteAllText($EnvFile, ($updatedLines -join [Environment]::NewLine) + [Environment]::NewLine, [Text.UTF8Encoding]::new($false))

Write-Output "Baixando imagem $Service`:$Version..."
docker compose --env-file $EnvFile -f $composeFile pull $Service
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao baixar a imagem. $versionKey foi revertida para '$currentVersion'."
}

Write-Output "Subindo o servico $Service..."
docker compose --env-file $EnvFile -f $composeFile up -d $Service
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao subir o servico. Verifique os logs com: docker compose --env-file `"$EnvFile`" -f `"$composeFile`" logs $Service"
}

docker compose --env-file $EnvFile -f $composeFile ps $Service
Write-Output "Deploy de $Service`:$Version concluido."
