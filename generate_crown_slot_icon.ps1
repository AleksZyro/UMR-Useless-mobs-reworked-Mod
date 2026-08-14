#
# Generiert 16x16 Crown-Slot-Icon für den Curios head-Slot.
# Transparenter Hintergrund + grau-silberne Krone-Silhouette (wie vanilla armor slot placeholders).
#

Add-Type -AssemblyName System.Drawing

$outPath = "src\main\resources\assets\usless_mobs\textures\slot\empty_crown_slot.png"

# 16x16 pixel art pattern. . = transparent, X = silhouette grey
$pattern = @(
    '................',
    '................',
    '................',
    '..X..X.X.X..X...',
    '..X.XXXXXXX.X...',
    '..XXXXXXXXXXX...',
    '..XX.XXXXX.XX...',
    '..XX..XXX..XX...',
    '..XXXXXXXXXXX...',
    '..XXXXXXXXXXX...',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................'
)

$bmp = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$transparent = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
$silhouette  = [System.Drawing.Color]::FromArgb(110, 139, 139, 139)  # semi-transparent grey

for ($y = 0; $y -lt 16; $y++) {
    $row = $pattern[$y]
    for ($x = 0; $x -lt 16; $x++) {
        if ($row[$x] -eq 'X') {
            $bmp.SetPixel($x, $y, $silhouette)
        } else {
            $bmp.SetPixel($x, $y, $transparent)
        }
    }
}

$outDir = Split-Path $outPath -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "Gespeichert: $outPath (16x16 crown silhouette)"
