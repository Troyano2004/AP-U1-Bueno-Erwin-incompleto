# Abre una ventanilla y escribe las ordenes del guion a los segundos indicados
# Guion: "seg:orden,seg:orden,..." medido desde $Inicio (ms Unix)
param([int]$Id, [long]$Inicio, [string]$Guion = "")
Set-Location (Split-Path $PSScriptRoot)
$host.UI.RawUI.WindowTitle = "Ventanilla V$Id"
& "$PSScriptRoot\cabecera.ps1" "VENTANILLA V$Id"
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "java"
$psi.Arguments = "-cp out Ventanilla $Id"
$psi.UseShellExecute = $false
$psi.RedirectStandardInput = $true
$global:ventanilla = [System.Diagnostics.Process]::Start($psi)
if ($Guion) {
  foreach ($paso in $Guion.Split(',')) {
    $partes = $paso.Split(':')
    $objetivo = $Inicio + [long]([double]$partes[0] * 1000)
    $espera = $objetivo - [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
    if ($espera -gt 0) { Start-Sleep -Milliseconds $espera }
    Write-Host $partes[1] -ForegroundColor Yellow
    $global:ventanilla.StandardInput.WriteLine($partes[1])
  }
}
Write-Host "`n[Guion terminado. Esta ventana queda abierta para la captura]" -ForegroundColor Green
