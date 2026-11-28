$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
Set-Location $repoRoot

git config core.hooksPath .githooks
Write-Host "Configured local git hooks path to .githooks"
Write-Host "Pre-commit docs validation is now enabled for this repository."
