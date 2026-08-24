#
# v4: Schleimkern-Schwert 256x256 mit voll-poliertem Graphics-Rendering.
# Drop-Shadow, atmosphaerischer Glow-Halo, getaperte Klinge mit 5-Stop-Gradient,
# Crystal-Facet-Highlights, multi-stop Gem mit Starburst, schwebende Slime-Partikel.
#

Add-Type -AssemblyName System.Drawing

$outPath = "src\main\resources\assets\usless_mobs\textures\item\schleimkern_schwert.png"

$W = 256
$H = 256
$bmp = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

# === ATMOSPHERIC HALO (behind everything, big diffuse glow) ===
$haloPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$haloPath.AddEllipse(0.0, 0.0, 256.0, 256.0)
$haloBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush $haloPath
$haloBrush.CenterColor = [System.Drawing.Color]::FromArgb(70, 120, 255, 170)
$haloBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 120, 255, 170))
$g.FillPath($haloBrush, $haloPath)
$haloBrush.Dispose()
$haloPath.Dispose()

# === FLOATING SLIME PARTICLES (background sparkles) ===
$rand = New-Object System.Random 2024
for ($i = 0; $i -lt 22; $i++) {
    $px = $rand.NextDouble() * 240.0 + 8.0
    $py = $rand.NextDouble() * 240.0 + 8.0
    $sz = 2.0 + $rand.NextDouble() * 5.0
    $a = 80 + $rand.Next(120)
    $partBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb($a, 180, 255, 210))
    $g.FillEllipse($partBrush, $px, $py, $sz, $sz)
    $partBrush.Dispose()
}

# Rotate -45 deg so vertical sword points top-right
$g.TranslateTransform(128.0, 128.0)
$g.RotateTransform(-45.0)
$g.TranslateTransform(-128.0, -128.0)

# === DROP SHADOW under entire sword (blurred via stacked low-alpha) ===
# Vertical shadow body, offset down-right (in pre-rotation space)
$shadowOffsets = @(8, 6, 4, 2)
$shadowAlphas  = @(20, 35, 55, 80)
for ($i = 0; $i -lt 4; $i++) {
    $off = $shadowOffsets[$i]
    $a = $shadowAlphas[$i]
    $shBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb($a, 0, 0, 0))
    # Body shadow (rectangle from blade to handle)
    $g.FillRectangle($shBrush, (110.0 + $off), (24.0 + $off), 36.0, 210.0)
    # Guard shadow
    $g.FillRectangle($shBrush, (60.0 + $off), (164.0 + $off), 136.0, 30.0)
    # Pommel shadow
    $g.FillEllipse($shBrush, (104.0 + $off), (212.0 + $off), 48.0, 44.0)
    $shBrush.Dispose()
}

# === BLADE OUTER GLOW (slime aura around blade) ===
$bladeGlowPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$bladeGlowPath.AddPolygon(@(
    (New-Object System.Drawing.PointF 128.0, 16.0),
    (New-Object System.Drawing.PointF 156.0, 32.0),
    (New-Object System.Drawing.PointF 166.0, 176.0),
    (New-Object System.Drawing.PointF 90.0, 176.0),
    (New-Object System.Drawing.PointF 100.0, 32.0)
))
$bladeGlowBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush $bladeGlowPath
$bladeGlowBrush.CenterColor = [System.Drawing.Color]::FromArgb(100, 130, 255, 170)
$bladeGlowBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 130, 255, 170))
$g.FillPath($bladeGlowBrush, $bladeGlowPath)
$bladeGlowBrush.Dispose()
$bladeGlowPath.Dispose()

# === BLADE (tapered crystal trapezoid) ===
$bladePts = @(
    (New-Object System.Drawing.PointF 128.0, 22.0),  # tip
    (New-Object System.Drawing.PointF 134.0, 34.0),
    (New-Object System.Drawing.PointF 144.0, 168.0),
    (New-Object System.Drawing.PointF 112.0, 168.0),
    (New-Object System.Drawing.PointF 122.0, 34.0)
)
$bladePath = New-Object System.Drawing.Drawing2D.GraphicsPath
$bladePath.AddPolygon($bladePts)

