#
# Generiert king_slime_krone.json programmatisch — variable Detailstufe.
# Parameter:
#   $tierCount         - Anzahl Band-Tiers (3-6)
#   $mainSpikes        - Haupt-Spikes (4 oder 8)
#   $useMidSpikes      - kleine Spikes zwischen Haupt-Spikes
#   $gemCount          - eingelassene Gems im Band (4, 8, 16)
#   $arches            - Bogen zwischen Spikes (true/false)
#
# Resultat: 60-200 Cubes je nach Settings.
#

$outPath = "src\main\resources\assets\usless_mobs\models\item\king_slime_krone.json"

# --- Settings (TUNE HIER FÜR MEHR/WENIGER DETAIL) ---
$tierCount      = 5
$mainSpikes     = 8
$useMidSpikes   = $true
$useTinySpikes  = $true   # noch kleinere Spikes zwischen mid + main
$gemCount       = 8
$useArches      = $true
$archSegments   = 4       # cubes pro Bogen

$elements = @()
$bandTexUv = '{"north":{"uv":[0,0,2,2],"texture":"#band"},"east":{"uv":[0,0,2,2],"texture":"#band"},"south":{"uv":[0,0,2,2],"texture":"#band"},"west":{"uv":[0,0,2,2],"texture":"#band"},"up":{"uv":[0,0,2,2],"texture":"#band"},"down":{"uv":[0,0,2,2],"texture":"#band"}}'
$gemTexUv  = '{"north":{"uv":[3,3,13,13],"texture":"#gem"},"east":{"uv":[3,3,13,13],"texture":"#gem"},"south":{"uv":[3,3,13,13],"texture":"#gem"},"west":{"uv":[3,3,13,13],"texture":"#gem"},"up":{"uv":[3,3,13,13],"texture":"#gem"},"down":{"uv":[3,3,13,13],"texture":"#gem"}}'

function Cube($name, $fromX, $fromY, $fromZ, $toX, $toY, $toZ, $useGem = $false) {
    $tex = if ($useGem) { $gemTexUv } else { $bandTexUv }
    return ('{"name":"' + $name + '","from":[' + $fromX + ',' + $fromY + ',' + $fromZ + '],"to":[' + $toX + ',' + $toY + ',' + $toZ + '],"faces":' + $tex + '}')
}

# --- BAND TIERS (multi-layer for depth) ---
# Tier configs: [innerInset, outerInset, height, yOffset]
$bandTiers = @(
    @{inset=0; height=1; y=8},   # bottom rim (widest)
    @{inset=1; height=1; y=9},   # first inset
    @{inset=0; height=1; y=10},  # main band (widest again)
    @{inset=1; height=1; y=11},  # second inset
    @{inset=0; height=1; y=12}   # top rim
)
$activeTiers = $bandTiers[0..([Math]::Min($tierCount, $bandTiers.Count) - 1)]

foreach ($tier in $activeTiers) {
    $i = $tier.inset
    $y0 = $tier.y
    $y1 = $tier.y + $tier.height
    $a = 2 + $i  # outer offset
    $b = 14 - $i # outer max
    $c = 3 + $i  # inner offset (band thickness 1)
    $d = 13 - $i
    # 4 walls
    $elements += Cube ("tier_y${y0}_front")  $c $y0 $a       $d $y1 ($a+1)
    $elements += Cube ("tier_y${y0}_back")   $c $y0 ($b-1)   $d $y1 $b
    $elements += Cube ("tier_y${y0}_left")   $a $y0 $c       ($a+1) $y1 $d
    $elements += Cube ("tier_y${y0}_right")  ($b-1) $y0 $c   $b $y1 $d
}

