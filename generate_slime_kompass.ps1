#
# v4: Slime-Kompass 256x256 max polish.
# Brushed-Metal-Goldring, 8 Sub-Cardinal-Ticks, Drop-Shadow unter Nadeln,
# zweite Glass-Reflection rechts-unten, gravierte Compass-Rose,
# polierter Pivot mit Glow.
#

Add-Type -AssemblyName System.Drawing

$outPath = "src\main\resources\assets\usless_mobs\textures\item\slime_kompass.png"

$W = 256
$H = 256
$cx = 128.0
$cy = 128.0

$bmp = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode      = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode  = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode    = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

# 1) Multi-layer drop shadow (blurred via stacked low-alpha)
$shadowSteps = @(@(20, 16), @(35, 12), @(55, 8), @(85, 4))
foreach ($s in $shadowSteps) {
    $a = $s[0]
    $off = $s[1]
    $sb = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb($a, 0, 0, 0))
    $g.FillEllipse($sb, (12.0 + $off / 2), (16.0 + $off), 232.0, 232.0)
    $sb.Dispose()
}

# 2) Outer gold ring with off-center radial bevel
$ringPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$ringPath.AddEllipse(12.0, 12.0, 232.0, 232.0)
$ringBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush $ringPath
$ringBrush.CenterPoint = New-Object System.Drawing.PointF 95.0, 95.0
$ringBrush.CenterColor = [System.Drawing.Color]::FromArgb(255, 255, 240, 160)
$ringBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 110, 70, 12))
$g.FillPath($ringBrush, $ringPath)
$ringBrush.Dispose()
$ringPath.Dispose()

# 3) Brushed metal texture on gold ring — concentric darker arcs (very subtle)
$brushedPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(35, 80, 50, 10)), 1.0
for ($r = 96.0; $r -le 120.0; $r += 3.0) {
    $g.DrawEllipse($brushedPen, ($cx - $r), ($cy - $r), ($r * 2), ($r * 2))
}
$brushedPen.Dispose()

# 4) Inner bevel ring (highlight) + outer rim shadow
$innerHiPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(220, 255, 245, 180)), 2.5
$g.DrawEllipse($innerHiPen, 36.0, 36.0, 184.0, 184.0)
$innerHiPen.Dispose()
$outerShadowPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(170, 60, 35, 8)), 2.5
$g.DrawEllipse($outerShadowPen, 12.0, 12.0, 232.0, 232.0)
$outerShadowPen.Dispose()

# 5) Decorative rivets — 4 cardinal + 4 sub-cardinal sizes
$rivetRadius = 102.0
# Main rivets (larger) at N/S/E/W of ring
$mainRivetSize = 11.0
$angles = @(-90.0, 0.0, 90.0, 180.0)
foreach ($angDeg in $angles) {
    $ang = $angDeg * [Math]::PI / 180.0
    $rx = $cx + [Math]::Cos($ang) * $rivetRadius - $mainRivetSize / 2.0
    $ry = $cy + [Math]::Sin($ang) * $rivetRadius - $mainRivetSize / 2.0
    $rivetDark = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 65, 35, 5))
    $g.FillEllipse($rivetDark, $rx, $ry, $mainRivetSize, $mainRivetSize)
    $rivetDark.Dispose()
    $rivetHi = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(220, 255, 230, 130))
    $g.FillEllipse($rivetHi, ($rx + 2), ($ry + 2), 4.0, 4.0)
    $rivetHi.Dispose()
}
# Sub-cardinal smaller rivets at NE/SE/SW/NW
$subRivetSize = 7.0
$subAngles = @(-45.0, 45.0, 135.0, -135.0)
foreach ($angDeg in $subAngles) {
    $ang = $angDeg * [Math]::PI / 180.0
    $rx = $cx + [Math]::Cos($ang) * $rivetRadius - $subRivetSize / 2.0
    $ry = $cy + [Math]::Sin($ang) * $rivetRadius - $subRivetSize / 2.0
    $rivetDark = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 65, 35, 5))
    $g.FillEllipse($rivetDark, $rx, $ry, $subRivetSize, $subRivetSize)
    $rivetDark.Dispose()
    $rivetHi = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(200, 255, 230, 130))
    $g.FillEllipse($rivetHi, ($rx + 1.5), ($ry + 1.5), 2.5, 2.5)
    $rivetHi.Dispose()
}

# 6) Inner face — slime green with vignette
$facePath = New-Object System.Drawing.Drawing2D.GraphicsPath
$facePath.AddEllipse(36.0, 36.0, 184.0, 184.0)
$faceBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush $facePath
$faceBrush.CenterPoint = New-Object System.Drawing.PointF 110.0, 110.0
$faceBrush.CenterColor = [System.Drawing.Color]::FromArgb(255, 155, 235, 170)
$faceBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 30, 90, 55))
$g.FillPath($faceBrush, $facePath)
$faceBrush.Dispose()
$facePath.Dispose()

