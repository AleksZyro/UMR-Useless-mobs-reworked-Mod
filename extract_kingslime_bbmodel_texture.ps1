#
# Extrahiert die in 'King slime.bbmodel' eingebettete Textur (base64)
# nach assets/usless_mobs/textures/entity/king_slime_geo.png fuer den
# GeckoLib-Renderer.
#

$bbmodelPath = "Texturen selber\King slime.bbmodel"
$outPath = "src\main\resources\assets\usless_mobs\textures\entity\king_slime_geo.png"

if (-not (Test-Path $bbmodelPath)) {
    Write-Error "bbmodel fehlt: $bbmodelPath"
    exit 1
}

Write-Output "[1/3] Parsing bbmodel..."
$json = Get-Content -Path $bbmodelPath -Raw | ConvertFrom-Json
if ($json.textures.Count -eq 0) {
    Write-Error "Keine Textur im bbmodel"
    exit 1
}

$srcData = $json.textures[0].source
$prefix = "data:image/png;base64,"
if (-not $srcData.StartsWith($prefix)) {
    Write-Error "Texture source ist nicht im erwarteten base64 PNG format"
    exit 1
}
$base64 = $srcData.Substring($prefix.Length)
$bytes = [Convert]::FromBase64String($base64)
Write-Output "[2/3] Decoded $($bytes.Length) bytes"

$outDir = Split-Path $outPath -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
[IO.File]::WriteAllBytes((Resolve-Path -Path . | Join-Path -ChildPath $outPath), $bytes)

Add-Type -AssemblyName System.Drawing
$bmp = [System.Drawing.Bitmap]::FromFile((Resolve-Path -Path $outPath).Path)
Write-Output "[3/3] Gespeichert: $outPath ($($bmp.Width)x$($bmp.Height))"
$bmp.Dispose()
