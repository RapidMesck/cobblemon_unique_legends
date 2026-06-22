param(
    [int]$StartDex = 1,
    [int]$EndDex = 1025,
    [int]$TextureSize = 32,
    [int]$Padding = 1,
    [switch]$Overwrite
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$packRoot = Join-Path $repoRoot "texture"
$pokemonTextureDir = Join-Path $packRoot "assets/minecraft/textures/item/unique_legends/pokemon"
$pokemonModelDir = Join-Path $packRoot "assets/minecraft/models/item/unique_legends/pokemon"
$itemModelDir = Join-Path $packRoot "assets/minecraft/models/item"
$netherStarModel = Join-Path $itemModelDir "nether_star.json"
$downloadedList = Join-Path $packRoot "downloaded-pokeapi-sprites.txt"
$missingList = Join-Path $packRoot "missing-pokeapi-sprites.txt"

$sources = @(
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/versions/generation-viii/icons/{0}.png",
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/versions/generation-vii/icons/{0}.png",
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/{0}.png"
)

New-Item -ItemType Directory -Force -Path $pokemonTextureDir | Out-Null
New-Item -ItemType Directory -Force -Path $pokemonModelDir | Out-Null
New-Item -ItemType Directory -Force -Path $itemModelDir | Out-Null

Add-Type -AssemblyName System.Drawing

function Save-SquarePng {
    param(
        [string]$SourcePath,
        [string]$TargetPath,
        [int]$Size
    )

    $sourceStream = [System.IO.File]::OpenRead($SourcePath)
    try {
        $sourceImage = [System.Drawing.Image]::FromStream($sourceStream)
        try {
            $bounds = Get-ContentBounds -Image $sourceImage
            $bitmap = New-Object System.Drawing.Bitmap $Size, $Size
            $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
            try {
                $graphics.Clear([System.Drawing.Color]::Transparent)
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                $drawableSize = [Math]::Max(1, $Size - ($Padding * 2))
                $scale = [Math]::Min($drawableSize / $bounds.Width, $drawableSize / $bounds.Height)
                $width = [Math]::Max(1, [int][Math]::Round($bounds.Width * $scale))
                $height = [Math]::Max(1, [int][Math]::Round($bounds.Height * $scale))
                $x = [int][Math]::Floor(($Size - $width) / 2)
                $y = [int][Math]::Floor(($Size - $height) / 2)
                $sourceRect = New-Object System.Drawing.Rectangle $bounds.X, $bounds.Y, $bounds.Width, $bounds.Height
                $targetRect = New-Object System.Drawing.Rectangle $x, $y, $width, $height
                $graphics.DrawImage($sourceImage, $targetRect, $sourceRect, [System.Drawing.GraphicsUnit]::Pixel)
                $bitmap.Save($TargetPath, [System.Drawing.Imaging.ImageFormat]::Png)
            } finally {
                $graphics.Dispose()
                $bitmap.Dispose()
            }
        } finally {
            $sourceImage.Dispose()
        }
    } finally {
        $sourceStream.Dispose()
    }
}

function Get-ContentBounds {
    param([System.Drawing.Image]$Image)

    $bitmap = New-Object System.Drawing.Bitmap $Image
    try {
        $minX = $bitmap.Width
        $minY = $bitmap.Height
        $maxX = -1
        $maxY = -1

        for ($y = 0; $y -lt $bitmap.Height; $y++) {
            for ($x = 0; $x -lt $bitmap.Width; $x++) {
                if ($bitmap.GetPixel($x, $y).A -gt 8) {
                    if ($x -lt $minX) { $minX = $x }
                    if ($y -lt $minY) { $minY = $y }
                    if ($x -gt $maxX) { $maxX = $x }
                    if ($y -gt $maxY) { $maxY = $y }
                }
            }
        }

        if ($maxX -lt $minX -or $maxY -lt $minY) {
            return New-Object System.Drawing.Rectangle 0, 0, $bitmap.Width, $bitmap.Height
        }

        return New-Object System.Drawing.Rectangle $minX, $minY, ($maxX - $minX + 1), ($maxY - $minY + 1)
    } finally {
        $bitmap.Dispose()
    }
}

function Try-DownloadSprite {
    param(
        [int]$DexNumber,
        [string]$TargetPath
    )

    $tempPath = Join-Path ([System.IO.Path]::GetTempPath()) "unique-legends-pokeapi-$DexNumber.png"
    foreach ($source in $sources) {
        $url = [string]::Format($source, $DexNumber)
        try {
            Invoke-WebRequest -Uri $url -OutFile $tempPath -UseBasicParsing -TimeoutSec 30
            if ((Test-Path -LiteralPath $tempPath) -and ((Get-Item -LiteralPath $tempPath).Length -gt 0)) {
                Save-SquarePng -SourcePath $tempPath -TargetPath $TargetPath -Size $TextureSize
                Remove-Item -LiteralPath $tempPath -Force
                return $url
            }
        } catch {
            if (Test-Path -LiteralPath $tempPath) {
                Remove-Item -LiteralPath $tempPath -Force
            }
        }
    }

    return $null
}

function Write-PokemonModel {
    param([int]$DexNumber)

    $modelPath = Join-Path $pokemonModelDir "$DexNumber.json"
    $json = @"
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "minecraft:item/unique_legends/pokemon/$DexNumber"
  }
}
"@
    Set-Content -LiteralPath $modelPath -Value $json -Encoding UTF8
}

