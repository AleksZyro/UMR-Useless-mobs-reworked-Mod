param()

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$exportRoot = Join-Path $projectRoot 'Modelle\Exports\corrupted_silverfish_v2'
$geometryPath = Join-Path $exportRoot 'geo\corrupted_silverfish.geo.json'
$texturePath = Join-Path $exportRoot 'textures\entity\corrupted_silverfish.png'
$animationPath = Join-Path $exportRoot 'animations\corrupted_silverfish.animation.json'
$blockbenchPath = Join-Path $projectRoot 'Modelle\Editierbar\Corrupted Silverfish v2.bbmodel'

foreach ($path in @($geometryPath, $texturePath, $animationPath, $blockbenchPath)) {
    $directory = Split-Path -Parent $path
    [void][System.IO.Directory]::CreateDirectory($directory)
}

$palette = [ordered]@{
    Outline = [System.Drawing.Color]::FromArgb(255, 24, 21, 31)
    Shadow = [System.Drawing.Color]::FromArgb(255, 43, 41, 51)
    Silver = [System.Drawing.Color]::FromArgb(255, 89, 96, 107)
    LightSilver = [System.Drawing.Color]::FromArgb(255, 140, 150, 163)
    Highlight = [System.Drawing.Color]::FromArgb(255, 201, 209, 218)
    DeepCorruption = [System.Drawing.Color]::FromArgb(255, 49, 15, 47)
    DarkRed = [System.Drawing.Color]::FromArgb(255, 122, 23, 61)
    Crimson = [System.Drawing.Color]::FromArgb(255, 181, 42, 79)
    CorruptionLight = [System.Drawing.Color]::FromArgb(255, 239, 92, 120)
    Violet = [System.Drawing.Color]::FromArgb(255, 75, 35, 110)
    Energy = [System.Drawing.Color]::FromArgb(255, 216, 137, 255)
}

function Paint-Region {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [int]$X,
        [int]$Y,
        [int]$Width,
        [int]$Height,
        [System.Drawing.Color]$Base,
        [System.Drawing.Color]$Shade,
        [System.Drawing.Color]$Light,
        [switch]$Corrupted
    )

    for ($py = $Y; $py -lt ($Y + $Height); $py++) {
        for ($px = $X; $px -lt ($X + $Width); $px++) {
            $isBorder = ($px -eq $X) -or ($py -eq $Y) -or ($px -eq ($X + $Width - 1)) -or ($py -eq ($Y + $Height - 1))
            if ($isBorder) {
                $color = $palette.Outline
            }
            elseif ((($px + ($py * 2)) % 7) -eq 0) {
                $color = $Shade
            }
            elseif (($py -eq ($Y + 1)) -or ((($px * 3 + $py) % 13) -eq 0)) {
                $color = $Light
            }
            else {
                $color = $Base
            }

            if ($Corrupted -and -not $isBorder -and ((($px + $py) % 5) -eq 0)) {
                $color = $palette.Crimson
            }

            $Bitmap.SetPixel($px, $py, $color)
        }
    }
}

