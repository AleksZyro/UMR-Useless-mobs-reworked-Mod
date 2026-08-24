#
# Malt ein Gold-Patch in die untere rechte Ecke von king_slime_geo.png.
# Wird von der Crown-Bone im king_slime.geo.json per UV samplet.
#
# Virtuelle UV-Coords: 64x64 (per bbmodel resolution).
# Gold-Patch bei virtuellem [60, 60] -> [64, 64], also unten rechts.
# Auf actual PNG 1536x1024:
#   x = (60/64)*1536 = 1440 .. 1536 (96px breit)
#   y = (60/64)*1024 = 960  .. 1024 (64px hoch)
#

Add-Type -AssemblyName System.Drawing

$path = "src\main\resources\assets\usless_mobs\textures\entity\king_slime_geo.png"

if (-not (Test-Path $path)) {
    Write-Error "Textur fehlt: $path"
    exit 1
}

$bmp = [System.Drawing.Bitmap]::FromFile((Resolve-Path $path).Path)
$copy = New-Object System.Drawing.Bitmap $bmp.Width, $bmp.Height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($copy)
$g.DrawImage($bmp, 0, 0)
$bmp.Dispose()

# Gold gradient patch with darker outline
$goldMain = [System.Drawing.Color]::FromArgb(255, 240, 195, 55)
$goldDark = [System.Drawing.Color]::FromArgb(255, 160, 105, 25)
$goldHi   = [System.Drawing.Color]::FromArgb(255, 255, 230, 130)

# Fill gold patch at (1440, 960) -> 96x64
$g.FillRectangle((New-Object System.Drawing.SolidBrush $goldDark), 1440, 960, 96, 64)
$g.FillRectangle((New-Object System.Drawing.SolidBrush $goldMain), 1446, 966, 84, 52)
$g.FillRectangle((New-Object System.Drawing.SolidBrush $goldHi),   1450, 970, 30, 18)

$g.Dispose()

$copy.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$copy.Dispose()
Write-Output "Gold-Patch gemalt bei (1440-1536, 960-1024) - virtuelle UV [60-64, 60-64]"
Write-Output "Gespeichert: $path"
