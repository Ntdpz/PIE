$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$allowed = @{
    'app' = @(':core:domain', ':core:data', ':core:designsystem', ':feature:home', ':feature:editor', ':feature:history', ':feature:settings')
    'core/domain' = @()
    'core/data' = @(':core:domain')
    'core/designsystem' = @()
    'feature/home' = @(':core:domain', ':core:designsystem')
    'feature/editor' = @(':core:domain', ':core:designsystem')
    'feature/history' = @(':core:domain', ':core:designsystem')
    'feature/settings' = @(':core:domain', ':core:designsystem')
}
$failures = [System.Collections.Generic.List[string]]::new()
foreach ($module in $allowed.Keys) {
    $buildPath = Join-Path $repoRoot "$module/build.gradle.kts"
    $buildText = Get-Content -LiteralPath $buildPath -Raw
    foreach ($match in [regex]::Matches($buildText, 'project\("([^"]+)"\)')) {
        if ($match.Groups[1].Value -notin $allowed[$module]) {
            $failures.Add("$module has forbidden dependency: $($match.Groups[1].Value)")
        }
    }
    $sourceRoot = Join-Path $repoRoot "$module/src/main"
    Get-ChildItem -LiteralPath $sourceRoot -Filter '*.kt' -Recurse | ForEach-Object {
        $source = Get-Content -LiteralPath $_.FullName -Raw
        if ($module -eq 'core/domain' -and $source -match '(?m)^import (android\.|androidx\.)') {
            $failures.Add("Domain imports Android: $($_.Name)")
        }
        if ($module.StartsWith('feature/') -and $source -match '(?m)^import (androidx\.room\.|com\.ntdpz\.pie\.core\.data\.)') {
            $failures.Add("Feature reaches into persistence: $($_.Name)")
        }
        if ($source -match '(?m)^import com\.ntdpz\.pie\.feature\.([a-z]+)\.') {
            if ($module -ne 'app' -and !$module.EndsWith("/$($Matches[1])")) {
                $failures.Add("Cross-feature import: $($_.Name)")
            }
        }
    }
}
if ($failures.Count -gt 0) { throw ($failures -join [Environment]::NewLine) }
Write-Output 'Architecture boundaries verified for all 8 modules.'
