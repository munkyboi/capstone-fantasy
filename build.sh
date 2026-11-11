#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
find build/classes -type f -name '*.class' -delete
find src -name '*.java' -print > build/sources.txt
javac --release 17 -d build/classes @build/sources.txt
rm -rf build/classes/assets
while IFS= read -r asset; do
    mkdir -p "build/classes/$(dirname "$asset")"
    cp "$asset" "build/classes/$asset"
done <<'ASSETS'
assets/backgrounds/camp-courtyard-layer-v2.png
assets/backgrounds/camp-courtyard-v1.png
assets/backgrounds/camp-distant-v1.png
assets/backgrounds/camp-near-layer-v1.png
assets/backgrounds/camp-parallax-base-v1.png
assets/backgrounds/camp-sky-v1.png
assets/backgrounds/campus-courtyard-v1.png
assets/backgrounds/campus-route-v1.png
assets/backgrounds/stage-1-campus-entrance-v2.png
assets/backgrounds/stage-2-library-hall-v2.png
assets/backgrounds/stage-3-training-ground-v3.png
assets/backgrounds/stage-4-old-campus-v2.png
assets/backgrounds/stage-5-crystal-chamber-v3.png
assets/backgrounds/title-campus-v1.png
assets/sprites/camp-angel-v1.png
assets/sprites/camp-bogart-v2.png
assets/sprites/camp-charice-v1.png
assets/sprites/camp-fire-v1.png
assets/sprites/camp-zack-v1.png
assets/sprites/crystal-guardian-v1.png
assets/sprites/library-wisp-v2.png
assets/sprites/stone-sentinel-v1.png
assets/sprites/student-party-idle-v1.png
assets/ui/carbon-market-icon-v1.png
assets/ui/hand-pointer-reference.png
assets/ui/reward-icons-v1.png
assets/ui/reward-uniform-v1.png
assets/ui/title-logo-v1.png
ASSETS
jar --create --file build/capstone-fantasy.jar --main-class game.Main -C build/classes .
printf '%s\n' 'Built build/capstone-fantasy.jar'
