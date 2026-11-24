param([string]$DatabaseUrl = '', [string]$DatabaseUser = 'gmao_test')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location (Join-Path $projectRoot 'backend')
try {
    if ($DatabaseUrl) {
        $env:GMAO_TEST_URL = $DatabaseUrl
        $env:GMAO_TEST_USER = $DatabaseUser
    } else { Write-Warning 'Sans GMAO_TEST_URL, les tests PostgreSQL sont ignorés ; seuls les tests unitaires backend tournent.' }
    & mvn test
    if ($LASTEXITCODE -ne 0) { throw 'Tests backend échoués.' }
} finally { Pop-Location }
Push-Location (Join-Path $projectRoot 'frontend')
try {
    & npm.cmd test
    if ($LASTEXITCODE -ne 0) { throw 'Tests frontend échoués.' }
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw 'Compilation frontend échouée.' }
} finally { Pop-Location }
