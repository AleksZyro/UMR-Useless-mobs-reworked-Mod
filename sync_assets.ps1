#
# sync_assets.ps1
# Kopiert deine bearbeiteten Texturen + Modelle aus den Editier-Ordnern
# (Grafiken/, Modelle/) zurueck in die echten Mod-Assets-Pfade.
#
# Workflow:
#   1. Datei in Grafiken/entity/foo.png oder Modelle/foo.geo.json aendern
#   2. Dieses Skript laufen lassen: .\sync_assets.ps1
#   3. .\gradlew.bat runClient   (oder nur Resources neu laden)
#
# Optional -DryRun zeigt was kopiert wuerde ohne tatsaechliches Kopieren.
#

param([switch]$DryRun)

$root = (Resolve-Path .).Path
$assets = Join-Path $root "src\main\resources\assets\usless_mobs"

$mappings = @(
    @{ Src = "Grafiken\entity";     Dst = "$assets\textures\entity";     Pattern = "*.png" }
    @{ Src = "Grafiken\item";       Dst = "$assets\textures\item";       Pattern = "*.png" }
    @{ Src = "Grafiken\block";      Dst = "$assets\textures\block";      Pattern = "*.png" }
    @{ Src = "Grafiken\mob_effect"; Dst = "$assets\textures\mob_effect"; Pattern = "*.png" }
    @{ Src = "Grafiken\slot";       Dst = "$assets\textures\slot";       Pattern = "*.png" }
    @{ Src = "Modelle";             Dst = "$assets\geo";                 Pattern = "*.geo.json" }
    @{ Src = "Modelle";             Dst = "$assets\animations";          Pattern = "*.animation.json" }
)

$copied = 0
$skipped = 0

foreach ($m in $mappings) {
    if (-not (Test-Path $m.Src)) { continue }
    if (-not (Test-Path $m.Dst)) { New-Item -ItemType Directory -Path $m.Dst -Force | Out-Null }

    Get-ChildItem -Path $m.Src -Filter $m.Pattern -File | ForEach-Object {
        $target = Join-Path $m.Dst $_.Name
        $needsCopy = $true
        if (Test-Path $target) {
            $srcInfo = Get-Item $_.FullName
            $dstInfo = Get-Item $target
            if ($srcInfo.LastWriteTime -le $dstInfo.LastWriteTime -and $srcInfo.Length -eq $dstInfo.Length) {
                $needsCopy = $false
            }
        }

        if ($needsCopy) {
            if ($DryRun) {
                Write-Output "[would copy] $($_.Name) -> $($m.Dst)"
            } else {
                Copy-Item -Path $_.FullName -Destination $target -Force
                Write-Output "[copied]     $($_.Name) -> $($m.Dst)"
            }
            $copied++
        } else {
            $skipped++
        }
    }
}

Write-Output ""
if ($DryRun) {
    Write-Output "DryRun done. $copied would be copied, $skipped were already up-to-date."
} else {
    Write-Output "Sync done. $copied copied, $skipped skipped (already up-to-date)."
    Write-Output "Now run: .\gradlew.bat runClient  (or just F3+T for resource reload if MC is already running)"
}
