# Guarda una captura de toda la pantalla principal en docs\img\<Nombre>.png
# Uso: powershell -ExecutionPolicy Bypass -File scripts\captura.ps1 e1
param([Parameter(Mandatory = $true)][string]$Nombre)

Add-Type -AssemblyName System.Windows.Forms, System.Drawing
if (-not ('TJCaptura.Dpi' -as [type])) {
  Add-Type -Namespace TJCaptura -Name Dpi -MemberDefinition @'
[DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
'@
}
# Coordenadas en pixeles reales aunque Windows tenga escalado (125 %, 150 %...)
[TJCaptura.Dpi]::SetProcessDPIAware() | Out-Null

$limites = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
$carpeta = Join-Path (Split-Path $PSScriptRoot) 'docs\img'
New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
if ($Nombre -notmatch '\.png$') { $Nombre += '.png' }
$ruta = Join-Path $carpeta $Nombre

$imagen = New-Object System.Drawing.Bitmap $limites.Width, $limites.Height
$grafico = [System.Drawing.Graphics]::FromImage($imagen)
try {
  $grafico.CopyFromScreen($limites.Location, [System.Drawing.Point]::Empty, $limites.Size)
  $imagen.Save($ruta, [System.Drawing.Imaging.ImageFormat]::Png)
  Write-Host "Captura guardada: $ruta ($($limites.Width)x$($limites.Height))"
} finally {
  $grafico.Dispose()
  $imagen.Dispose()
}