# --- EMBEDDED GEMS in mid-band tier ---
$gemY0 = 10
$gemY1 = 11
$gemPositions = @()  # list of (x, z, name) for gem placement
if ($gemCount -ge 4) {
    $gemPositions += @{x=7; z=1; w=2; d=2; name="gem_F"}    # front protrudes
    $gemPositions += @{x=7; z=13; w=2; d=2; name="gem_B"}   # back protrudes
    $gemPositions += @{x=1; z=7; w=2; d=2; name="gem_L"}    # left
    $gemPositions += @{x=13; z=7; w=2; d=2; name="gem_R"}   # right
}
if ($gemCount -ge 8) {
    # diagonals
    $gemPositions += @{x=3; z=3; w=2; d=2; name="gem_FL"}
    $gemPositions += @{x=11; z=3; w=2; d=2; name="gem_FR"}
    $gemPositions += @{x=3; z=11; w=2; d=2; name="gem_BL"}
    $gemPositions += @{x=11; z=11; w=2; d=2; name="gem_BR"}
}
foreach ($g in $gemPositions) {
    $elements += Cube $g.name $g.x $gemY0 $g.z ($g.x + $g.w) $gemY1 ($g.z + $g.d) $true
}

# --- MAIN SPIKES (with diamond tips, stepped pyramid) ---
$topY = 13
if ($activeTiers.Count -gt 0) { $topY = $activeTiers[-1].y + 1 }
$mainSpikePositions = @()
if ($mainSpikes -ge 4) {
    $mainSpikePositions += @{x=2; z=2; name="FL"}
    $mainSpikePositions += @{x=12; z=2; name="FR"}
    $mainSpikePositions += @{x=2; z=12; name="BL"}
    $mainSpikePositions += @{x=12; z=12; name="BR"}
}
if ($mainSpikes -ge 8) {
    $mainSpikePositions += @{x=7; z=2; name="F"}
    $mainSpikePositions += @{x=7; z=12; name="B"}
    $mainSpikePositions += @{x=2; z=7; name="L"}
    $mainSpikePositions += @{x=12; z=7; name="R"}
}
foreach ($s in $mainSpikePositions) {
    # base 2x2x2, mid 1x2x1 narrowing, diamond tip 1x1x1
    $bx = $s.x; $bz = $s.z
    $elements += Cube ("spike_${($s.name)}_base") $bx $topY $bz ($bx+2) ($topY+2) ($bz+2)
    $elements += Cube ("spike_${($s.name)}_mid") ($bx+0.5) ($topY+2) ($bz+0.5) ($bx+1.5) ($topY+3) ($bz+1.5)
    $elements += Cube ("diamond_${($s.name)}") ($bx+0.5) ($topY+3) ($bz+0.5) ($bx+1.5) ($topY+4) ($bz+1.5) $true
}

# --- MID SPIKES (smaller, between mains if mains are at cardinals) ---
if ($useMidSpikes -and $mainSpikes -eq 4) {
    $midSpikePositions = @(
        @{x=7; z=2; name="midF"},
        @{x=7; z=12; name="midB"},
        @{x=2; z=7; name="midL"},
        @{x=12; z=7; name="midR"}
    )
    foreach ($s in $midSpikePositions) {
        $bx = $s.x; $bz = $s.z
        $elements += Cube ("spike_${($s.name)}_base") $bx $topY $bz ($bx+2) ($topY+1) ($bz+2)
        $elements += Cube ("diamond_${($s.name)}") ($bx+0.5) ($topY+1) ($bz+0.5) ($bx+1.5) ($topY+2) ($bz+1.5) $true
    }
}

# --- TINY SPIKES (smallest, between main+mid if both exist) ---
if ($useTinySpikes -and $mainSpikes -ge 8) {
    $tinyPositions = @(
        @{x=4; z=2; name="t1"}, @{x=10; z=2; name="t2"},
        @{x=4; z=12; name="t3"}, @{x=10; z=12; name="t4"},
        @{x=2; z=4; name="t5"}, @{x=2; z=10; name="t6"},
        @{x=12; z=4; name="t7"}, @{x=12; z=10; name="t8"}
    )
    foreach ($s in $tinyPositions) {
        $bx = $s.x; $bz = $s.z
        $elements += Cube ("tiny_${($s.name)}") $bx $topY $bz ($bx+2) ($topY+1) ($bz+2)
    }
}

