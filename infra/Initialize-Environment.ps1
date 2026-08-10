[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$DatabaseCredentialFile,

    [switch]$Force
)

$ErrorActionPreference = 'Stop'

$infraDirectory = $PSScriptRoot
$destination = Join-Path $infraDirectory '.env'
$credentialPath = (Resolve-Path -LiteralPath $DatabaseCredentialFile).Path

if ((Test-Path -LiteralPath $destination) -and -not $Force) {
    throw "O arquivo $destination ja existe. Use -Force somente se quiser substitui-lo."
}

$databaseValues = @{}
Get-Content -LiteralPath $credentialPath | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') {
        $databaseValues[$matches[1]] = $matches[2]
    }
}

foreach ($requiredKey in @('DB_NAME', 'DB_USERNAME', 'DB_PASSWORD')) {
    if ([string]::IsNullOrWhiteSpace($databaseValues[$requiredKey])) {
        throw "A credencial nao contem $requiredKey."
    }
}

function New-RandomSecret {
    param([int]$Bytes = 48)

    $buffer = New-Object byte[] $Bytes
    [Security.Cryptography.RandomNumberGenerator]::Fill($buffer)
    return [Convert]::ToBase64String($buffer).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

$content = @(
    'DB_HOST=platform-postgres'
    'DB_PORT=5432'
    'DB_HOST_PORT=5432'
    "DB_NAME=$($databaseValues['DB_NAME'])"
    "DB_USERNAME=$($databaseValues['DB_USERNAME'])"
    "DB_PASSWORD=$($databaseValues['DB_PASSWORD'])"
    'API_HOST_PORT=8082'
    "JWT_SECRET=$(New-RandomSecret)"
    "USER_BLOCKED_USERNAME_HMAC_SECRET=$(New-RandomSecret)"
    'COMESEBEBES_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173'
    'COMESEBEBES_JWT_ISSUER=comesebebes'
    'COMESEBEBES_JWT_EXPIRATION_MINUTES=60'
    'COMESEBEBES_REFRESH_TOKEN_EXPIRATION_DAYS=30'
    'COMESEBEBES_LOGIN_RATE_LIMIT_MAX_ATTEMPTS=5'
    'COMESEBEBES_LOGIN_RATE_LIMIT_WINDOW_SECONDS=300'
) -join [Environment]::NewLine

[IO.File]::WriteAllText($destination, $content + [Environment]::NewLine, [Text.UTF8Encoding]::new($false))
Write-Output "Arquivo de ambiente criado em $destination. Os valores secretos nao foram exibidos."
