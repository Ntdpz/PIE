$ErrorActionPreference = 'Stop'
$designRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../design'))
$documentPath = Join-Path $designRoot 'PIE.pen'
$documentText = [IO.File]::ReadAllText($documentPath)
$document = $documentText | ConvertFrom-Json -Depth 100
$expectedFrames = @('OXucF', 'CAfaV', 'VGQFw', 'u8Ddh', 'p5QJT', 'zS2l2', 'm16X2', 'hBgUz')
foreach ($frameId in $expectedFrames) {
    if ($document.children.id -notcontains $frameId) {
        throw "Missing approved R2 frame: $frameId"
    }
}
$imagePaths = [regex]::Matches($documentText, '"url":\s*"([^"\r\n]+)"') |
    ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
foreach ($imagePath in $imagePaths) {
    if ([IO.Path]::IsPathRooted($imagePath) -or $imagePath -match '^https?://') {
        throw "Design images must use local relative paths: $imagePath"
    }
    $resolvedImage = [IO.Path]::GetFullPath((Join-Path $designRoot $imagePath))
    if (-not $resolvedImage.StartsWith($designRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Image path must remain inside design/: $imagePath"
    }
    if (-not (Test-Path -LiteralPath $resolvedImage -PathType Leaf)) {
        throw "Missing design image: $imagePath"
    }
}
Write-Output "Pen JSON valid; all $($expectedFrames.Count) approved R2 frames and $(@($imagePaths).Count) local images present."
