. "$PSScriptRoot\abrir.ps1"
& "$PSScriptRoot\compilar.ps1"
Abrir 'coordinador.ps1'
Start-Sleep -Seconds 3
$ini = (Ahora) + 5000
Abrir 'ventanilla.ps1' @('-Id','1','-Inicio',"$ini",'-Guion','3:turno,14:turno')
Abrir 'ventanilla.ps1' @('-Id','2','-Inicio',"$ini",'-Guion','4:turno')
Abrir 'ventanilla.ps1' @('-Id','3','-Inicio',"$ini",'-Guion','5:llamar')
# En el segundo 9 se detiene el coordinador; en el 14 V1 pide turno sin coordinador
$espera = ($ini + 9000) - (Ahora); if ($espera -gt 0) { Start-Sleep -Milliseconds $espera }
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -match 'Coordinador' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
Write-Host "Coordinador detenido (E5)" -ForegroundColor Red
