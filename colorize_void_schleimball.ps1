#
# v2: Void-Schleimball aus blauer_schleimball.png — Recolor + Nebula-Overlay.
# Recolor: Blau -> Void-Violett mit Sternenfeld.
# Overlay: 2 farbige Nebula-Wolken + zentraler Void-Glow ueber den recolor.
#

Add-Type -AssemblyName System.Drawing

$srcPath = "src\main\resources\assets\usless_mobs\textures\item\blauer_schleimball.png"
$outPath = "src\main\resources\assets\usless_mobs\textures\item\void_schleimball.png"

if (-not (Test-Path $srcPath)) {
    Write-Error "Quelltextur fehlt: $srcPath"
    exit 1
}

$src = [System.Drawing.Bitmap](New-Object System.Drawing.Bitmap $srcPath)
$W = $src.Width
$H = $src.Height
$dst = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Write-Output "[1/3] Recolor ${W}x${H}"

$rand = New-Object System.Random 7777

for ($y = 0; $y -lt $H; $y++) {
    for ($x = 0; $x -lt $W; $x++) {
        $p = $src.GetPixel($x, $y)
        if ($p.A -lt 8) {
            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
            continue
        }

        $maxC = [Math]::Max([Math]::Max($p.R, $p.G), $p.B) / 255.0
        $minC = [Math]::Min([Math]::Min($p.R, $p.G), $p.B) / 255.0
        $lightness = ($maxC + $minC) / 2.0

        # Void-Palette
        $rNew = [Math]::Round(8 + 80 * $lightness)
        $gNew = [Math]::Round(0 + 22 * $lightness)
        $bNew = [Math]::Round(18 + 140 * $lightness)

        # Stern-Funkel
        $r = $rand.NextDouble()
        if ($lightness -gt 0.65 -and $r -lt 0.020) {
            # Brighter white stars
            $rNew = 240; $gNew = 240; $bNew = 255
        } elseif ($lightness -gt 0.55 -and $r -lt 0.014) {
            # Cyan stars
            $rNew = 90; $gNew = 220; $bNew = 200
        } elseif ($lightness -gt 0.55 -and $r -lt 0.010) {
            # Magenta stars
            $rNew = 220; $gNew = 90; $bNew = 200
        }

        $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($p.A, $rNew, $gNew, $bNew))
    }
}
$src.Dispose()
Write-Output "[2/3] Nebula-Overlay + Glow"

# === OVERLAY: Nebula-Wolken + zentraler Void-Glow via Graphics ===
$g = [System.Drawing.Graphics]::FromImage($dst)
$g.SmoothingMode      = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.CompositingMode    = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

# Magenta Nebula-Wolke oben-links
$neb1Path = New-Object System.Drawing.Drawing2D.GraphicsPath
$neb1Path.AddEllipse(20.0, 30.0, 130.0, 100.0)
$neb1Brush = New-Object System.Drawing.Drawing2D.PathGradientBrush $neb1Path
$neb1Brush.CenterColor = [System.Drawing.Color]::FromArgb(90, 210, 80, 200)
$neb1Brush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 210, 80, 200))
$g.FillPath($neb1Brush, $neb1Path)
$neb1Brush.Dispose()
$neb1Path.Dispose()

# Cyan Nebula-Wolke rechts-unten
$neb2Path = New-Object System.Drawing.Drawing2D.GraphicsPath
$neb2Path.AddEllipse(110.0, 120.0, 130.0, 110.0)
$neb2Brush = New-Object System.Drawing.Drawing2D.PathGradientBrush $neb2Path
$neb2Brush.CenterColor = [System.Drawing.Color]::FromArgb(80, 80, 200, 220)
$neb2Brush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 80, 200, 220))
$g.FillPath($neb2Brush, $neb2Path)
$neb2Brush.Dispose()
$neb2Path.Dispose()

# Zentraler Void-Glow (bright magenta -> transparent)
$centerPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$centerPath.AddEllipse(80.0, 80.0, 96.0, 96.0)
$centerBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush $centerPath
$centerBrush.CenterPoint = New-Object System.Drawing.PointF 128.0, 128.0
$centerBrush.CenterColor = [System.Drawing.Color]::FromArgb(110, 230, 150, 255)
$centerBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 230, 150, 255))
$centerBrush.FocusScales = New-Object System.Drawing.PointF 0.3, 0.3
$g.FillPath($centerBrush, $centerPath)
$centerBrush.Dispose()
$centerPath.Dispose()

# Bright Stars (8 grosse weisse Punkte mit Spike-Highlight)
$starBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
$starPositions = @(
    @(50, 70), @(190, 60), @(80, 180), @(200, 170),
    @(130, 40), @(40, 130), @(190, 220), @(70, 220)
)
foreach ($pos in $starPositions) {
    $sx = $pos[0]
    $sy = $pos[1]
    # Glow halo
    $haloPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $haloPath.AddEllipse(($sx - 8), ($sy - 8), 16, 16)
    $haloBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush $haloPath
    $haloBrush.CenterColor = [System.Drawing.Color]::FromArgb(140, 200, 220, 255)
    $haloBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 200, 220, 255))
    $g.FillPath($haloBrush, $haloPath)
    $haloBrush.Dispose()
    $haloPath.Dispose()
    # Star core
    $g.FillEllipse($starBrush, ($sx - 2.5), ($sy - 2.5), 5.0, 5.0)
}
$starBrush.Dispose()

$g.Dispose()

$outDir = Split-Path $outPath -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$dst.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$dst.Dispose()
Write-Output "[3/3] Gespeichert: $outPath (v2 nebula)"
