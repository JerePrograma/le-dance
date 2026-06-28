$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\.."))
if (-not (Test-Path -LiteralPath (Join-Path $repoRoot "frontend\node_modules"))) {
    throw "Falta frontend\node_modules. Ejecutá scripts\codex\setup.ps1 primero."
}

Push-Location (Join-Path $repoRoot "frontend")
try {
    & npm run dev
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
