#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
find build/classes -type f -name '*.class' -delete
find src -name '*.java' -print > build/sources.txt
javac --release 17 -d build/classes @build/sources.txt
mkdir -p build/classes/assets/sprites build/classes/assets/backgrounds build/classes/assets/ui
cp assets/sprites/*.png build/classes/assets/sprites/
cp assets/backgrounds/*.png build/classes/assets/backgrounds/
cp assets/ui/*.png build/classes/assets/ui/
jar --create --file build/capstone-fantasy.jar --main-class game.Main -C build/classes .
printf '%s\n' 'Built build/capstone-fantasy.jar'
