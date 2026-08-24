#
# Generates 32 compass frames (slime_kompass_00..31.png) plus matching
# item-model JSONs by compositing the base texture and a rotated copy of
# the needle texture for each angle step.
#

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Drawing.Drawing2D

$basePath   = "Grafiken\item\slime_kompass_ohne_Zeiger.png"
$needlePath = "Grafiken\item\Zeiger.png"

$texOutDir   = "src\main\resources\assets\usless_mobs\textures\item"
$modelOutDir = "src\main\resources\assets\usless_mobs\models\item"

if (-not (Test-Path $basePath))   { Write-Error "Missing $basePath"; exit 1 }
if (-not (Test-Path $needlePath)) { Write-Error "Missing $needlePath"; exit 1 }

# Load source bitmaps (1024x1024).
$baseSrc   = [System.Drawing.Bitmap]::FromFile((Resolve-Path $basePath).Path)
$needleSrc = [System.Drawing.Bitmap]::FromFile((Resolve-Path $needlePath).Path)

# Output resolution per frame. 256x256 is the sweet spot for inventory icons.
$outSize = 256

# Needle is shrunk so it fits inside the compass face (source PNG fills the whole canvas).
$needleScale = 0.55

# Compass center sits slightly above canvas center because the base PNG has a charm-hanger below.
# Positive value moves the pivot upward.
$pivotOffsetUp = 6

# Pre-downscale base to outSize (reuse across all 32 frames).
$baseSmall = New-Object System.Drawing.Bitmap $outSize, $outSize, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gB = [System.Drawing.Graphics]::FromImage($baseSmall)
$gB.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gB.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$gB.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$gB.DrawImage($baseSrc, 0, 0, $outSize, $outSize)
$gB.Dispose()

# Pre-downscale needle to (outSize * needleScale) so we can draw it 1:1 around the pivot.
$needleSize = [int]($outSize * $needleScale)
$needleSmall = New-Object System.Drawing.Bitmap $needleSize, $needleSize, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gN = [System.Drawing.Graphics]::FromImage($needleSmall)
$gN.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gN.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$gN.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$gN.DrawImage($needleSrc, 0, 0, $needleSize, $needleSize)
$gN.Dispose()

$baseSrc.Dispose()
$needleSrc.Dispose()

if (-not (Test-Path $texOutDir))   { New-Item -ItemType Directory -Path $texOutDir -Force | Out-Null }
if (-not (Test-Path $modelOutDir)) { New-Item -ItemType Directory -Path $modelOutDir -Force | Out-Null }

# Generate 32 frames.
for ($i = 0; $i -lt 32; $i++) {
    $angleDeg = $i * 11.25
    $idx = '{0:D2}' -f $i

    # Compose: copy base, then rotate-and-overlay needle.
    $frame = New-Object System.Drawing.Bitmap $outSize, $outSize, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $gF = [System.Drawing.Graphics]::FromImage($frame)
    $gF.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $gF.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $gF.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $gF.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $gF.CompositingMode    = [System.Drawing.Drawing2D.CompositingMode]::SourceOver

    # 1) Base layer
    $gF.DrawImage($baseSmall, 0, 0)

    # 2) Rotate around the compass face center (slightly above canvas midpoint),
    #    then draw the needle centered on that pivot.
    $pivotX = $outSize / 2.0
    $pivotY = ($outSize / 2.0) - $pivotOffsetUp
    $gF.TranslateTransform($pivotX, $pivotY)
    $gF.RotateTransform($angleDeg)
    $halfNeedle = $needleSize / 2.0
    $gF.DrawImage($needleSmall, -$halfNeedle, -$halfNeedle)
    $gF.ResetTransform()
    $gF.Dispose()

    $texPath   = Join-Path $texOutDir   "slime_kompass_${idx}.png"
    $modelPath = Join-Path $modelOutDir "slime_kompass_${idx}.json"
    $frame.Save($texPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $frame.Dispose()

    # Sub-model: single layer pointing at our generated texture.
    $modelJson = @"
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "usless_mobs:item/slime_kompass_${idx}"
  }
}
"@
    $modelJson | Out-File -FilePath $modelPath -Encoding utf8 -NoNewline
}

$baseSmall.Dispose()
$needleSmall.Dispose()
Write-Output "Generated 32 compass frames + sub-models (${outSize}x${outSize})"
