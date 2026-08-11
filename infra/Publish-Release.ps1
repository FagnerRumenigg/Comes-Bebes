[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)]
    [ValidateSet('api', 'validator')]
    [string]$Service,

    [Parameter(Mandatory)]
    [string]$Version,

    [string]$DockerHubUsername = 'fagnerrumenigg',

    [securestring]$DockerHubToken,

    [switch]$SkipCleanup
)

$ErrorActionPreference = 'Stop'

$repoName = if ($Service -eq 'api') { 'comesebebes-api' } else { 'comesebebes-validator' }
$workflowFile = if ($Service -eq 'api') { 'release-api.yml' } else { 'release-validator.yml' }
$tagName = "$Service-v$Version"

# --- 1. Criar e enviar a tag, disparando o workflow de release -------------

Write-Output "Criando tag $tagName..."
git tag -a $tagName -m "$Service release $Version"
if ($LASTEXITCODE -ne 0) { throw "Falha ao criar a tag $tagName. Ela ja existe?" }

git push origin $tagName
if ($LASTEXITCODE -ne 0) { throw "Falha ao enviar a tag $tagName." }

# --- 2. Aguardar o workflow correspondente aparecer e concluir -------------

Write-Output "Aguardando o workflow $workflowFile iniciar..."
$runId = $null
for ($attempt = 1; $attempt -le 10; $attempt++) {
    Start-Sleep -Seconds 5
    $run = gh run list --workflow $workflowFile --branch $tagName --limit 1 --json databaseId | ConvertFrom-Json
    if ($run.Count -gt 0) {
        $runId = $run[0].databaseId
        break
    }
}
if (-not $runId) { throw "O workflow nao apareceu apos 50s. Confira manualmente com: gh run list --workflow $workflowFile" }

Write-Output "Acompanhando a execucao $runId..."
gh run watch $runId --exit-status
if ($LASTEXITCODE -ne 0) { throw "O workflow de release falhou. Confira com: gh run view $runId --log-failed" }

Write-Output "Imagem $repoName`:$Version publicada com sucesso no Docker Hub."

if ($SkipCleanup) {
    Write-Output "SkipCleanup ativado, nao vou remover tags antigas."
    return
}

# --- 3. Remover do Docker Hub todas as tags exceto a nova e 'latest' -------

if (-not $DockerHubToken) {
    $DockerHubToken = Read-Host -Prompt "Access Token do Docker Hub (permissao Read/Write/Delete)" -AsSecureString
}
$plainToken = [PSCredential]::new('x', $DockerHubToken).GetNetworkCredential().Password

Write-Output "Autenticando na API do Docker Hub..."
$loginBody = @{ username = $DockerHubUsername; password = $plainToken } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri 'https://hub.docker.com/v2/users/login/' -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = "JWT $($login.token)" }

Write-Output "Listando tags de $DockerHubUsername/$repoName..."
$tagsResponse = Invoke-RestMethod -Method Get -Headers $headers -Uri "https://hub.docker.com/v2/repositories/$DockerHubUsername/$repoName/tags/?page_size=100"
$keep = @($Version, 'latest')
$toDelete = $tagsResponse.results | Where-Object { $keep -notcontains $_.name }

if (-not $toDelete) {
    Write-Output "Nenhuma tag antiga para remover."
    return
}

foreach ($tag in $toDelete) {
    if ($PSCmdlet.ShouldProcess("$DockerHubUsername/$repoName`:$($tag.name)", 'Excluir tag no Docker Hub')) {
        Invoke-RestMethod -Method Delete -Headers $headers -Uri "https://hub.docker.com/v2/repositories/$DockerHubUsername/$repoName/tags/$($tag.name)/"
        Write-Output "Removida: $($tag.name)"
    }
}

Write-Output "Limpeza concluida. Restaram apenas '$Version' e 'latest' em $repoName."