# 5-stop gradient across width (left-dark, mid-bright crystal stripe, right-darker)
$bladeRect = New-Object System.Drawing.RectangleF 112.0, 22.0, 32.0, 146.0
$bladeBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush `
    $bladeRect, `
    ([System.Drawing.Color]::FromArgb(255,40,140,90)), `
    ([System.Drawing.Color]::FromArgb(255,40,140,90)), `
    0.0
$blend = New-Object System.Drawing.Drawing2D.ColorBlend 5
$blend.Colors = @(
    [System.Drawing.Color]::FromArgb(255, 30, 95, 60),
    [System.Drawing.Color]::FromArgb(255, 90, 195, 135),
    [System.Drawing.Color]::FromArgb(255, 220, 255, 230),
    [System.Drawing.Color]::FromArgb(255, 80, 180, 125),
    [System.Drawing.Color]::FromArgb(255, 25, 80, 50)
)
$blend.Positions = @(0.0, 0.32, 0.5, 0.68, 1.0)
$bladeBrush.InterpolationColors = $blend
$g.FillPath($bladeBrush, $bladePath)
$bladeBrush.Dispose()

# Crystal facet highlights — small triangles giving cut-gem feel
$facetBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(120, 255, 255, 245))
$facet1 = @(
    (New-Object System.Drawing.PointF 124.0, 50.0),
    (New-Object System.Drawing.PointF 130.0, 50.0),
    (New-Object System.Drawing.PointF 132.0, 90.0),
    (New-Object System.Drawing.PointF 125.0, 90.0)
)
$g.FillPolygon($facetBrush, $facet1)
$facet2 = @(
    (New-Object System.Drawing.PointF 126.0, 110.0),
    (New-Object System.Drawing.PointF 130.0, 110.0),
    (New-Object System.Drawing.PointF 132.0, 150.0),
    (New-Object System.Drawing.PointF 127.0, 150.0)
)
$g.FillPolygon($facetBrush, $facet2)
$facetBrush.Dispose()

# Specular highlights — bright dots
$specBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(245, 255, 255, 250))
$g.FillEllipse($specBrush, 126.0, 56.0, 6.0, 14.0)
$g.FillEllipse($specBrush, 127.0, 122.0, 5.0, 10.0)
$specBrush.Dispose()

# Blade outline (smooth, slightly thick for cartoon feel)
$outlinePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 12, 50, 30)), 2.5
$outlinePen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
$g.DrawPath($outlinePen, $bladePath)
$outlinePen.Dispose()
$bladePath.Dispose()

# === SLIME DRIPS (organic shapes off blade) ===
# Drip 1: hanging from right edge mid-blade
$drip1Path = New-Object System.Drawing.Drawing2D.GraphicsPath
$drip1Path.AddPolygon(@(
    (New-Object System.Drawing.PointF 144.0, 92.0),
    (New-Object System.Drawing.PointF 156.0, 102.0),
    (New-Object System.Drawing.PointF 156.0, 120.0),
    (New-Object System.Drawing.PointF 150.0, 132.0),
    (New-Object System.Drawing.PointF 142.0, 122.0),
    (New-Object System.Drawing.PointF 140.0, 105.0)
))
$drip1Brush = New-Object System.Drawing.Drawing2D.PathGradientBrush $drip1Path
$drip1Brush.CenterColor = [System.Drawing.Color]::FromArgb(255, 200, 255, 220)
$drip1Brush.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 60, 160, 100))
$g.FillPath($drip1Brush, $drip1Path)
$drip1Brush.Dispose()
$dripOutline = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 20, 70, 45)), 1.5
$g.DrawPath($dripOutline, $drip1Path)
$drip1Path.Dispose()

