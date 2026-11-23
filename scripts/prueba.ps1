Set-Location (Split-Path $PSScriptRoot)
$host.UI.RawUI.WindowTitle = "PruebaCarga"
& "$PSScriptRoot\cabecera.ps1" "PRUEBACARGA"
java -cp out PruebaCarga
