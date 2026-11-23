# Detiene coordinador, ventanillas y PruebaCarga que sigan corriendo
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -match 'Coordinador|Ventanilla|PruebaCarga' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
