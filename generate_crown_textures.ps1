#
# Generiert 16x16 Texturen fuer das 3D-Krone-Model:
#   crown_band.png   - goldener Band/Spike-Look mit vertikalem Gradient
#   crown_gem.png    - blau-gruener Edelstein fuer den Front-Spike
#

Add-Type -AssemblyName System.Drawing

function Make-Texture($path, $rows) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt 16; $y++) {
        $row = $rows[$y]
        for ($x = 0; $x -lt 16; $x++) {
            $c = $row[$x]
            if ($c -eq '.') {
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
            } elseif ($c -eq 'D') {
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 184, 134, 11))  # dark goldenrod (no near-black)
            } elseif ($c -eq 'M') {
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 218, 165, 32))  # goldenrod
            } elseif ($c -eq 'G') {
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 255, 215, 0))   # pure gold
            } elseif ($c -eq 'H') {
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 255, 236, 139)) # bright highlight
            } elseif ($c -eq 'g') {
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 40, 95, 65))    # gem dark
            } elseif ($c -eq 'm') {
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 90, 200, 145))  # gem mid
            } elseif ($c -eq 'h') {
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 200, 255, 220)) # gem hi
            }
        }
    }
    $outDir = Split-Path $path -Parent
    if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "Gespeichert: $path"
}

# Crown band texture (gold mit metallischem Sheen)
$bandRows = @(
    'HHHHHHHHHHHHHHHH',
    'GHHGGHGHGGHHGGHG',
    'GGGGGGGGGGGGGGGG',
    'GMGMGGMGMGMMGMGG',
    'MMMMMMMMMMMMMMMM',
    'MDMDMDDMDMDDMDMM',
    'DDDDDDDDDDDDDDDD',
    'DMDDMMDMDDMDMDDM',
    'MMMMMMMMMMMMMMMM',
    'MGMGGMMGMGMGGMMG',
    'GGGGGGGGGGGGGGGG',
    'GHGGHGHGGHHGGHGG',
    'HHHHHHHHHHHHHHHH',
    'HHGHGHGGHGHGGHGH',
    'GGGGGGGGGGGGGGGG',
    'MMMMMMMMMMMMMMMM'
)
Make-Texture "src\main\resources\assets\usless_mobs\textures\item\crown_band.png" $bandRows

# Crown diamond texture — multi-facet octagonal crystal with center sparkle
function Make-DiamondTexture($path) {
    # 16x16 char-pattern (D=deep, M=mid, B=bright cyan, W=white hi)
    # Bigger center sparkle (4-pixel star + crescent) for more visual pop.
    $rows = @(
        '....DDDDDDDD....',
        '...DMMMMMMMMD...',
        '..DMBBBBBBBBMD..',
        '..DMBBBWWBBBBMD.',
        '.DMBBWWWWWBBBMD.',
        '.DMBWWWWWWWBBMD.',
        'DMBBWWWWWWWBBBMD',
        'DMBBBWWWWWBBBBMD',
        'DMBBBBBWBBBBBMMD',
        'DMBBBBBBBBBMMMMD',
        '.DMBBBBBBBMMMMD.',
        '.DMBBBBBMMMMDDD.',
        '..DMBBBMMMDDDD..',
        '..DMMMMMDDDDD...',
        '...DMMDDDDDD....',
        '....DDDDDDDD....'
    )
    $deepBlue   = [System.Drawing.Color]::FromArgb(255, 18, 60, 96)
    $midBlue    = [System.Drawing.Color]::FromArgb(255, 70, 150, 210)
    $brightCyan = [System.Drawing.Color]::FromArgb(255, 130, 220, 255)
    $hiWhite    = [System.Drawing.Color]::FromArgb(255, 240, 252, 255)
    $outline    = [System.Drawing.Color]::FromArgb(255, 10, 40, 70)
    $transp     = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)

    $bmp = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $c = $rows[$y][$x]
            switch ($c) {
                '.' { $col = $outline }
                'D' { $col = $deepBlue }
                'M' { $col = $midBlue }
                'B' { $col = $brightCyan }
                'W' { $col = $hiWhite }
                default { $col = $transp }
            }
            $bmp.SetPixel($x, $y, $col)
        }
    }
    $outDir = Split-Path $path -Parent
    if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "Gespeichert: $path"
}
Make-DiamondTexture "src\main\resources\assets\usless_mobs\textures\item\crown_gem.png"