# Drop 2: small detached droplet
$drop2Path = New-Object System.Drawing.Drawing2D.GraphicsPath
$drop2Path.AddEllipse(160.0, 138.0, 12.0, 16.0)
$drop2Brush = New-Object System.Drawing.Drawing2D.PathGradientBrush $drop2Path
$drop2Brush.CenterColor = [System.Drawing.Color]::FromArgb(255, 200, 255, 220)
$drop2Brush.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 60, 160, 100))
$g.FillPath($drop2Brush, $drop2Path)
$drop2Brush.Dispose()
$g.DrawEllipse($dripOutline, 160.0, 138.0, 12.0, 16.0)

# Drop 3: tiny round
$drop3Path = New-Object System.Drawing.Drawing2D.GraphicsPath
$drop3Path.AddEllipse(166.0, 122.0, 7.0, 8.0)
$drop3Brush = New-Object System.Drawing.Drawing2D.PathGradientBrush $drop3Path
$drop3Brush.CenterColor = [System.Drawing.Color]::FromArgb(255, 220, 255, 230)
$drop3Brush.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 70, 170, 110))
$g.FillPath($drop3Brush, $drop3Path)
$drop3Brush.Dispose()
$g.DrawEllipse($dripOutline, 166.0, 122.0, 7.0, 8.0)
$dripOutline.Dispose()

# === CROSS-GUARD with bevel ===
$guardRect = New-Object System.Drawing.RectangleF 60.0, 168.0, 136.0, 24.0
$guardGrad = New-Object System.Drawing.Drawing2D.LinearGradientBrush `
    $guardRect, `
    ([System.Drawing.Color]::FromArgb(255, 30, 30, 30)), `
    ([System.Drawing.Color]::FromArgb(255, 30, 30, 30)), `
    90.0
$guardBlend = New-Object System.Drawing.Drawing2D.ColorBlend 5
$guardBlend.Colors = @(
    [System.Drawing.Color]::FromArgb(255, 130, 80, 15),
    [System.Drawing.Color]::FromArgb(255, 255, 230, 110),
    [System.Drawing.Color]::FromArgb(255, 230, 180, 55),
    [System.Drawing.Color]::FromArgb(255, 170, 115, 25),
    [System.Drawing.Color]::FromArgb(255, 95, 55, 10)
)
$guardBlend.Positions = @(0.0, 0.2, 0.5, 0.8, 1.0)
$guardGrad.InterpolationColors = $guardBlend
$g.FillRectangle($guardGrad, $guardRect)
$guardGrad.Dispose()

# Guard outline + end caps with sharper edges
$guardOutlinePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 60, 30, 5)), 2.0
$g.DrawRectangle($guardOutlinePen, 60.0, 168.0, 136.0, 24.0)
$guardOutlinePen.Dispose()

# === GEM (Schleimkern) with starburst ===
# Halo behind gem
$gemHaloPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$gemHaloPath.AddEllipse(86.0, 152.0, 84.0, 56.0)
$gemHaloBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush $gemHaloPath
$gemHaloBrush.CenterColor = [System.Drawing.Color]::FromArgb(180, 130, 255, 170)
$gemHaloBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 130, 255, 170))
$g.FillPath($gemHaloBrush, $gemHaloPath)
$gemHaloBrush.Dispose()
$gemHaloPath.Dispose()

# Gem body
$gemRect = New-Object System.Drawing.RectangleF 108.0, 162.0, 40.0, 36.0
$gemPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$gemPath.AddEllipse($gemRect)
$gemBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush $gemPath
$gemBrush.CenterPoint = New-Object System.Drawing.PointF 122.0, 174.0  # off-center for top-left light
$gemBrush.CenterColor = [System.Drawing.Color]::FromArgb(255, 240, 255, 245)
$gemBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 30, 110, 70))
$g.FillPath($gemBrush, $gemPath)
$gemBrush.Dispose()
# Gem outline
$gemOutlinePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 10, 50, 30)), 2.0
$g.DrawEllipse($gemOutlinePen, $gemRect)
$gemOutlinePen.Dispose()
# Gem starburst (4 short rays of bright light)
$rayPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(180, 255, 255, 255)), 2.0
$g.DrawLine($rayPen, 128.0, 156.0, 128.0, 168.0)   # up
$g.DrawLine($rayPen, 128.0, 192.0, 128.0, 204.0)   # down
$g.DrawLine($rayPen, 100.0, 180.0, 112.0, 180.0)   # left
$g.DrawLine($rayPen, 144.0, 180.0, 156.0, 180.0)   # right
$rayPen.Dispose()
# Gem center bright spot
$gemHi = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
$g.FillEllipse($gemHi, 118.0, 168.0, 9.0, 7.0)
$gemHi.Dispose()
$gemPath.Dispose()

