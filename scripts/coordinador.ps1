param([string]$Modo = "")
Set-Location (Split-Path $PSScriptRoot)
$host.UI.RawUI.WindowTitle = "Coordinador $Modo"
& "$PSScriptRoot\cabecera.ps1" "COORDINADOR $Modo"
if ($Modo -eq "inseguro") { java -cp out Coordinador --inseguro } else { java -cp out Coordinador }
Write-Host "`n[El coordinador se detuvo]" -ForegroundColor Red