# --- CENTER SPIKE (taller, stepped pyramid) ---
$centerY = $topY
$elements += Cube "center_base" 6 $centerY 6 10 ($centerY+1) 10
$elements += Cube "center_mid1" 6.5 ($centerY+1) 6.5 9.5 ($centerY+3) 9.5
$elements += Cube "center_mid2" 7 ($centerY+3) 7 9 ($centerY+5) 9
$elements += Cube "center_top"  7.25 ($centerY+5) 7.25 8.75 ($centerY+6) 8.75
# Big central orb
$elements += Cube "center_orb"  6 ($centerY+6) 6 10 ($centerY+9) 10 $true

# --- ARCHES (if enabled, 4 arches between FL-FR, FR-BR, etc.) ---
if ($useArches -and $mainSpikes -ge 4) {
    $archStartY = $topY + 4
    $archPeakY = $centerY + 6
    # Arch front (FL to FR)
    for ($i = 0; $i -lt $archSegments; $i++) {
        $t = ($i + 0.5) / $archSegments
        $x0 = 2.5 + (12.5 - 2.5) * $t - 0.5
        # Parabola: peak in middle
        $h = -4 * $t * ($t - 1)  # 0 at ends, 1 at middle
        $y = $archStartY + ($archPeakY - $archStartY) * $h
        $elements += Cube ("arch_F_$i") $x0 $y 2.5 ($x0 + 1) ($y + 1) 3.5
        $elements += Cube ("arch_B_$i") $x0 $y 12.5 ($x0 + 1) ($y + 1) 13.5
    }
    # Side arches (FL-BL, FR-BR)
    for ($i = 0; $i -lt $archSegments; $i++) {
        $t = ($i + 0.5) / $archSegments
        $z0 = 2.5 + (12.5 - 2.5) * $t - 0.5
        $h = -4 * $t * ($t - 1)
        $y = $archStartY + ($archPeakY - $archStartY) * $h
        $elements += Cube ("arch_L_$i") 2.5 $y $z0 3.5 ($y + 1) ($z0 + 1)
        $elements += Cube ("arch_R_$i") 12.5 $y $z0 13.5 ($y + 1) ($z0 + 1)
    }
}

# Total cube count
Write-Output "Generated $($elements.Count) cubes"

# --- Build final JSON ---
$elementsJson = $elements -join ",`n    "

$displayJson = @'
  "display": {
    "head": {
      "translation": [0, 14.0, 0],
      "rotation": [0, 0, 0],
      "scale": [1.0, 1.0, 1.0]
    },
    "thirdperson_righthand": {
      "rotation": [60, 0, 0],
      "translation": [0, 3.0, 1.0],
      "scale": [0.5, 0.5, 0.5]
    },
    "thirdperson_lefthand": {
      "rotation": [60, 0, 0],
      "translation": [0, 3.0, 1.0],
      "scale": [0.5, 0.5, 0.5]
    },
    "firstperson_righthand": {
      "rotation": [0, -90, 25],
      "translation": [1.13, 3.2, 1.13],
      "scale": [0.6, 0.6, 0.6]
    },
    "firstperson_lefthand": {
      "rotation": [0, 90, -25],
      "translation": [1.13, 3.2, 1.13],
      "scale": [0.6, 0.6, 0.6]
    },
    "ground": {
      "translation": [0, 3, 0],
      "scale": [0.45, 0.45, 0.45]
    },
    "gui": {
      "rotation": [30, 225, 0],
      "translation": [0, 0, 0],
      "scale": [0.75, 0.75, 0.75]
    },
    "fixed": {
      "rotation": [0, 0, 0],
      "translation": [0, 0, 0],
      "scale": [1, 1, 1]
    }
  }
'@

$fullJson = @"
{
  "credit": "Generated by generate_crown_model.ps1 — $($elements.Count) cubes",
  "texture_size": [16, 16],
  "textures": {
    "band": "usless_mobs:item/crown_band",
    "gem": "usless_mobs:item/crown_gem",
    "particle": "usless_mobs:item/crown_band"
  },
  "elements": [
    $elementsJson
  ],
$displayJson
}
"@

$outDir = Split-Path $outPath -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$fullJson | Out-File -FilePath $outPath -Encoding utf8
Write-Output "Gespeichert: $outPath"
