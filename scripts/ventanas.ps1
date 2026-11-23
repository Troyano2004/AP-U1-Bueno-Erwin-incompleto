# Utilidades Win32 para las capturas de los escenarios (se carga con: . scripts\ventanas.ps1)
#   Close-VentanaEscenario           cierra las ventanas Coordinador, Ventanilla V1/V2/V3, PruebaCarga y Logs
#   Wait-Ventana  <patrones> [seg]   espera a que existan ventanas con esos titulos
#   Set-Mosaico   <patrones>         las acomoda en mosaico en la pantalla principal y las trae al frente
Add-Type -AssemblyName System.Windows.Forms
if (-not ('TJVentanas' -as [type])) {
  Add-Type -TypeDefinition @'
using System;
using System.Text;
using System.Collections.Generic;
using System.Runtime.InteropServices;
public static class TJVentanas {
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
  public delegate bool EnumProc(IntPtr h, IntPtr p);
  [DllImport("user32.dll")] static extern bool EnumWindows(EnumProc f, IntPtr p);
  [DllImport("user32.dll", CharSet = CharSet.Unicode)] static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll", CharSet = CharSet.Unicode)] static extern int GetClassName(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, uint msg, IntPtr w, IntPtr l);
  [DllImport("user32.dll")] public static extern bool IsWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);
  [DllImport("user32.dll")] public static extern bool IsZoomed(IntPtr h);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
  [DllImport("user32.dll")] public static extern bool MoveWindow(IntPtr h, int x, int y, int w, int alto, bool repintar);
  [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr despues, int x, int y, int w, int alto, uint flags);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
  [DllImport("dwmapi.dll")] public static extern int DwmGetWindowAttribute(IntPtr h, int atributo, out RECT r, int tam);

  // Ventanas de consola visibles (Windows Terminal o consola clasica): "handle|titulo"
  public static List<string> Consolas() {
    var lista = new List<string>();
    EnumWindows(delegate (IntPtr h, IntPtr p) {
      if (!IsWindowVisible(h)) return true;
      var clase = new StringBuilder(256); GetClassName(h, clase, 256);
      string c = clase.ToString();
      if (c != "CASCADIA_HOSTING_WINDOW_CLASS" && c != "ConsoleWindowClass") return true;
      var titulo = new StringBuilder(512); GetWindowText(h, titulo, 512);
      lista.Add(h.ToInt64() + "|" + titulo);
      return true;
    }, IntPtr.Zero);
    return lista;
  }
}
'@
}
# Trabajar en pixeles reales aunque Windows tenga escalado
[TJVentanas]::SetProcessDPIAware() | Out-Null

$script:TitulosEscenario = '^(Coordinador( .*)?|Ventanilla V[123]|PruebaCarga|Logs)$'

function Get-VentanaConsola([string]$Patron) {
  foreach ($linea in [TJVentanas]::Consolas()) {
    $h, $t = $linea -split '\|', 2
    if ($t -match $Patron) { [pscustomobject]@{ Handle = [IntPtr][long]$h; Titulo = $t } }
  }
}

function Close-VentanaEscenario([int]$TimeoutS = 10) {
  $abiertas = @(Get-VentanaConsola $script:TitulosEscenario)
  foreach ($v in $abiertas) {
    Write-Host "Cerrando ventana: $($v.Titulo)"
    [TJVentanas]::PostMessage($v.Handle, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero) | Out-Null  # WM_CLOSE
  }
  $limite = (Get-Date).AddSeconds($TimeoutS)
  while (@(Get-VentanaConsola $script:TitulosEscenario).Count -gt 0 -and (Get-Date) -lt $limite) {
    Start-Sleep -Milliseconds 300
  }
  $quedan = @(Get-VentanaConsola $script:TitulosEscenario)
  if ($quedan.Count -gt 0) { Write-Warning "Siguen abiertas: $($quedan.Titulo -join ', ')" }
}

function Wait-Ventana([string[]]$Patrones, [int]$TimeoutS = 60) {
  $limite = (Get-Date).AddSeconds($TimeoutS)
  do {
    $faltan = @($Patrones | Where-Object { -not (Get-VentanaConsola $_) })
    if ($faltan.Count -eq 0) { return $true }
    Start-Sleep -Milliseconds 200
  } while ((Get-Date) -lt $limite)
  Write-Warning "No aparecieron: $($faltan -join ', ')"
  return $false
}

function Set-Mosaico([string[]]$Patrones) {
  $ventanas = @(foreach ($p in $Patrones) { Get-VentanaConsola $p | Select-Object -First 1 })
  $n = $ventanas.Count
  if ($n -eq 0) { Write-Warning 'No hay ventanas para acomodar'; return }
  $area = [System.Windows.Forms.Screen]::PrimaryScreen.WorkingArea
  # Hasta 4 ventanas: columnas de alto completo (asi se ve la cabecera); mas de 4: dos filas
  $columnas = if ($n -le 4) { $n } else { [math]::Ceiling($n / 2) }
  $filas = [math]::Ceiling($n / $columnas)
  $ancho = [math]::Floor($area.Width / $columnas)
  $alto = [math]::Floor($area.Height / $filas)
  for ($i = 0; $i -lt $n; $i++) {
    $h = $ventanas[$i].Handle
    $x = $area.X + ($i % $columnas) * $ancho
    $y = $area.Y + [math]::Floor($i / $columnas) * $alto
    if ([TJVentanas]::IsIconic($h) -or [TJVentanas]::IsZoomed($h)) { [TJVentanas]::ShowWindow($h, 9) | Out-Null }  # SW_RESTORE
    [TJVentanas]::MoveWindow($h, $x, $y, $ancho, $alto, $true) | Out-Null
    # Compensar el borde invisible de Windows 10/11 para que las ventanas queden pegadas
    $r = New-Object TJVentanas+RECT; $f = New-Object TJVentanas+RECT
    [TJVentanas]::GetWindowRect($h, [ref]$r) | Out-Null
    if ([TJVentanas]::DwmGetWindowAttribute($h, 9, [ref]$f, 16) -eq 0) {  # DWMWA_EXTENDED_FRAME_BOUNDS
      $izq = $f.Left - $r.Left; $arr = $f.Top - $r.Top; $der = $r.Right - $f.Right; $aba = $r.Bottom - $f.Bottom
      [TJVentanas]::MoveWindow($h, $x - $izq, $y - $arr, $ancho + $izq + $der, $alto + $arr + $aba, $true) | Out-Null
    }
    # Traer al frente: TOPMOST y luego NOTOPMOST la deja encima de las demas ventanas normales
    [TJVentanas]::SetWindowPos($h, [IntPtr](-1), 0, 0, 0, 0, 0x0043) | Out-Null  # NOSIZE|NOMOVE|SHOWWINDOW
    [TJVentanas]::SetWindowPos($h, [IntPtr](-2), 0, 0, 0, 0, 0x0043) | Out-Null
  }
  [TJVentanas]::SetForegroundWindow($ventanas[$n - 1].Handle) | Out-Null
  Write-Host ("Mosaico: " + (($ventanas | ForEach-Object Titulo) -join ' | '))
}