# 7) Engraved compass rose (very subtle — diagonal lines from center)
$rosePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(50, 20, 60, 35)), 1.5
for ($deg = 0; $deg -lt 360; $deg += 45) {
    $ang = $deg * [Math]::PI / 180.0
    $x1 = $cx + [Math]::Cos($ang) * 50.0
    $y1 = $cy + [Math]::Sin($ang) * 50.0
    $x2 = $cx + [Math]::Cos($ang) * 80.0
    $y2 = $cy + [Math]::Sin($ang) * 80.0
    $g.DrawLine($rosePen, $x1, $y1, $x2, $y2)
}
$rosePen.Dispose()

# 8) Cardinal tick marks — engraved pills with bright edge
$tickDark = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 15, 55, 30))
$tickHi = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(220, 200, 240, 200))
# N (taller for "north" emphasis)
$g.FillRectangle($tickDark, 122.0, 42.0, 12.0, 26.0)
$g.FillRectangle($tickHi, 124.0, 44.0, 2.0, 22.0)
# S
$g.FillRectangle($tickDark, 122.0, 188.0, 12.0, 22.0)
$g.FillRectangle($tickHi, 124.0, 190.0, 2.0, 18.0)
# W
$g.FillRectangle($tickDark, 42.0, 122.0, 26.0, 12.0)
$g.FillRectangle($tickHi, 44.0, 124.0, 22.0, 2.0)
# E
$g.FillRectangle($tickDark, 188.0, 122.0, 26.0, 12.0)
$g.FillRectangle($tickHi, 190.0, 124.0, 22.0, 2.0)
$tickDark.Dispose()
$tickHi.Dispose()

# 9) Sub-cardinal small dots (NE/SE/SW/NW)
$subTickBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(220, 30, 75, 45))
$subDistance = 70.0
foreach ($angDeg in @(-45.0, 45.0, 135.0, -135.0)) {
    $ang = $angDeg * [Math]::PI / 180.0
    $sx = $cx + [Math]::Cos($ang) * $subDistance - 4.0
    $sy = $cy + [Math]::Sin($ang) * $subDistance - 4.0
    $g.FillEllipse($subTickBrush, $sx, $sy, 8.0, 8.0)
}
$subTickBrush.Dispose()

# 10) Needle shadow on face (offset below)
$needleShadowBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(100, 0, 0, 0))
$shadowOffsetX = 3.0
$shadowOffsetY = 5.0
$redShadowPts = @(
    (New-Object System.Drawing.PointF (128.0 + $shadowOffsetX), (60.0 + $shadowOffsetY)),
    (New-Object System.Drawing.PointF (120.0 + $shadowOffsetX), (90.0 + $shadowOffsetY)),
    (New-Object System.Drawing.PointF (124.0 + $shadowOffsetX), (130.0 + $shadowOffsetY)),
    (New-Object System.Drawing.PointF (132.0 + $shadowOffsetX), (130.0 + $shadowOffsetY)),
    (New-Object System.Drawing.PointF (136.0 + $shadowOffsetX), (90.0 + $shadowOffsetY))
)
$whiteShadowPts = @(
    (New-Object System.Drawing.PointF (128.0 + $shadowOffsetX), (196.0 + $shadowOffsetY)),
    (New-Object System.Drawing.PointF (120.0 + $shadowOffsetX), (166.0 + $shadowOffsetY)),
    (New-Object System.Drawing.PointF (124.0 + $shadowOffsetX), (126.0 + $shadowOffsetY)),
    (New-Object System.Drawing.PointF (132.0 + $shadowOffsetX), (126.0 + $shadowOffsetY)),
    (New-Object System.Drawing.PointF (136.0 + $shadowOffsetX), (166.0 + $shadowOffsetY))
)
$g.FillPolygon($needleShadowBrush, $redShadowPts)
$g.FillPolygon($needleShadowBrush, $whiteShadowPts)
$needleShadowBrush.Dispose()

# 11) Red north needle with gradient + highlight + outline
$redPts = @(
    (New-Object System.Drawing.PointF 128.0, 56.0),
    (New-Object System.Drawing.PointF 118.0, 88.0),
    (New-Object System.Drawing.PointF 124.0, 130.0),
    (New-Object System.Drawing.PointF 132.0, 130.0),
    (New-Object System.Drawing.PointF 138.0, 88.0)
)
$redPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$redPath.AddPolygon($redPts)
$redRect = New-Object System.Drawing.RectangleF 118.0, 56.0, 20.0, 74.0
$redGrad = New-Object System.Drawing.Drawing2D.LinearGradientBrush `
    $redRect, `
    ([System.Drawing.Color]::FromArgb(255, 255, 100, 100)), `
    ([System.Drawing.Color]::FromArgb(255, 160, 20, 20)), `
    90.0