$bitmap = [System.Drawing.Bitmap]::new(128, 64, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
try {
    $bitmap.MakeTransparent()

    # Body atlas regions.
    Paint-Region $bitmap 0 0 24 16 $palette.Silver $palette.Shadow $palette.Highlight
    Paint-Region $bitmap 24 0 32 20 $palette.LightSilver $palette.Silver $palette.Highlight
    Paint-Region $bitmap 56 0 28 20 $palette.Silver $palette.Shadow $palette.LightSilver -Corrupted
    Paint-Region $bitmap 84 0 24 20 $palette.Silver $palette.Shadow $palette.LightSilver -Corrupted
    Paint-Region $bitmap 108 0 20 16 $palette.Shadow $palette.Outline $palette.Silver

    # Appendages and corruption atlas regions.
    Paint-Region $bitmap 0 32 16 16 $palette.Silver $palette.Shadow $palette.LightSilver
    Paint-Region $bitmap 16 32 16 16 $palette.Silver $palette.Shadow $palette.LightSilver
    Paint-Region $bitmap 32 32 16 16 $palette.Silver $palette.Shadow $palette.LightSilver
    Paint-Region $bitmap 48 32 16 16 $palette.Silver $palette.Shadow $palette.Highlight
    Paint-Region $bitmap 64 32 10 32 $palette.DeepCorruption $palette.Violet $palette.Crimson -Corrupted
    Paint-Region $bitmap 75 32 10 32 $palette.DarkRed $palette.DeepCorruption $palette.CorruptionLight -Corrupted
    Paint-Region $bitmap 86 32 10 32 $palette.DeepCorruption $palette.Violet $palette.Crimson -Corrupted
    Paint-Region $bitmap 96 32 16 32 $palette.DarkRed $palette.DeepCorruption $palette.CorruptionLight -Corrupted
    Paint-Region $bitmap 112 32 16 32 $palette.Violet $palette.DeepCorruption $palette.Crimson -Corrupted

    foreach ($point in @(@(103, 36), @(104, 37), @(119, 39), @(120, 40))) {
        $bitmap.SetPixel($point[0], $point[1], $palette.Energy)
    }

    $bitmap.Save($texturePath, [System.Drawing.Imaging.ImageFormat]::Png)
}
finally {
    $bitmap.Dispose()
}

function New-Cube {
    param(
        [double[]]$Origin,
        [double[]]$Size,
        [double[]]$Uv
    )

    return [ordered]@{ origin = $Origin; size = $Size; uv = $Uv }
}

function New-Bone {
    param(
        [string]$Name,
        [string]$Parent,
        [double[]]$Pivot,
        [System.Collections.IDictionary]$Cube,
        [double[]]$Rotation
    )

    $bone = [ordered]@{ name = $Name }
    if ($Parent) { $bone.parent = $Parent }
    $bone.pivot = $Pivot
    if ($Rotation) { $bone.rotation = $Rotation }
    if ($Cube) { $bone.cubes = @($Cube) }
    return $bone
}

$boneSpecs = @(
    [ordered]@{ Name='root'; Parent=$null; From=$null; To=$null; Pivot=@(0,0,0); Rotation=$null; Uv=$null },
    [ordered]@{ Name='head'; Parent='root'; From=@(-3,1,-11); To=@(3,5,-5); Pivot=@(0,3,-8); Rotation=$null; Uv=@(0,0) },
    [ordered]@{ Name='front_shell'; Parent='root'; From=@(-4,1,-6); To=@(4,6,0); Pivot=@(0,3,-3); Rotation=$null; Uv=@(24,0) },
    [ordered]@{ Name='middle_shell'; Parent='root'; From=@(-3.5,1,-1); To=@(3.5,5,5); Pivot=@(0,3,2); Rotation=$null; Uv=@(56,0) },
    [ordered]@{ Name='tail_shell'; Parent='root'; From=@(-2.5,1,4); To=@(2.5,5,10); Pivot=@(0,3,7); Rotation=$null; Uv=@(84,0) },
    [ordered]@{ Name='left_front_leg'; Parent='front_shell'; From=@(3,0,-6); To=@(5,1,-2); Pivot=@(3,1,-4); Rotation=$null; Uv=@(0,32) },
    [ordered]@{ Name='right_front_leg'; Parent='front_shell'; From=@(-5,0,-6); To=@(-3,1,-2); Pivot=@(-3,1,-4); Rotation=$null; Uv=@(0,32) },
    [ordered]@{ Name='left_middle_leg'; Parent='middle_shell'; From=@(3,0,0); To=@(5,1,4); Pivot=@(3,1,2); Rotation=$null; Uv=@(16,32) },
    [ordered]@{ Name='right_middle_leg'; Parent='middle_shell'; From=@(-5,0,0); To=@(-3,1,4); Pivot=@(-3,1,2); Rotation=$null; Uv=@(16,32) },
    [ordered]@{ Name='left_back_leg'; Parent='tail_shell'; From=@(2.5,0,4); To=@(4.5,1,9); Pivot=@(2.5,1,6); Rotation=$null; Uv=@(32,32) },
    [ordered]@{ Name='right_back_leg'; Parent='tail_shell'; From=@(-4.5,0,4); To=@(-2.5,1,9); Pivot=@(-2.5,1,6); Rotation=$null; Uv=@(32,32) },
    [ordered]@{ Name='left_mandible'; Parent='head'; From=@(0.5,1,-13); To=@(2.5,2,-10); Pivot=@(1.5,1.5,-10); Rotation=@(0,-10,0); Uv=@(48,32) },
    [ordered]@{ Name='right_mandible'; Parent='head'; From=@(-2.5,1,-13); To=@(-0.5,2,-10); Pivot=@(-1.5,1.5,-10); Rotation=@(0,10,0); Uv=@(48,32) },
    [ordered]@{ Name='tail_tip'; Parent='tail_shell'; From=@(-1.5,1.5,9); To=@(1.5,4,12); Pivot=@(0,2.5,9); Rotation=$null; Uv=@(108,0) },
    [ordered]@{ Name='corruption_spine_1'; Parent='front_shell'; From=@(-0.5,6,-5); To=@(0.5,9,-3); Pivot=@(0,6,-4); Rotation=$null; Uv=@(64,32) },
    [ordered]@{ Name='corruption_spine_2'; Parent='middle_shell'; From=@(-0.5,5,-1); To=@(0.5,8.5,1); Pivot=@(0,5,0); Rotation=$null; Uv=@(75,32) },
    [ordered]@{ Name='corruption_spine_3'; Parent='tail_shell'; From=@(-0.5,5,5); To=@(0.5,8,7); Pivot=@(0,5,6); Rotation=$null; Uv=@(86,32) },
    [ordered]@{ Name='corruption_crystal_left'; Parent='middle_shell'; From=@(1.5,4.5,0); To=@(3,7,2); Pivot=@(2,4.5,1); Rotation=@(0,0,-20); Uv=@(96,32) },
    [ordered]@{ Name='corruption_crystal_right'; Parent='tail_shell'; From=@(-2.5,4,5); To=@(-1,6.5,7); Pivot=@(-1.75,4,6); Rotation=@(0,0,20); Uv=@(112,32) }
)

$bones = @()
foreach ($spec in $boneSpecs) {
    $cube = $null
    if ($spec.From) {
        $size = for ($axis = 0; $axis -lt 3; $axis++) { [double]$spec.To[$axis] - [double]$spec.From[$axis] }
        $cube = New-Cube -Origin $spec.From -Size $size -Uv $spec.Uv
    }
    $bones += New-Bone -Name $spec.Name -Parent $spec.Parent -Pivot $spec.Pivot -Cube $cube -Rotation $spec.Rotation
}

$geometry = [ordered]@{
    format_version = '1.12.0'
    'minecraft:geometry' = @(
        [ordered]@{
            description = [ordered]@{
                identifier = 'geometry.corrupted_silverfish'
                texture_width = 128
                texture_height = 64
                visible_bounds_width = 2.2
                visible_bounds_height = 1.4
                visible_bounds_offset = @(0, 0.5, 0)
            }
            bones = $bones
        }
    )
}
$geometry | ConvertTo-Json -Depth 50 | Set-Content -LiteralPath $geometryPath -Encoding utf8

function New-RotationTrack {
    param([string]$Axis, [double[]]$Values)
    $track = [ordered]@{}
    $times = @('0.0','0.3','0.6','0.9','1.2')
    for ($index = 0; $index -lt $times.Count; $index++) {
        $vector = @(0.0, 0.0, 0.0)
        if ($Axis -eq 'Y') { $vector[1] = $Values[$index] } else { $vector[2] = $Values[$index] }
        $track[$times[$index]] = [ordered]@{ post = $vector; lerp_mode = 'linear' }
    }
    return [ordered]@{ rotation = $track }
}

function New-ScaleTrack {
    param([double[]]$Values)
    $track = [ordered]@{}
    $times = @('0.0','0.3','0.6','0.9','1.2')
    for ($index = 0; $index -lt $times.Count; $index++) {
        $track[$times[$index]] = [ordered]@{ post = @(1.0, $Values[$index], 1.0); lerp_mode = 'linear' }
    }
    return [ordered]@{ scale = $track }
}

$animatedBones = [ordered]@{
    head = New-RotationTrack 'Y' @(0,-1.5,0,1.5,0)
    front_shell = New-RotationTrack 'Y' @(0,2.5,0,-2.5,0)
    middle_shell = New-RotationTrack 'Y' @(0,-2,0,2,0)
    tail_shell = New-RotationTrack 'Y' @(0,3.5,0,-3.5,0)
    tail_tip = New-RotationTrack 'Y' @(0,-5,0,5,0)
    left_front_leg = New-RotationTrack 'Z' @(0,2,0,-2,0)
    right_front_leg = New-RotationTrack 'Z' @(0,-2,0,2,0)
    left_middle_leg = New-RotationTrack 'Z' @(0,-1.5,0,1.5,0)
    right_middle_leg = New-RotationTrack 'Z' @(0,1.5,0,-1.5,0)
    left_back_leg = New-RotationTrack 'Z' @(0,-2,0,2,0)
    right_back_leg = New-RotationTrack 'Z' @(0,2,0,-2,0)
    corruption_crystal_left = New-ScaleTrack @(1,1.03,1,0.98,1)
    corruption_crystal_right = New-ScaleTrack @(1,0.98,1,1.03,1)
}

$animation = [ordered]@{
    format_version = '1.8.0'
    animations = [ordered]@{
        'animation.corrupted_silverfish.idle' = [ordered]@{
            loop = $true
            animation_length = 1.2
            bones = $animatedBones
        }
    }
}
$animation | ConvertTo-Json -Depth 50 | Set-Content -LiteralPath $animationPath -Encoding utf8

# Build a deterministic editable Blockbench source with one cube per non-root bone.
$elements = @()
$groups = @()
$groupByName = @{}
$outlinerNodeByName = @{}
$elementByBone = @{}

for ($index = 0; $index -lt $boneSpecs.Count; $index++) {
    $spec = $boneSpecs[$index]
    $groupUuid = '33333333-3333-4333-8333-{0:d12}' -f ($index + 1)
    $group = [ordered]@{
        name = $spec.Name
        uuid = $groupUuid
        export = $true
        locked = $false
        origin = $spec.Pivot
        rotation = $(if ($spec.Rotation) { $spec.Rotation } else { @(0,0,0) })
        color = ($index % 8)
        children = @()
        reset = $false
        shade = $true
        mirror_uv = $false
        visibility = $true
        autouv = 0
        isOpen = $true
    }
    $groups += $group
    $groupByName[$spec.Name] = $group
    $outlinerNodeByName[$spec.Name] = [ordered]@{
        uuid = $groupUuid
        isOpen = $true
        children = @()
    }

    if ($spec.From) {
        $elementUuid = '44444444-4444-4444-8444-{0:d12}' -f ($index + 1)
        $element = [ordered]@{
            name = $spec.Name
            box_uv = $true
            from = $spec.From
            to = $spec.To
            autouv = 0
            color = ($index % 8)
            origin = $spec.Pivot
            uv_offset = $spec.Uv
            type = 'cube'
            uuid = $elementUuid
        }
        $elements += $element
        $elementByBone[$spec.Name] = $elementUuid
        $outlinerNodeByName[$spec.Name].children = @($elementUuid)
    }
}

foreach ($spec in $boneSpecs | Where-Object { $_.Parent }) {
    $outlinerNodeByName[$spec.Parent].children += $outlinerNodeByName[$spec.Name]
}

$textureBytes = [System.IO.File]::ReadAllBytes($texturePath)
$textureSource = 'data:image/png;base64,' + [Convert]::ToBase64String($textureBytes)

function New-BbKeyframes {
    param(
        [string]$Channel,
        [object[]]$Vectors,
        [int]$Seed
    )

    $times = @(0.0, 0.3, 0.6, 0.9, 1.2)
    $keyframes = @()
    for ($index = 0; $index -lt $times.Count; $index++) {
        $vector = $Vectors[$index]
        $keyframes += [ordered]@{
            channel = $Channel
            data_points = @(
                [ordered]@{
                    x = [string]$vector[0]
                    y = [string]$vector[1]
                    z = [string]$vector[2]
                }
            )
            uuid = '66666666-6666-4666-8666-{0:d12}' -f ($Seed + $index)
            time = $times[$index]
            color = -1
            interpolation = 'linear'
        }
    }
    return $keyframes
}

$bbTracks = [ordered]@{
    head = @('rotation', @(@(0,0,0),@(0,-1.5,0),@(0,0,0),@(0,1.5,0),@(0,0,0)))
    front_shell = @('rotation', @(@(0,0,0),@(0,2.5,0),@(0,0,0),@(0,-2.5,0),@(0,0,0)))
    middle_shell = @('rotation', @(@(0,0,0),@(0,-2,0),@(0,0,0),@(0,2,0),@(0,0,0)))
    tail_shell = @('rotation', @(@(0,0,0),@(0,3.5,0),@(0,0,0),@(0,-3.5,0),@(0,0,0)))
    tail_tip = @('rotation', @(@(0,0,0),@(0,-5,0),@(0,0,0),@(0,5,0),@(0,0,0)))
    left_front_leg = @('rotation', @(@(0,0,0),@(0,0,2),@(0,0,0),@(0,0,-2),@(0,0,0)))
    right_front_leg = @('rotation', @(@(0,0,0),@(0,0,-2),@(0,0,0),@(0,0,2),@(0,0,0)))
    left_middle_leg = @('rotation', @(@(0,0,0),@(0,0,-1.5),@(0,0,0),@(0,0,1.5),@(0,0,0)))
    right_middle_leg = @('rotation', @(@(0,0,0),@(0,0,1.5),@(0,0,0),@(0,0,-1.5),@(0,0,0)))
    left_back_leg = @('rotation', @(@(0,0,0),@(0,0,-2),@(0,0,0),@(0,0,2),@(0,0,0)))
    right_back_leg = @('rotation', @(@(0,0,0),@(0,0,2),@(0,0,0),@(0,0,-2),@(0,0,0)))
    corruption_crystal_left = @('scale', @(@(1,1,1),@(1,1.03,1),@(1,1,1),@(1,0.98,1),@(1,1,1)))
    corruption_crystal_right = @('scale', @(@(1,1,1),@(1,0.98,1),@(1,1,1),@(1,1.03,1),@(1,1,1)))
}

$bbAnimators = [ordered]@{}
$trackIndex = 0
foreach ($trackEntry in $bbTracks.GetEnumerator()) {
    $boneName = $trackEntry.Key
    $channel = $trackEntry.Value[0]
    $vectors = $trackEntry.Value[1]
    $groupUuid = $groupByName[$boneName].uuid
    $bbAnimators[$groupUuid] = [ordered]@{
        name = $boneName
        type = 'bone'
        rotation_global = $false
        quaternion_interpolation = $false
        keyframes = @(New-BbKeyframes -Channel $channel -Vectors $vectors -Seed (100 + ($trackIndex * 10)))
    }
    $trackIndex++
}

$bbAnimation = [ordered]@{
    uuid = '77777777-7777-4777-8777-777777777777'
    name = 'animation.corrupted_silverfish.idle'
    loop = 'loop'
    override = $false
    length = 1.2
    snapping = 20
    selected = $true
    saved = $true
    path = 'corrupted_silverfish.animation.json'
    anim_time_update = ''
    blend_weight = ''
    start_delay = ''
    loop_delay = ''
    animators = $bbAnimators
}

$bbmodel = [ordered]@{
    meta = [ordered]@{ format_version = '5.0'; model_format = 'geckolib_model'; box_uv = $true }
    name = 'Corrupted Silverfish v2'
    model_identifier = 'geometry.corrupted_silverfish'
    visible_box = @(2.2, 1.4, 0)
    variable_placeholders = ''
    timeline_setups = @()
    unhandled_root_fields = @{}
    geckolib_modid = 'usless_mobs'
    geckolib_filepath_cache = ''
    resolution = [ordered]@{ width = 128; height = 64 }
    elements = $elements
    groups = $groups
    outliner = @($outlinerNodeByName['root'])
    textures = @(
        [ordered]@{
            path = ''
            name = 'corrupted_silverfish.png'
            folder = 'entity'
            namespace = 'usless_mobs'
            id = '0'
            particle = $false
            render_mode = 'default'
            visible = $true
            mode = 'bitmap'
            saved = $true
            uuid = '55555555-5555-4555-8555-555555555555'
            source = $textureSource
        }
    )
    animations = @($bbAnimation)
    geckolib_model_type = 'Entity'
}
$bbmodel | ConvertTo-Json -Depth 80 | Set-Content -LiteralPath $blockbenchPath -Encoding utf8

Write-Output 'CORRUPTED_SILVERFISH_V2=BUILT'
Write-Output "GEOMETRY=$geometryPath"
Write-Output "TEXTURE=$texturePath"
Write-Output "ANIMATION=$animationPath"
Write-Output "BLOCKBENCH=$blockbenchPath"
