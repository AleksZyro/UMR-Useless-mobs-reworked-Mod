#
# Generiert Trophy-spezifische Texturen:
#   trophy_slime.png  - 16x16 lila Slime-Body mit gelben Sprenkeln (kingly purple)
#   trophy_base.png   - 16x16 Marmor-artiger Sockel (creme/grau)
#

Add-Type -AssemblyName System.Drawing

# Trophy slime body — purple base with yellow speckles + subtle highlight
function New-TrophySlimeTexture($path) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $rand = New-Object System.Random 999

    $deepPurple   = [System.Drawing.Color]::FromArgb(255, 60, 25, 105)
    $midPurple    = [System.Drawing.Color]::FromArgb(255, 105, 55, 165)
    $brightPurple = [System.Drawing.Color]::FromArgb(255, 155, 95, 215)
    $hiViolet     = [System.Drawing.Color]::FromArgb(255, 200, 155, 240)
    $yellowAccent = [System.Drawing.Color]::FromArgb(255, 255, 220, 80)

    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            # Base: subtle vertical gradient from mid to deep purple
            $t = $y / 15.0
            $r = [int]([Math]::Round(105 + ($deepPurple.R - $midPurple.R) * $t))
            $g = [int]([Math]::Round(55 + ($deepPurple.G - $midPurple.G) * $t))
            $b = [int]([Math]::Round(165 + ($deepPurple.B - $midPurple.B) * $t))
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $r, $g, $b))
        }
    }
    # Noise highlights (bright purple flecks)
    for ($i = 0; $i -lt 8; $i++) {
        $hx = $rand.Next(16); $hy = $rand.Next(16)
        $bmp.SetPixel($hx, $hy, $brightPurple)
    }
    # 3-4 bright violet sparkles
    for ($i = 0; $i -lt 4; $i++) {
        $hx = $rand.Next(2, 14); $hy = $rand.Next(2, 14)
        $bmp.SetPixel($hx, $hy, $hiViolet)
    }
    # 6 small yellow speckles (signature king-slime look)
    for ($i = 0; $i -lt 6; $i++) {
        $hx = $rand.Next(1, 15); $hy = $rand.Next(1, 15)
        $bmp.SetPixel($hx, $hy, $yellowAccent)
    }
    $outDir = Split-Path $path -Parent
    if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "Gespeichert: $path"
}

# Marble pedestal — cream/grey with subtle veining
function New-TrophyBaseTexture($path) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $rand = New-Object System.Random 1234

    $cream   = [System.Drawing.Color]::FromArgb(255, 235, 225, 200)
    $midGrey = [System.Drawing.Color]::FromArgb(255, 200, 190, 170)
    $vein    = [System.Drawing.Color]::FromArgb(255, 140, 130, 115)
    $hi      = [System.Drawing.Color]::FromArgb(255, 255, 250, 235)

    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $useMid = $rand.NextDouble() -lt 0.35
            if ($useMid) {
                $bmp.SetPixel($x, $y, $midGrey)
            } else {
                $bmp.SetPixel($x, $y, $cream)
            }
        }
    }
    # Diagonal veins
    for ($i = 0; $i -lt 6; $i++) {
        $startX = $rand.Next(16); $startY = $rand.Next(16)
        $dx = $rand.Next(-1, 2); $dy = $rand.Next(-1, 2)
        if ($dx -eq 0 -and $dy -eq 0) { $dx = 1 }
        for ($s = 0; $s -lt 4; $s++) {
            $vx = $startX + $dx * $s; $vy = $startY + $dy * $s
            if ($vx -ge 0 -and $vx -lt 16 -and $vy -ge 0 -and $vy -lt 16) {
                $bmp.SetPixel($vx, $vy, $vein)
            }
        }
    }
    # A few bright highlights
    for ($i = 0; $i -lt 5; $i++) {
        $bmp.SetPixel($rand.Next(16), $rand.Next(16), $hi)
    }
    $outDir = Split-Path $path -Parent
    if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "Gespeichert: $path"
}

New-TrophySlimeTexture "src\main\resources\assets\usless_mobs\textures\block\trophy_slime.png"
New-TrophyBaseTexture "src\main\resources\assets\usless_mobs\textures\block\trophy_base.png"
