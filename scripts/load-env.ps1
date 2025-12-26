# Loads environment variables from a .env file into the current PowerShell session
# Usage:
#   pwsh -File scripts/load-env.ps1
#   # or in an open shell
#   . ./scripts/load-env.ps1

$envFile = Join-Path $PSScriptRoot '..\.env'

if (-not (Test-Path $envFile)) {
    Write-Host "No .env file found at $envFile. Create one or copy .env.example." -ForegroundColor Yellow
    return
}

Get-Content -Path $envFile | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line)) { return }
    if ($line.StartsWith('#')) { return }

    $parts = $line -split '=', 2
    if ($parts.Count -lt 2) { return }

    $name = $parts[0].Trim()
    $value = $parts[1].Trim().Trim('"')

    # Set for current process/session
    [System.Environment]::SetEnvironmentVariable($name, $value, 'Process')
    Write-Host "Loaded $name" -ForegroundColor Green
}

Write-Host "Environment variables loaded from .env" -ForegroundColor Green
