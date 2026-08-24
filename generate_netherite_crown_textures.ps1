#
# Generiert netherite_crown_band.png - dunkles Netherite-themed Band.
# Diamond bleibt blau (same crown_gem.png wird wiederverwendet).
#

Add-Type -AssemblyName System.Drawing

$outPath = "src\main\resources\assets\usless_mobs\textures\item\netherite_crown_band.png"

# 16x16 char-pattern (D=deep, M=mid dark netherite, G=netherite-grey, H=highlight ember)
# Netherite-Palette: tiefes schwarz-violett mit ember-orange Sprenkeln
$rows = @(
    'GGGGGGGGGGGGGGGG',
    'GMMGGMGMGGMMGGMG',
    'MMMMMMMMMMMMMMMM',
    'MDMDMDDMDMDDMDMM',
    'DDDDDDDDDDDDDDDD',
    'DMDDMHDMDDMDMHDM',
    'MMMMMMMMMMMMMMMM',
    'MGMGGMMGMGMGGMMG',
    'GGGGGGGGGGGGGGGG',
    'GHGGHGHGGHHGGHGG',
    'MMMMMMMMMMMMMMMM',
    'MGMGGMMGMGMGGMMG',
    'GGGGGGGGGGGGGGGG',
    'GGHGGHGGHGHGGHGH',
    'DDDDDDDDDDDDDDDD',
    'MMMMMMMMMMMMMMMM'
)

# Netherite color palette
$deepBlack  = [System.Drawing.Color]::FromArgb(255, 22, 17, 22)      # very dark
$midDark    = [System.Drawing.Color]::FromArgb(255, 50, 38, 48)      # netherite scuffed
$netGrey    = [System.Drawing.Color]::FromArgb(255, 78, 65, 76)      # netherite ingot grey
$ember      = [System.Drawing.Color]::FromArgb(255, 195, 95, 30)     # ember-orange sparkle

$bmp = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
for ($y = 0; $y -lt 16; $y++) {
    $row = $rows[$y]
    for ($x = 0; $x -lt 16; $x++) {
        $c = $row[$x]
        switch ($c) {
            'D' { $col = $deepBlack }
            'M' { $col = $midDark }
            'G' { $col = $netGrey }
            'H' { $col = $ember }
            default { $col = $midDark }
        }
        $bmp.SetPixel($x, $y, $col)
    }
}

$outDir = Split-Path $outPath -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "Gespeichert: $outPath"
