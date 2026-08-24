#
# Extrahiert Vanilla slime.png aus dem MC-Jar und faerbt sie um zu einer
# koeniglich-lilanen King-Slime Textur mit goldenem leuchtenden Kern.
#
# Strategie:
#  - Aussenkoerper (obere Haelfte Y 0-16): Gruen -> Lila, gold-Funkel
#    zufaellig dazu
#  - Innenkern (untere Haelfte Y 16-32): Gruen -> Goldgelb mit
#    cyan-weissem Mittel-Glow
#  - Nicht-opake Pixel (Aussenseite) bleiben transparent
#  - Resultat als HD 1024x512 (16x scale)
#

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$jar = Join-Path $env:APPDATA ".minecraft\versions\1.20.6\1.20.6.jar"
$entry = "assets/minecraft/textures/entity/slime/slime.png"
$tmpVanilla = "Texturen selber\slime_vanilla.png"
$outMod     = "src\main\resources\assets\usless_mobs\textures\entity\king_slime.png"

# Extrahieren
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
$item = $zip.Entries | Where-Object { $_.FullName -eq $entry }
[System.IO.Compression.ZipFileExtensions]::ExtractToFile($item, $tmpVanilla, $true)
$zip.Dispose()
Write-Output "[1/4] Vanilla slime.png extrahiert"

# Vanilla 64x32 laden
$src = [System.Drawing.Bitmap](New-Object System.Drawing.Bitmap $tmpVanilla)
$W = $src.Width
$H = $src.Height
$dst = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Write-Output "[2/4] Pixel-by-Pixel umfaerben (${W}x${H})"

$rand = New-Object System.Random 1337
$splitY = [Math]::Floor($H / 2)  # 16 fuer 64x32: oben = aussen, unten = innen

for ($y = 0; $y -lt $H; $y++) {
    for ($x = 0; $x -lt $W; $x++) {
        $p = $src.GetPixel($x, $y)
        if ($p.A -lt 8) {
            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
            continue
        }

        # Luminanz schaetzen (Vanilla-Slime hat Gruen-Werte)
        $lum = (0.299 * $p.R + 0.587 * $p.G + 0.114 * $p.B) / 255.0

        if ($y -lt $splitY) {
            # AUSSEN-WUERFEL: koeniglich-lila
            # Basis-Lila Mapping ueber Luminanz
            $rNew = [Math]::Round(91 + 99 * $lum)    # #5B->#8A->#BD lila Stufe
            $gNew = [Math]::Round(39 + 77 * $lum)
            $bNew = [Math]::Round(137 + 60 * $lum)

            # Selten Gold-Funkel (5% Chance bei hellen Pixeln)
            if ($lum -gt 0.5 -and $rand.NextDouble() -lt 0.06) {
                $rNew = 255; $gNew = 217; $bNew = 61
            }

            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($p.A, $rNew, $gNew, $bNew))
        } else {
            # INNEN-WUERFEL: leuchtender Goldkern mit Cyan-Hot-Spot
            # Distanz vom Zentrum -> Glow-Intensitaet
            # Innencube belegt UV (0,16)-(32,32); Zentrum bei (16, 24)
            $cx = 16.0
            $cy = 24.0
            $dx = $x - $cx
            $dy = $y - $cy
            $dist = [Math]::Sqrt($dx * $dx + $dy * $dy)
            $normDist = [Math]::Min($dist / 10.0, 1.0)
            $glow = 1.0 - $normDist  # 1.0 in Mitte, 0.0 am Rand

            # Drei Farbstops:
            # Aussen (glow=0):   tiefes warm-Gold #C97A0C
            # Mitte (glow=0.6):  helles Gelb     #FFD93D
            # Zentrum (glow=1):  Cyan-Weiss      #BFE8FF
            if ($glow -lt 0.6) {
                # tiefes Gold -> helles Gelb
                $t = $glow / 0.6
                $rNew = [Math]::Round(201 + 54 * $t)   # 201 -> 255
                $gNew = [Math]::Round(122 + 95 * $t)   # 122 -> 217
                $bNew = [Math]::Round(12 + 49 * $t)    # 12  -> 61
            } else {
                # helles Gelb -> Cyan-Weiss
                $t = ($glow - 0.6) / 0.4
                $rNew = [Math]::Round(255 - 64 * $t)   # 255 -> 191
                $gNew = [Math]::Round(217 + 15 * $t)   # 217 -> 232
                $bNew = [Math]::Round(61 + 194 * $t)   # 61  -> 255
            }

            # Leichte Variation durch original Luminanz (gibt Pixel-Detail)
            $variation = ($lum - 0.5) * 30
            $rNew = [Math]::Max(0, [Math]::Min(255, $rNew + $variation))
            $gNew = [Math]::Max(0, [Math]::Min(255, $gNew + $variation))
            $bNew = [Math]::Max(0, [Math]::Min(255, $bNew + $variation))

            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($p.A, [int]$rNew, [int]$gNew, [int]$bNew))
        }
    }
}
$src.Dispose()
Write-Output "[3/4] Farbumwandlung fertig"

# Auf 1024x512 (16x) hochskalieren via Nearest-Neighbor
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

# Ziel-Ordner sicherstellen
$outDir = Split-Path $outMod -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

$big.Save($outMod, [System.Drawing.Imaging.ImageFormat]::Png)
$big.Dispose()
Write-Output "[4/4] HD-Textur gespeichert: $outMod (${bigW}x${bigH})"
