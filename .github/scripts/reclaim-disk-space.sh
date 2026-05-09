#!/usr/bin/env bash
set -euo pipefail

echo "Disk usage before reclaim:"
df -h /

sudo rm -rf \
  /usr/share/dotnet           `# .NET SDKs` \
  /usr/share/swift            `# Swift toolchain` \
  /opt/ghc                    `# Haskell (GHC)` \
  /usr/local/.ghcup           `# Haskell (GHCup)` \
  /usr/local/julia*           `# Julia` \
  /usr/share/miniconda        `# Miniconda` \
  /opt/pipx                   `# Python (pipx)` \
  /usr/share/rust             `# Rust` \
  /usr/local/lib/android      `# Android SDKs` \
  /usr/local/share/chromium   `# Chromium` \
  /opt/microsoft              `# Microsoft Edge` \
  /opt/google                 `# Google Chrome` \
  /usr/lib/google-cloud-sdk   `# Google Cloud SDK` \
  /opt/az                     `# Azure CLI` \
  /usr/local/share/powershell `# PowerShell`

echo "Disk usage after reclaim:"
df -h /
