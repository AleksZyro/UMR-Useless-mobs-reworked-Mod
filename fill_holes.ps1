#
# Erkennt und fuellt interne Transparenz-Loecher in PNG-Texturen.
# Flood-Fill vom Rand markiert echten Hintergrund; alle uebrigen
# transparenten Pixel sind innere Loecher und werden mit der Durchschnitts-
# farbe der naechsten opaken Nachbarn gefuellt.
#

Add-Type -AssemblyName System.Drawing

$ALPHA_THRESHOLD = 16

function Process-Image {
    param([string]$Path, [bool]$Fill)

    $bmp = New-Object System.Drawing.Bitmap $Path
    $W = $bmp.Width
    $H = $bmp.Height

    # In Format32bppArgb konvertieren fuer LockBits
    $argb = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $gfx = [System.Drawing.Graphics]::FromImage($argb)
    $gfx.DrawImage($bmp, 0, 0)
    $gfx.Dispose()
    $bmp.Dispose()

    $rect = New-Object System.Drawing.Rectangle 0, 0, $W, $H
    $data = $argb.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadWrite, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $bytes = [Math]::Abs($data.Stride) * $H
    $buffer = New-Object byte[] $bytes
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $buffer, 0, $bytes)

    # Flood-Fill von Rand markiert Aussenseite
    $exterior = New-Object bool[] ($W * $H)
    $stack = New-Object System.Collections.Generic.Stack[int]
    for ($x = 0; $x -lt $W; $x++) {
        $top = $x
        $bot = ($H - 1) * $W + $x
        if ($buffer[$top * 4 + 3] -lt $ALPHA_THRESHOLD) { [void]$stack.Push($top) }
        if ($buffer[$bot * 4 + 3] -lt $ALPHA_THRESHOLD) { [void]$stack.Push($bot) }
    }
    for ($y = 0; $y -lt $H; $y++) {
        $left = $y * $W
        $right = $y * $W + ($W - 1)
        if ($buffer[$left * 4 + 3] -lt $ALPHA_THRESHOLD) { [void]$stack.Push($left) }
        if ($buffer[$right * 4 + 3] -lt $ALPHA_THRESHOLD) { [void]$stack.Push($right) }
    }
    while ($stack.Count -gt 0) {
        $idx = $stack.Pop()
        if ($idx -lt 0 -or $idx -ge ($W * $H)) { continue }
        if ($exterior[$idx]) { continue }
        if ($buffer[$idx * 4 + 3] -ge $ALPHA_THRESHOLD) { continue }
        $exterior[$idx] = $true
        $x = $idx % $W
        $y = [Math]::Floor($idx / $W)
        if ($x -gt 0)         { [void]$stack.Push($idx - 1) }
        if ($x -lt ($W - 1))  { [void]$stack.Push($idx + 1) }
        if ($y -gt 0)         { [void]$stack.Push($idx - $W) }
        if ($y -lt ($H - 1))  { [void]$stack.Push($idx + $W) }
    }

    # Interne Loecher sammeln
    $holes = New-Object System.Collections.Generic.List[int]
    for ($idx = 0; $idx -lt ($W * $H); $idx++) {
        if ($buffer[$idx * 4 + 3] -lt $ALPHA_THRESHOLD -and -not $exterior[$idx]) {
            $holes.Add($idx)
        }
    }

    if ($Fill -and $holes.Count -gt 0) {
        foreach ($idx in $holes) {
            $x = $idx % $W
            $y = [Math]::Floor($idx / $W)
            $sumR = 0; $sumG = 0; $sumB = 0; $cnt = 0
            $maxR = [Math]::Max($W, $H)
            for ($r = 1; $r -le $maxR -and $cnt -lt 8; $r++) {
                for ($dy = -$r; $dy -le $r; $dy++) {
                    for ($dx = -$r; $dx -le $r; $dx++) {
                        if ([Math]::Abs($dx) -ne $r -and [Math]::Abs($dy) -ne $r) { continue }
                        $nx = $x + $dx; $ny = $y + $dy
                        if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge $W -or $ny -ge $H) { continue }
                        $nidx = $ny * $W + $nx
                        $na = $buffer[$nidx * 4 + 3]
                        if ($na -ge 200) {
                            $sumB += $buffer[$nidx * 4 + 0]
                            $sumG += $buffer[$nidx * 4 + 1]
                            $sumR += $buffer[$nidx * 4 + 2]
                            $cnt++
                            if ($cnt -ge 8) { break }
                        }
                    }
                    if ($cnt -ge 8) { break }
                }
                if ($cnt -gt 0 -and $r -gt 3) { break }
            }
            if ($cnt -gt 0) {
                $buffer[$idx * 4 + 0] = [byte]($sumB / $cnt)
                $buffer[$idx * 4 + 1] = [byte]($sumG / $cnt)
                $buffer[$idx * 4 + 2] = [byte]($sumR / $cnt)
                $buffer[$idx * 4 + 3] = 255
            }
        }
        [System.Runtime.InteropServices.Marshal]::Copy($buffer, 0, $data.Scan0, $bytes)
    }

    $argb.UnlockBits($data)
    if ($Fill) {
        $argb.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    $argb.Dispose()
    return $holes.Count
}

$itemDir = "src\main\resources\assets\usless_mobs\textures\item"
$files = @(
    "blauer_schleimball.png",
    "goldener_schleimball.png",
    "schleimkern.png",
    "netherite_schleimkern.png",
    "schleimreaktor_brustpanzer.png",
    "schleimreaktor_schmiedevorlage.png"
)

Write-Output "Analyse + Fuellen:"
Write-Output ("-" * 60)
foreach ($f in $files) {
    $p = Join-Path $itemDir $f
    $cnt = Process-Image -Path $p -Fill $true
    if ($cnt -gt 0) {
        Write-Output ("  [FIX]  {0,-40} {1,5} Loecher gefuellt" -f $f, $cnt)
    } else {
        Write-Output ("  [OK]   {0,-40} keine Loecher" -f $f)
    }
}
