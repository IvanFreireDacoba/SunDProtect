#!/usr/bin/env bash
# Empaqueta el resourcepack/ ya generado (ver generate_icons.py) en un
# zip por version de Minecraft, cada uno con su propio pack_format
# (15 para 1.20-1.20.1, 34 para 1.21-1.21.1 -- confirmado contra la
# plantilla real "Resource pack format" de minecraft.wiki, no adivinado).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACK_DIR="$REPO_ROOT/resourcepack"
DIST_DIR="$REPO_ROOT/dist"

mkdir -p "$DIST_DIR"

for version in 1.20.1 1.21.1; do
  work="$(mktemp -d)"
  cp -r "$PACK_DIR/assets" "$work/assets"
  cp "$PACK_DIR/pack.mcmeta.$version" "$work/pack.mcmeta"

  zip_path="$DIST_DIR/sundprotect-resourcepack-mc$version.zip"
  rm -f "$zip_path"
  (cd "$work" && zip -qr "$zip_path" pack.mcmeta assets)
  rm -rf "$work"

  echo "Generado $zip_path"
done
