# Utilidad: abre un script en una ventana nueva de PowerShell que no se cierra
function Abrir([string]$script, [string[]]$argumentos = @()) {
  $lista = @('-NoExit', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSScriptRoot\$script`"") + $argumentos
  Start-Process powershell -ArgumentList $lista
}
function Ahora { [DateTimeOffset]::Now.ToUnixTimeMilliseconds() }
