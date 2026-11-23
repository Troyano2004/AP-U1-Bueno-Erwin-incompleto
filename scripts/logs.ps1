Set-Location (Split-Path $PSScriptRoot)
$host.UI.RawUI.WindowTitle = "Logs"
& "$PSScriptRoot\cabecera.ps1" "LOGS"
Get-ChildItem logs\*.log | Format-Table Name, Length
Write-Host "--- coordinador.log (primeras 25 lineas) ---" -ForegroundColor Cyan
Get-Content logs\coordinador.log -TotalCount 25
