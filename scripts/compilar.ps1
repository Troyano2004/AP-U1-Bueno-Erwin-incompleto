Set-Location (Split-Path $PSScriptRoot)
& "$PSScriptRoot\cerrar.ps1"
Start-Sleep -Milliseconds 500
javac -encoding UTF-8 -d out src\*.java
if ($LASTEXITCODE -ne 0) { Write-Host "Error al compilar" -ForegroundColor Red; exit 1 }
