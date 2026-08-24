param(
    [ValidateSet('Production', 'Preview')]
    [string]$Target = 'Production'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

function Assert-Asset {
    param(
        [Parameter(Mandatory)]
        [bool]$Condition,

        [Parameter(Mandatory)]
        [string]$Message
    )

    if (-not $Condition) {
        throw "ASSET_CHECK_FAILED: $Message"
    }
}

try {
    $projectRoot = Split-Path -Parent $PSScriptRoot

    if ($Target -eq 'Production') {
        $assetRoot = Join-Path $projectRoot 'src\main\mobs\endermite\resources\assets\usless_mobs'
        $paths = [ordered]@{
            Geometry  = Join-Path $assetRoot 'geo\corrupted_silverfish.geo.json'
            Texture   = Join-Path $assetRoot 'textures\entity\corrupted_silverfish.png'
            Animation = Join-Path $assetRoot 'animations\corrupted_silverfish.animation.json'
        }
    }
    else {
        $assetRoot = Join-Path $projectRoot 'Modelle\Exports\corrupted_silverfish_v2'
        $paths = [ordered]@{
            Geometry   = Join-Path $assetRoot 'geo\corrupted_silverfish.geo.json'
            Texture    = Join-Path $assetRoot 'textures\entity\corrupted_silverfish.png'
            Animation  = Join-Path $assetRoot 'animations\corrupted_silverfish.animation.json'
            Blockbench = Join-Path $projectRoot 'Modelle\Editierbar\Corrupted Silverfish v2.bbmodel'
        }
    }

    foreach ($entry in $paths.GetEnumerator()) {
        Assert-Asset (Test-Path -LiteralPath $entry.Value -PathType Leaf) "$($entry.Key) file does not exist: $($entry.Value)"
    }

    $geometryJson = Get-Content -Raw -LiteralPath $paths.Geometry | ConvertFrom-Json
    $geometryProperty = $geometryJson.PSObject.Properties['minecraft:geometry']
    Assert-Asset ($null -ne $geometryProperty) 'minecraft:geometry property is missing.'
    Assert-Asset ($null -ne $geometryProperty.Value) 'minecraft:geometry property must not be null.'

    $geometryEntries = @($geometryProperty.Value)
    Assert-Asset ($geometryEntries.Count -eq 1) "Expected exactly one minecraft:geometry entry, found $($geometryEntries.Count)."

    $geometry = $geometryEntries[0]
    $declaredWidth = [int]$geometry.description.texture_width
    $declaredHeight = [int]$geometry.description.texture_height

    $allowedRgb = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    @(
        '24,21,31'
        '43,41,51'
        '89,96,107'
        '140,150,163'
        '201,209,218'
        '49,15,47'
        '122,23,61'
        '181,42,79'
        '239,92,120'
        '75,35,110'
        '216,137,255'
    ) | ForEach-Object { [void]$allowedRgb.Add($_) }

    $bitmap = $null
    try {
        $bitmap = [System.Drawing.Bitmap]::new($paths.Texture)

        Assert-Asset ($bitmap.Width -eq $declaredWidth) "Texture width $($bitmap.Width) does not match declared texture_width $declaredWidth."
        Assert-Asset ($bitmap.Height -eq $declaredHeight) "Texture height $($bitmap.Height) does not match declared texture_height $declaredHeight."
        Assert-Asset ($bitmap.Width -eq 128) "Texture width must be exactly 128, found $($bitmap.Width)."
        Assert-Asset ($bitmap.Height -eq 64) "Texture height must be exactly 64, found $($bitmap.Height)."
        Assert-Asset ([System.Drawing.Image]::IsAlphaPixelFormat($bitmap.PixelFormat)) "Texture pixel format $($bitmap.PixelFormat) is not alpha-capable."

        $visiblePixels = 0
        for ($y = 0; $y -lt $bitmap.Height; $y++) {
            for ($x = 0; $x -lt $bitmap.Width; $x++) {
                $pixel = $bitmap.GetPixel($x, $y)
                if ($pixel.A -eq 0) {
                    continue
                }

                $visiblePixels++
                $rgb = '{0},{1},{2}' -f $pixel.R, $pixel.G, $pixel.B
                Assert-Asset ($allowedRgb.Contains($rgb)) "Visible pixel at ($x,$y) uses disallowed RGB $rgb."
            }
        }

        Assert-Asset ($visiblePixels -gt 0) 'Texture must contain at least one visible pixel.'
    }
    finally {
        if ($null -ne $bitmap) {
            $bitmap.Dispose()
        }
    }

    $requiredBones = @(
        'root'
        'head'
        'front_shell'
        'middle_shell'
        'tail_shell'
        'left_front_leg'
        'right_front_leg'
        'left_middle_leg'
        'right_middle_leg'
        'left_back_leg'
        'right_back_leg'
        'left_mandible'
        'right_mandible'
        'tail_tip'
        'corruption_spine_1'
        'corruption_spine_2'
        'corruption_spine_3'
        'corruption_crystal_left'
        'corruption_crystal_right'
    )

    $bonesProperty = $geometry.PSObject.Properties['bones']
    Assert-Asset ($null -ne $bonesProperty) 'geometry.bones property is missing.'
    Assert-Asset ($null -ne $bonesProperty.Value) 'geometry.bones property must not be null.'

    $bones = @($bonesProperty.Value)
    $boneNames = @($bones | ForEach-Object { [string]$_.name })
    $boneNameSet = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    $boneNames | ForEach-Object { [void]$boneNameSet.Add($_) }

    foreach ($requiredBone in $requiredBones) {
        Assert-Asset ($boneNameSet.Contains($requiredBone)) "Required bone is missing: $requiredBone."
    }

    $cubes = @(
        foreach ($bone in $bones) {
            if ($null -ne $bone.cubes) {
                @($bone.cubes)
            }
        }
    )

    foreach ($cube in $cubes) {
        $sizeProperty = $cube.PSObject.Properties['size']
        Assert-Asset ($null -ne $sizeProperty) 'Cube size property is missing.'
        Assert-Asset ($null -ne $sizeProperty.Value) 'Cube size property must not be null.'

        $size = @($sizeProperty.Value)
        Assert-Asset ($size.Count -eq 3) "Every cube size must contain exactly 3 dimensions; found $($size.Count)."

        foreach ($dimension in $size) {
            $numericDimension = [double]$dimension
            Assert-Asset ($numericDimension -gt 0) "Every cube dimension must be greater than 0; found $dimension."
        }
    }

    Assert-Asset ($cubes.Count -ge 18) "Expected at least 18 cubes, found $($cubes.Count)."

    $animationJson = Get-Content -Raw -LiteralPath $paths.Animation | ConvertFrom-Json
    $idleProperty = $animationJson.animations.PSObject.Properties['animation.corrupted_silverfish.idle']
    Assert-Asset ($null -ne $idleProperty) 'Required animation animation.corrupted_silverfish.idle is missing.'

    $idleAnimation = $idleProperty.Value
    Assert-Asset (($idleAnimation.loop -is [bool]) -and $idleAnimation.loop) 'Idle animation loop must be true.'

    $animationLength = [double]$idleAnimation.animation_length
    Assert-Asset (($animationLength -ge 1.0) -and ($animationLength -le 1.5)) "Idle animation length must be between 1.0 and 1.5 inclusive; found $animationLength."

    $animatedBonesProperty = $idleAnimation.PSObject.Properties['bones']
    Assert-Asset ($null -ne $animatedBonesProperty) 'Idle animation bones property is missing.'
    Assert-Asset ($null -ne $animatedBonesProperty.Value) 'Idle animation bones property must not be null.'

    $animatedBoneProperties = @($animatedBonesProperty.Value.PSObject.Properties)
    Assert-Asset ($animatedBoneProperties.Count -ge 5) "Expected at least 5 animated bones, found $($animatedBoneProperties.Count)."

    foreach ($animatedBoneProperty in $animatedBoneProperties) {
        Assert-Asset ($boneNameSet.Contains($animatedBoneProperty.Name)) "Animated bone does not exist in geometry: $($animatedBoneProperty.Name)."
        Assert-Asset ($null -ne $animatedBoneProperty.Value) "Animated bone has null channel data: $($animatedBoneProperty.Name)."
        $channelProperties = @(
            $animatedBoneProperty.Value.PSObject.Properties |
                Where-Object { $_.MemberType -eq [System.Management.Automation.PSMemberTypes]::NoteProperty }
        )
        Assert-Asset ($channelProperties.Count -gt 0) "Animated bone has no channels: $($animatedBoneProperty.Name)."
    }

    if ($Target -eq 'Preview') {
        $blockbenchJson = Get-Content -Raw -LiteralPath $paths.Blockbench | ConvertFrom-Json
        Assert-Asset ($blockbenchJson.meta.model_format -eq 'geckolib_model') "Blockbench meta.model_format must be geckolib_model; found $($blockbenchJson.meta.model_format)."
        Assert-Asset ([int]$blockbenchJson.resolution.width -eq 128) "Blockbench resolution width must be 128; found $($blockbenchJson.resolution.width)."
        Assert-Asset ([int]$blockbenchJson.resolution.height -eq 64) "Blockbench resolution height must be 64; found $($blockbenchJson.resolution.height)."
    }

    Write-Output ('ASSET_CHECK=PASS;TARGET={0};BONES={1};CUBES={2};ANIMATED_BONES={3}' -f $Target, $bones.Count, $cubes.Count, $animatedBoneProperties.Count)
}
catch {
    $message = $_.Exception.Message
    if (-not $message.StartsWith('ASSET_CHECK_FAILED:')) {
        $message = "ASSET_CHECK_FAILED: $message"
    }

    [Console]::Error.WriteLine($message)
    exit 1
}