$g.FillPath($redGrad, $redPath)
$redGrad.Dispose()
$redHiPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(220, 255, 220, 220)), 2.5
$g.DrawLine($redHiPen, 128.0, 64.0, 128.0, 124.0)
$redHiPen.Dispose()
$needleOutline = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 80, 5, 5)), 1.5
$g.DrawPath($needleOutline, $redPath)
$needleOutline.Dispose()
$redPath.Dispose()

# 12) White south needle
$whitePts = @(
    (New-Object System.Drawing.PointF 128.0, 200.0),
    (New-Object System.Drawing.PointF 118.0, 168.0),
    (New-Object System.Drawing.PointF 124.0, 126.0),
    (New-Object System.Drawing.PointF 132.0, 126.0),
    (New-Object System.Drawing.PointF 138.0, 168.0)
)
$whitePath = New-Object System.Drawing.Drawing2D.GraphicsPath
$whitePath.AddPolygon($whitePts)
$whiteRect = New-Object System.Drawing.RectangleF 118.0, 126.0, 20.0, 74.0
$whiteGrad = New-Object System.Drawing.Drawing2D.LinearGradientBrush `
    $whiteRect, `
    ([System.Drawing.Color]::FromArgb(255, 255, 255, 255)), `
    ([System.Drawing.Color]::FromArgb(255, 170, 170, 175)), `
    90.0
$g.FillPath($whiteGrad, $whitePath)
$whiteGrad.Dispose()
$whiteHi = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(200, 255, 255, 255)), 2.0
$g.DrawLine($whiteHi, 128.0, 132.0, 128.0, 192.0)
$whiteHi.Dispose()
$whiteOutlinePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 80, 80, 90)), 1.5
$g.DrawPath($whiteOutlinePen, $whitePath)
$whiteOutlinePen.Dispose()
$whitePath.Dispose()

# 13) Central pivot with metallic depth + slime glow
# Outer dark rim
$pivotOuter = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 15, 15, 20))
$g.FillEllipse($pivotOuter, 112.0, 112.0, 32.0, 32.0)
$pivotOuter.Dispose()
# Metallic ring
$pivotRing = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 60, 60, 75))
$g.FillEllipse($pivotRing, 116.0, 116.0, 24.0, 24.0)
$pivotRing.Dispose()
# Inner slime glow
$pivotGlowPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$pivotGlowPath.AddEllipse(118.0, 118.0, 20.0, 20.0)
$pivotGlowBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush $pivotGlowPath
$pivotGlowBrush.CenterColor = [System.Drawing.Color]::FromArgb(240, 200, 255, 220)
$pivotGlowBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 70, 220, 130))
$g.FillPath($pivotGlowBrush, $pivotGlowPath)
$pivotGlowBrush.Dispose()
$pivotGlowPath.Dispose()
# Specular highlight
$pivotSpec = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
$g.FillEllipse($pivotSpec, 122.0, 120.0, 6.0, 5.0)
$pivotSpec.Dispose()

# 14) Glass dome reflections — large upper-left + small lower-right
# Main reflection (upper-left)
$dome1Path = New-Object System.Drawing.Drawing2D.GraphicsPath
$dome1Path.AddEllipse(40.0, 44.0, 130.0, 90.0)
$dome1Brush = New-Object System.Drawing.Drawing2D.PathGradientBrush $dome1Path
$dome1Brush.CenterPoint = New-Object System.Drawing.PointF 70.0, 60.0
$dome1Brush.CenterColor = [System.Drawing.Color]::FromArgb(160, 255, 255, 255)
$dome1Brush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 255, 255, 255))
$g.FillPath($dome1Brush, $dome1Path)
$dome1Brush.Dispose()
$dome1Path.Dispose()
# Small secondary reflection (lower-right)
$dome2Path = New-Object System.Drawing.Drawing2D.GraphicsPath
$dome2Path.AddEllipse(160.0, 170.0, 40.0, 26.0)
$dome2Brush = New-Object System.Drawing.Drawing2D.PathGradientBrush $dome2Path
$dome2Brush.CenterColor = [System.Drawing.Color]::FromArgb(110, 255, 255, 255)
$dome2Brush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 255, 255, 255))
$g.FillPath($dome2Brush, $dome2Path)
$dome2Brush.Dispose()
$dome2Path.Dispose()

$g.Dispose()

$outDir = Split-Path $outPath -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "Gespeichert: $outPath (256x256, v4 max polish)"
