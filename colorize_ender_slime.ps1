#
# v2: Ender-Slime Textur — dramatischere Void-Palette.
# Aussenkoerper: tiefes Schwarz-Violett mit Nebula-Variation + Stern-Funkel
# (weiss + cyan + magenta Sprenkel). Innenkoerper: multi-stop Glow
# (deep-void -> magenta -> cyan -> weiss). HD 1024x512.
#

Add-Type -AssemblyName System.Drawing

$srcPath = "Texturen selber\slime_vanilla.png"
$outMod  = "src\main\resources\assets\usless_mobs\textures\entity\ender_schleim.png"

if (-not (Test-Path $srcPath)) {
    Write-Error "Slime-Quelltextur fehlt: $srcPath. Erst colorize_king_slime.ps1 laufen lassen (extrahiert slime.png aus dem MC-Jar)."
    exit 1
}

$src = [System.Drawing.Bitmap](New-Object System.Drawing.Bitmap $srcPath)
$W = $src.Width
$H = $src.Height
$dst = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Write-Output "[1/3] Pixel-Recolor ${W}x${H}"

$rand = New-Object System.Random 42
$splitY = [Math]::Floor($H / 2)

for ($y = 0; $y -lt $H; $y++) {
    for ($x = 0; $x -lt $W; $x++) {
        $p = $src.GetPixel($x, $y)
        if ($p.A -lt 8) {
            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
            continue
        }

        $lum = (0.299 * $p.R + 0.587 * $p.G + 0.114 * $p.B) / 255.0

        if ($y -lt $splitY) {
            # === AUSSEN: dramatische Void-Palette ===
            # Lumi-Mapping mit Nebula-Variation (Position beeinflusst Farbton)
            $nebulaShift = ($x / [double]$W) * 0.3 + ($y / [double]$H) * 0.2
            # Basis: sehr dunkel -> mittel-violett
            $rNew = [Math]::Round(5 + 95 * $lum + $nebulaShift * 20)
            $gNew = [Math]::Round(0 + 15 * $lum)
            $bNew = [Math]::Round(15 + 140 * $lum + $nebulaShift * 30)

            # Stern-Funkel
            if ($lum -gt 0.55 -and $rand.NextDouble() -lt 0.045) {
                # Weisser Stern (hellster)
                $rNew = 240; $gNew = 240; $bNew = 255
            } elseif ($lum -gt 0.5 -and $rand.NextDouble() -lt 0.025) {
                # Cyan-Enderpearl-Sprenkel
                $rNew = 60; $gNew = 200; $bNew = 180
            } elseif ($lum -gt 0.5 -and $rand.NextDouble() -lt 0.015) {
                # Magenta-Nebula-Sprenkel
                $rNew = 220; $gNew = 80; $bNew = 200
            }

            # Clamp
            $rNew = [Math]::Max(0, [Math]::Min(255, $rNew))
            $gNew = [Math]::Max(0, [Math]::Min(255, $gNew))
            $bNew = [Math]::Max(0, [Math]::Min(255, $bNew))
            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($p.A, [int]$rNew, [int]$gNew, [int]$bNew))
        } else {
            # === INNEN: multi-stop Glow ===
            $cx = 16.0
            $cy = 24.0
            $dx = $x - $cx
            $dy = $y - $cy
            $dist = [Math]::Sqrt($dx * $dx + $dy * $dy)
            $normDist = [Math]::Min($dist / 11.0, 1.0)
            $glow = 1.0 - $normDist

            # 4-stop gradient: deep-void -> deep-violet -> magenta -> cyan-weiss
            if ($glow -lt 0.35) {
                # deep-void -> deep violet
                $t = $glow / 0.35
                $rNew = [Math]::Round(20  + 50  * $t)  # 20 -> 70
                $gNew = [Math]::Round(5   + 10  * $t)  # 5 -> 15
                $bNew = [Math]::Round(35  + 75  * $t)  # 35 -> 110
            } elseif ($glow -lt 0.7) {
                # deep violet -> magenta
                $t = ($glow - 0.35) / 0.35
                $rNew = [Math]::Round(70  + 165 * $t)  # 70 -> 235
                $gNew = [Math]::Round(15  + 50  * $t)  # 15 -> 65
                $bNew = [Math]::Round(110 + 110 * $t)  # 110 -> 220
            } else {
                # magenta -> cyan-weiss
                $t = ($glow - 0.7) / 0.3
                $rNew = [Math]::Round(235 - 80  * $t)  # 235 -> 155
                $gNew = [Math]::Round(65  + 175 * $t)  # 65  -> 240
                $bNew = [Math]::Round(220 + 35  * $t)  # 220 -> 255
            }

            # Variation via original Luminanz fuer Pixel-Detail
            $variation = ($lum - 0.5) * 25
            $rNew = [Math]::Max(0, [Math]::Min(255, $rNew + $variation))
            $gNew = [Math]::Max(0, [Math]::Min(255, $gNew + $variation))
            $bNew = [Math]::Max(0, [Math]::Min(255, $bNew + $variation))

            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($p.A, [int]$rNew, [int]$gNew, [int]$bNew))
        }
    }
}
$src.Dispose()
Write-Output "[2/3] Farbumwandlung fertig"

# Upscale 16x to HD 1024x512 nearest-neighbor
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
