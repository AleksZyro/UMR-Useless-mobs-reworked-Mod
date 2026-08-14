#
# Generates king_slime_trophy.png (256x256) — purple King Slime cube on
# pedestal with a small gold crown on top. Awarded only on Hard difficulty.
#

Add-Type -AssemblyName System.Drawing

$outPath = "src\main\resources\assets\usless_mobs\textures\item\king_slime_trophy.png"

$W = 256
$H = 256
$bmp = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

# Drop shadow under pedestal
$shadow = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(120, 0, 0, 0))
$g.FillEllipse($shadow, 30, 215, 200, 30)
$shadow.Dispose()

# Pedestal — stone-grey trapezoid (wider at top)
$pedestalGrad = New-Object System.Drawing.Drawing2D.LinearGradientBrush `
    (New-Object System.Drawing.Rectangle 36, 175, 184, 45), `
    ([System.Drawing.Color]::FromArgb(255, 180, 180, 195)), `
    ([System.Drawing.Color]::FromArgb(255, 85, 85, 100)), `
    90
$pedestalPts = @(
    (New-Object System.Drawing.PointF 40, 220),
    (New-Object System.Drawing.PointF 50, 175),
    (New-Object System.Drawing.PointF 206, 175),
    (New-Object System.Drawing.PointF 216, 220)
)
$g.FillPolygon($pedestalGrad, $pedestalPts)
$pedestalGrad.Dispose()
# Pedestal outline
$pedestalOutline = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 40, 40, 50)), 3
$g.DrawPolygon($pedestalOutline, $pedestalPts)
$pedestalOutline.Dispose()

# Pedestal top edge highlight
$topHi = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(180, 255, 255, 255)), 2
$g.DrawLine($topHi, 52, 178, 204, 178)
$topHi.Dispose()

# King Slime body (purple cube on pedestal) — drawn as 3 quadrilaterals (front + top + right side)
# Front face
$bodyFrontGrad = New-Object System.Drawing.Drawing2D.LinearGradientBrush `
    (New-Object System.Drawing.Rectangle 70, 70, 110, 110), `
    ([System.Drawing.Color]::FromArgb(255, 175, 95, 220)), `
    ([System.Drawing.Color]::FromArgb(255, 80, 30, 130)), `
    90
$frontPts = @(
    (New-Object System.Drawing.PointF 70, 75),
    (New-Object System.Drawing.PointF 175, 75),
    (New-Object System.Drawing.PointF 175, 175),
    (New-Object System.Drawing.PointF 70, 175)
)
$g.FillPolygon($bodyFrontGrad, $frontPts)
$bodyFrontGrad.Dispose()

# Top face (lighter purple, isometric)
$topGrad = New-Object System.Drawing.Drawing2D.LinearGradientBrush `
    (New-Object System.Drawing.Rectangle 70, 35, 140, 40), `
    ([System.Drawing.Color]::FromArgb(255, 220, 160, 255)), `
    ([System.Drawing.Color]::FromArgb(255, 150, 80, 200)), `
    45
$topFacePts = @(
    (New-Object System.Drawing.PointF 70, 75),
    (New-Object System.Drawing.PointF 105, 40),
    (New-Object System.Drawing.PointF 210, 40),
    (New-Object System.Drawing.PointF 175, 75)
)
$g.FillPolygon($topGrad, $topFacePts)
$topGrad.Dispose()

# Right side (darker)
$sideGrad = New-Object System.Drawing.Drawing2D.LinearGradientBrush `
    (New-Object System.Drawing.Rectangle 175, 40, 40, 140), `
    ([System.Drawing.Color]::FromArgb(255, 100, 45, 160)), `
    ([System.Drawing.Color]::FromArgb(255, 50, 15, 90)), `
    0
$sidePts = @(
    (New-Object System.Drawing.PointF 175, 75),
    (New-Object System.Drawing.PointF 210, 40),
    (New-Object System.Drawing.PointF 210, 145),
    (New-Object System.Drawing.PointF 175, 175)
)
$g.FillPolygon($sideGrad, $sidePts)
$sideGrad.Dispose()

# Cube outline
$cubeOutline = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 25, 0, 50)), 3
$g.DrawPolygon($cubeOutline, $frontPts)
$g.DrawPolygon($cubeOutline, $topFacePts)
$g.DrawPolygon($cubeOutline, $sidePts)
$cubeOutline.Dispose()

# Yellow speckles on slime body (like King Slime entity)
$speckleBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 220, 60))
$rand = New-Object System.Random 333
for ($i = 0; $i -lt 12; $i++) {
    $sx = 80 + $rand.Next(85)
    $sy = 90 + $rand.Next(75)
    $g.FillEllipse($speckleBrush, $sx, $sy, 6, 6)
}
$speckleBrush.Dispose()

# Small gold crown on top
$crownBase = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 215, 0))
$g.FillRectangle($crownBase, 120, 20, 50, 14)
$crownBase.Dispose()
$crownDark = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 180, 140, 20))
$g.FillRectangle($crownDark, 120, 30, 50, 4)
$crownDark.Dispose()
# 4 small spikes on crown
$crownSpike = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 215, 0))
$g.FillRectangle($crownSpike, 122, 12, 6, 8)
$g.FillRectangle($crownSpike, 135, 6, 6, 14)
$g.FillRectangle($crownSpike, 148, 6, 6, 14)
$g.FillRectangle($crownSpike, 161, 12, 6, 8)
$crownSpike.Dispose()
# Crown center diamond
$diamond = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 100, 200, 255))
$g.FillRectangle($diamond, 141, 22, 7, 7)
$diamond.Dispose()
$crownEdge = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 130, 90, 0)), 2
$g.DrawRectangle($crownEdge, 120, 20, 50, 14)
$crownEdge.Dispose()

# Engraved plaque on pedestal
$plaqueBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 180, 140, 20))
$g.FillRectangle($plaqueBrush, 90, 195, 76, 15)
$plaqueBrush.Dispose()
$plaqueEdge = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 80, 50, 0)), 2
$g.DrawRectangle($plaqueEdge, 90, 195, 76, 15)
$plaqueEdge.Dispose()

$g.Dispose()
$outDir = Split-Path $outPath -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "Gespeichert: $outPath (256x256)"
