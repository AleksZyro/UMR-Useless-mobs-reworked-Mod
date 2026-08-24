$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$output = Join-Path $PSScriptRoot '..\src\main\resources\assets\usless_mobs\textures\mob_effect\rabbit_form.png'
$bitmap = [System.Drawing.Bitmap]::new(32, 32, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.Clear([System.Drawing.Color]::Transparent)

function Fill-PixelRect([string]$hex, [int]$x, [int]$y, [int]$width, [int]$height) {
    $brush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($hex))
    try { $graphics.FillRectangle($brush, $x, $y, $width, $height) } finally { $brush.Dispose() }
}

# Compact pixel-art rabbit: dark witch outline, lavender fur and cyan eyes.
Fill-PixelRect '#24152f' 7 2 7 15
Fill-PixelRect '#24152f' 18 2 7 15
Fill-PixelRect '#24152f' 5 12 22 15
Fill-PixelRect '#24152f' 9 26 14 4
Fill-PixelRect '#8f6bb3' 9 4 3 11
Fill-PixelRect '#8f6bb3' 20 4 3 11
Fill-PixelRect '#a987ca' 8 14 16 11
Fill-PixelRect '#c6a8df' 11 15 10 8
Fill-PixelRect '#54d7ef' 9 18 4 3
Fill-PixelRect '#54d7ef' 19 18 4 3
Fill-PixelRect '#3b214b' 10 19 2 2
Fill-PixelRect '#3b214b' 20 19 2 2
Fill-PixelRect '#e48bbf' 14 22 4 3
Fill-PixelRect '#5b3a72' 11 26 10 2

$graphics.Dispose()
$bitmap.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
Write-Output $output