# === HANDLE (polished wood with wrap bands) ===
$handleRect = New-Object System.Drawing.RectangleF 116.0, 192.0, 24.0, 40.0
$handleGrad = New-Object System.Drawing.Drawing2D.LinearGradientBrush `
    $handleRect, `
    ([System.Drawing.Color]::FromArgb(255, 60, 35, 15)), `
    ([System.Drawing.Color]::FromArgb(255, 60, 35, 15)), `
    0.0
$handleBlend = New-Object System.Drawing.Drawing2D.ColorBlend 4
$handleBlend.Colors = @(
    [System.Drawing.Color]::FromArgb(255, 50, 28, 12),
    [System.Drawing.Color]::FromArgb(255, 165, 110, 60),
    [System.Drawing.Color]::FromArgb(255, 130, 80, 35),
    [System.Drawing.Color]::FromArgb(255, 40, 22, 8)
)
$handleBlend.Positions = @(0.0, 0.35, 0.65, 1.0)
$handleGrad.InterpolationColors = $handleBlend
$g.FillRectangle($handleGrad, $handleRect)
$handleGrad.Dispose()

# Wrap bands (3 darker horizontal lines)
$wrapPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(220, 30, 15, 5)), 2.0
$g.DrawLine($wrapPen, 116.0, 200.0, 140.0, 200.0)
$g.DrawLine($wrapPen, 116.0, 212.0, 140.0, 212.0)
$g.DrawLine($wrapPen, 116.0, 224.0, 140.0, 224.0)
$wrapPen.Dispose()

# Handle outline
$handleOutlinePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 25, 12, 5)), 2.0
$g.DrawRectangle($handleOutlinePen, 116.0, 192.0, 24.0, 40.0)
$handleOutlinePen.Dispose()

# === POMMEL (Slime ball with full radial glow) ===
# Outer glow
$pomGlowPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$pomGlowPath.AddEllipse(86.0, 200.0, 84.0, 80.0)
$pomGlowBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush $pomGlowPath
$pomGlowBrush.CenterColor = [System.Drawing.Color]::FromArgb(150, 130, 255, 170)
$pomGlowBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 130, 255, 170))
$g.FillPath($pomGlowBrush, $pomGlowPath)
$pomGlowBrush.Dispose()
$pomGlowPath.Dispose()

# Body
$pommelRect = New-Object System.Drawing.RectangleF 104.0, 218.0, 48.0, 42.0
$pommelPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$pommelPath.AddEllipse($pommelRect)
$pommelBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush $pommelPath
$pommelBrush.CenterPoint = New-Object System.Drawing.PointF 118.0, 230.0
$pommelBrush.CenterColor = [System.Drawing.Color]::FromArgb(255, 230, 255, 235)
$pommelBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 30, 110, 70))
$g.FillPath($pommelBrush, $pommelPath)
$pommelBrush.Dispose()
$pommelOutlinePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 10, 50, 30)), 2.0
$g.DrawEllipse($pommelOutlinePen, $pommelRect)
$pommelOutlinePen.Dispose()
# Pommel specular
$pomSpec = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(245, 255, 255, 255))
$g.FillEllipse($pomSpec, 114.0, 225.0, 13.0, 9.0)
$pomSpec.Dispose()
$pommelPath.Dispose()

$g.ResetTransform()
$g.Dispose()

$outDir = Split-Path $outPath -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "Gespeichert: $outPath (256x256, v4 max polish)"
