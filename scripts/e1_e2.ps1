. "$PSScriptRoot\abrir.ps1"
& "$PSScriptRoot\compilar.ps1"
Abrir 'coordinador.ps1'
Start-Sleep -Seconds 3
$ini = (Ahora) + 5000
# E1 (segundos 3 a 9) -> captura E1 entre el segundo 12 y el 19
# E2 (segundos 20 a 42) -> captura E2 al final
Abrir 'ventanilla.ps1' @('-Id','1','-Inicio',"$ini",'-Guion','3:turno,6:turno,9:estado,20:turno,21:turno,22:turno,23:turno,24:turno,25:turno,26:turno')
Abrir 'ventanilla.ps1' @('-Id','2','-Inicio',"$ini",'-Guion','4:turno,7:llamar,28:token-malo,29:sin-token,42:estado')
Abrir 'ventanilla.ps1' @('-Id','3','-Inicio',"$ini",'-Guion','5:turno,8:llamar,31:llamar,32:llamar,33:llamar,34:llamar,35:llamar,36:llamar,37:llamar,38:llamar,39:llamar')
