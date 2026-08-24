#
# Re-koloriert Vanilla slime.png zu einem knall-goldenen Slime.
# Aussenkoerper: warm-Gold-Gradient (kein Gruen-Stich).
# Innenkoerper: heller goldener Kern mit weiss-gelbem Hotspot.
# Resultat als HD 1024x512 (16x scale).
#

Add-Type -AssemblyName System.Drawing

$srcPath = "Texturen selber\slime_vanilla.png"
$outMod  = "src\main\resources\assets\usless_mobs\textures\entity\goldener_schleim.png"

if (-not (Test-Path $srcPath)) {
    Write-Error "Slime-Quelltextur fehlt: $srcPath. Lass erst colorize_king_slime.ps1 laufen (extrahiert slime.png aus dem MC-Jar)."
    exit 1
}

$src = [System.Drawing.Bitmap](New-Object System.Drawing.Bitmap $srcPath)
$W = $src.Width
$H = $src.Height
$dst = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Write-Output "[1/3] Pixel-Recolor ${W}x${H}"

$rand = New-Object System.Random 2025
$splitY = [Math]::Floor($H / 2)

for ($y = 0; $y -lt $H; $y++) {
    for ($x = 0; $x -lt $W; $x++) {
        $p = $src.GetPixel($x, $y)
        if ($p.A -lt 8) {
            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
            continue
        }

        # Luminanz via originalem Gruen-Wert (Vanilla-Slime ist gruen-dominant)
        $lum = (0.299 * $p.R + 0.587 * $p.G + 0.114 * $p.B) / 255.0

        if ($y -lt $splitY) {
            # AUSSEN: kraftvolles Gold mit squared luminance curve.
            # Vanilla slime hat zu helle Pixel — lineares Mapping wird pale.
            # lum² zieht alles in den mid-dark Bereich, dann erst hell wenn original sehr hell ist.
            $effLum = $lum * $lum
            # Stops: deep gold (#3D2A05) -> rich gold (#FFC72C). Blau low fuer Saettigung.
            $rNew = [Math]::Round(60  + 195 * $effLum)  # 60  -> 255
            $gNew = [Math]::Round(42  + 173 * $effLum)  # 42  -> 215
            $bNew = [Math]::Round(5   + 35  * $effLum)  # 5   -> 40

            # Sehr selten weisser Funkel auf den allerhellsten Pixeln
            if ($lum -gt 0.78 -and $rand.NextDouble() -lt 0.03) {
                $rNew = 255; $gNew = 245; $bNew = 180
            }

            $rNew = [Math]::Max(0, [Math]::Min(255, $rNew))
            $gNew = [Math]::Max(0, [Math]::Min(255, $gNew))
            $bNew = [Math]::Max(0, [Math]::Min(255, $bNew))
            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($p.A, [int]$rNew, [int]$gNew, [int]$bNew))
        } else {
            # INNEN: helles glow-Gold mit weiss-gelbem Hotspot
            $cx = 16.0
            $cy = 24.0
            $dx = $x - $cx
            $dy = $y - $cy
            $dist = [Math]::Sqrt($dx * $dx + $dy * $dy)
            $normDist = [Math]::Min($dist / 11.0, 1.0)
            $glow = 1.0 - $normDist

            if ($glow -lt 0.55) {
                # tiefes Gold (goldenrod) -> bright gold
                $t = $glow / 0.55
                $rNew = [Math]::Round(184 + 71  * $t)  # 184 -> 255
                $gNew = [Math]::Round(134 + 81  * $t)  # 134 -> 215
                $bNew = [Math]::Round(11  + 19  * $t)  # 11  -> 30
            } else {
                # bright gold -> weiss-gelb
                $t = ($glow - 0.55) / 0.45
                $rNew = [Math]::Round(255)
                $gNew = [Math]::Round(215 + 40 * $t)   # 215 -> 255
                $bNew = [Math]::Round(30  + 200 * $t)  # 30  -> 230
            }

            # Subtile Variation durch original Luminanz
            $variation = ($lum - 0.5) * 20
            $rNew = [Math]::Max(0, [Math]::Min(255, $rNew + $variation))
            $gNew = [Math]::Max(0, [Math]::Min(255, $gNew + $variation))
            $bNew = [Math]::Max(0, [Math]::Min(255, $bNew + $variation))

            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($p.A, [int]$rNew, [int]$gNew, [int]$bNew))
        }
    }
}
$src.Dispose()
Write-Output "[2/3] Farbumwandlung fertig"

# Upscale 16x to HD 1024x512
$bigW = $W * 16
$bigH = $H * 16
$big = New-Object System.Drawing.Bitmap $bigW, $bigH
$g = [System.Drawing.Graphics]::FromImage($big)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::None
$g.DrawImage($dst, 0, 0, $bigW, $bigH)
$g.Dispose()
$dst.Dispose()

$outDir = Split-Path $outMod -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$big.Save($outMod, [System.Drawing.Imaging.ImageFormat]::Png)
$big.Dispose()
Write-Output "[3/3] Gespeichert: $outMod (${bigW}x${bigH})"