function Write-NetherStarOverrides {
    param([int[]]$DexNumbers)

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("{")
    $lines.Add('  "parent": "minecraft:item/generated",')
    $lines.Add('  "textures": {')
    $lines.Add('    "layer0": "minecraft:item/nether_star"')
    $lines.Add('  },')
    $lines.Add('  "overrides": [')

    $overrideBlocks = New-Object System.Collections.Generic.List[string]
    foreach ($dexNumber in ($DexNumbers | Sort-Object -Unique)) {
        $overrideBlocks.Add(@"
    {
      "predicate": {
        "custom_model_data": $dexNumber
      },
      "model": "minecraft:item/unique_legends/pokemon/$dexNumber"
    }
"@)
    }

    $overrideBlocks.Add(@"
    {
      "predicate": {
        "custom_model_data": 900009
      },
      "model": "minecraft:item/unique_legends/gui/locked_fallback"
    }
"@)

    for ($i = 0; $i -lt $overrideBlocks.Count; $i++) {
        $block = $overrideBlocks[$i]
        if ($i -lt $overrideBlocks.Count - 1) {
            $lines.Add($block + ",")
        } else {
            $lines.Add($block)
        }
    }

    $lines.Add("  ]")
    $lines.Add("}")
    Set-Content -LiteralPath $netherStarModel -Value ($lines -join [Environment]::NewLine) -Encoding UTF8
}

$downloaded = New-Object System.Collections.Generic.List[int]
$downloadLog = New-Object System.Collections.Generic.List[string]
$missing = New-Object System.Collections.Generic.List[int]

foreach ($dexNumber in $StartDex..$EndDex) {
    $targetPath = Join-Path $pokemonTextureDir "$dexNumber.png"
    if ((Test-Path -LiteralPath $targetPath) -and -not $Overwrite) {
        Write-PokemonModel -DexNumber $dexNumber
        $downloaded.Add($dexNumber)
        $downloadLog.Add("$dexNumber existing")
        Write-Host "[$dexNumber] existing"
        continue
    }

    $sourceUrl = Try-DownloadSprite -DexNumber $dexNumber -TargetPath $targetPath
    if ($sourceUrl) {
        Write-PokemonModel -DexNumber $dexNumber
        $downloaded.Add($dexNumber)
        $downloadLog.Add("$dexNumber $sourceUrl")
        Write-Host "[$dexNumber] downloaded"
    } else {
        $missing.Add($dexNumber)
        Write-Warning "[$dexNumber] missing from configured PokeAPI sprite sources"
    }
}

Write-NetherStarOverrides -DexNumbers $downloaded.ToArray()
Set-Content -LiteralPath $downloadedList -Value $downloadLog -Encoding UTF8
Set-Content -LiteralPath $missingList -Value $missing -Encoding UTF8

Write-Host ""
Write-Host "Downloaded/existing sprites: $($downloaded.Count)"
Write-Host "Missing sprites: $($missing.Count)"
Write-Host "Updated: $netherStarModel"
Write-Host "Downloaded log: $downloadedList"
Write-Host "Missing log: $missingList"
