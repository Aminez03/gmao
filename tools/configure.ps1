$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $envPath) { throw '.env existe déjà : aucun secret ne sera écrasé.' }
function New-RandomSecret {
    $bytes = New-Object byte[] 48
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $generator.GetBytes($bytes)
    $generator.Dispose()
    return [Convert]::ToBase64String($bytes)
}
$settings = Get-Content -LiteralPath (Join-Path $projectRoot '.env.example') -Raw
$settings = $settings.Replace('CHANGE_ME_RANDOM_DATABASE_PASSWORD', (New-RandomSecret))
$settings = $settings.Replace('CHANGE_ME_AT_LEAST_48_RANDOM_CHARACTERS_FOR_JWT_SIGNATURE', (New-RandomSecret))
$settings = $settings.Replace('CHANGE_ME_AT_LEAST_12_CHARACTERS', (New-RandomSecret))
$settings = $settings.Replace('CHANGE_ME_RANDOM_STORAGE_SECRET', (New-RandomSecret))
$settings = $settings.Replace('CHANGE_ME_RANDOM_MONITORING_PASSWORD', (New-RandomSecret))
[IO.File]::WriteAllText($envPath, $settings, (New-Object Text.UTF8Encoding $false))
Write-Output '.env créé avec des secrets aléatoires. Renseignez BOOTSTRAP_EMAIL avant le premier démarrage.'
