#
# Setzt 12 einzelne AI-generierte Panel-PNGs zu einem korrekten Minecraft
# armor layer_1.png Sheet zusammen (HD: 1024x512, scale 16x von 64x32).
#
# Nutzung:
#   1. Generiere alle 12 Panels mit den Prompts und speichere sie als
#      PNG in:  .\Texturen selber\layer_panels\
#   2. Stelle sicher dass die Dateinamen exakt stimmen (siehe $panels unten)
#   3. PowerShell:  .\assemble_armor_layer.ps1
#

Add-Type -AssemblyName System.Drawing

# Skalierungsfaktor: 16x Vanilla = HD (jeder Vanilla-Pixel = 16x16 Real-Pixel)
$SCALE = 16
$SHEET_W = 64 * $SCALE   # 1024
$SHEET_H = 32 * $SCALE   # 512

# UV-Positionen aus dem Vanilla Minecraft humanoid Modell (in Vanilla-Pixeln,
# Origin oben-links). Jedes Panel: {Datei, X, Y, Breite, Hoehe}
$panels = @(
    @{ File = "body_top.png";    X = 20; Y = 16; W = 8; H = 4  },
    @{ File = "body_bottom.png"; X = 28; Y = 16; W = 8; H = 4  },
    @{ File = "body_right.png";  X = 16; Y = 20; W = 4; H = 12 },
    @{ File = "body_front.png";  X = 20; Y = 20; W = 8; H = 12 },
    @{ File = "body_left.png";   X = 28; Y = 20; W = 4; H = 12 },
    @{ File = "body_back.png";   X = 32; Y = 20; W = 8; H = 12 },
    @{ File = "arm_top.png";     X = 44; Y = 16; W = 4; H = 4  },
    @{ File = "arm_bottom.png";  X = 48; Y = 16; W = 4; H = 4  },
    @{ File = "arm_outer.png";   X = 40; Y = 20; W = 4; H = 12 },
    @{ File = "arm_front.png";   X = 44; Y = 20; W = 4; H = 12 },
    @{ File = "arm_inner.png";   X = 48; Y = 20; W = 4; H = 12 },
    @{ File = "arm_back.png";    X = 52; Y = 20; W = 4; H = 12 }
)

$panelDir = Join-Path $PSScriptRoot "Texturen selber\layer_panels"
$outFile  = Join-Path $PSScriptRoot "src\main\resources\assets\usless_mobs\textures\models\armor\schleimreaktor_layer_1.png"

if (-not (Test-Path $panelDir)) {
    Write-Error "Panel-Ordner fehlt: $panelDir"
    exit 1
}

# Leere transparente Bitmap erstellen
$sheet = New-Object System.Drawing.Bitmap $SHEET_W, $SHEET_H
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.Clear([System.Drawing.Color]::Transparent)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::None

$missing = @()
foreach ($p in $panels) {
    $src = Join-Path $panelDir $p.File
    if (-not (Test-Path $src)) {
        $missing += $p.File
        continue
    }

    $img = [System.Drawing.Image]::FromFile($src)
    $destRect = New-Object System.Drawing.Rectangle (
        ($p.X * $SCALE),
        ($p.Y * $SCALE),
        ($p.W * $SCALE),
        ($p.H * $SCALE)
    )
    $g.DrawImage($img, $destRect, 0, 0, $img.Width, $img.Height, [System.Drawing.GraphicsUnit]::Pixel)
    $img.Dispose()
    Write-Output ("Eingef" + [char]0xFC + "gt: $($p.File) -> ($($p.X),$($p.Y)) $($p.W)x$($p.H)")
}

$g.Dispose()

if ($missing.Count -gt 0) {
    Write-Warning "Folgende Panels fehlen (Slots bleiben transparent):"
    $missing | ForEach-Object { Write-Warning "  - $_" }
}

# Ziel-Ordner sicherstellen
$outDir = Split-Path $outFile -Parent
if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

$sheet.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()

Write-Output ""
Write-Output "Fertig: $outFile (${SHEET_W}x${SHEET_H})"
