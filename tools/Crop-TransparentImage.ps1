param(
    [Parameter(Mandatory)]
    [string]$InputPath,

    [Parameter(Mandatory)]
    [string]$OutputPath,

    [ValidateRange(0, 256)]
    [int]$Padding = 24
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$source = $null
$target = $null
$graphics = $null
try {
    $source = [System.Drawing.Bitmap]::new((Resolve-Path -LiteralPath $InputPath).Path)
    $minX = $source.Width
    $minY = $source.Height
    $maxX = -1
    $maxY = -1

    for ($y = 0; $y -lt $source.Height; $y++) {
        for ($x = 0; $x -lt $source.Width; $x++) {
            if ($source.GetPixel($x, $y).A -eq 0) { continue }
            $minX = [Math]::Min($minX, $x)
            $minY = [Math]::Min($minY, $y)
            $maxX = [Math]::Max($maxX, $x)
            $maxY = [Math]::Max($maxY, $y)
        }
    }

    if ($maxX -lt 0) {
        throw 'Das Eingabebild enthält keine sichtbaren Pixel.'
    }

    $left = [Math]::Max(0, $minX - $Padding)
    $top = [Math]::Max(0, $minY - $Padding)
    $right = [Math]::Min($source.Width - 1, $maxX + $Padding)
    $bottom = [Math]::Min($source.Height - 1, $maxY + $Padding)
    $width = $right - $left + 1
    $height = $bottom - $top + 1

    $target = [System.Drawing.Bitmap]::new($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($target)
    $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $destination = [System.Drawing.Rectangle]::new(0, 0, $width, $height)
    $sourceRectangle = [System.Drawing.Rectangle]::new($left, $top, $width, $height)
    $graphics.DrawImage($source, $destination, $sourceRectangle, [System.Drawing.GraphicsUnit]::Pixel)

    $outputDirectory = Split-Path -Parent $OutputPath
    if ($outputDirectory) { [void][System.IO.Directory]::CreateDirectory($outputDirectory) }
    $target.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output "CROP=PASS;WIDTH=$width;HEIGHT=$height;LEFT=$left;TOP=$top"
}
finally {
    if ($null -ne $graphics) { $graphics.Dispose() }
    if ($null -ne $target) { $target.Dispose() }
    if ($null -ne $source) { $source.Dispose() }
}
