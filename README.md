# Capstone Fantasy

Capstone Fantasy is a student-themed Java Swing RPG created for an Object-Oriented Programming capstone. Choose a party of students, follow the campus route, fight monsters, and prepare for the Crystal Guardian.

## Build and run

Install JDK 17 or newer. From the project folder:

```sh
sh build.sh
java -jar build/capstone-fantasy.jar
```

The window is resizable. The game uses a 16:9 viewport with nearest-neighbour scaling for its pixel art.

## Playing

Use the arrow keys to navigate and Enter or Space to confirm. Instructions and additional shortcuts appear on each screen.

- Choose Zack, Bogart, Charice and/or Angel for the active party; the default party size is three and can be changed in Settings.
- Select encounters along the five-stage campus route. A completed stage can be replayed. After a victory, choose **Explore** to repeat the stage or **Camp** to return to camp.
- In battle, use Attack, Ability, Item, Defend or Escape. Combatants act as their action-point (AP) bars fill. Characters have distinct skills; enemies include Wisps, Attack and Support Sentinels, and the Crystal Guardian.
- At camp, use items, manage equipment, spend level-up points, and save. The shared inventory includes First-Aid Kits, White Flowers, and equipment with weapon, armor and accessory slots.
- Defeat the Mysterious Merchant after stage 3 to unlock the Carbon Market. Monsters award XP and Petot, and may drop items.

Normal stage battles start with the party's AP bars full. When enabled, non-boss stages can instead begin with an ambush, giving enemies an opportunity to act first. This is a gameplay variation of the assignment's party-first rule; it should not be presented as strict party-first combat.

## Settings

The title-screen settings include battle speed (5, 10, 15 or 20 base AP per second), difficulty, XP boost, level cap (20–100 in steps of 10), party size (1–4), ambushes and the Greed is Good Petot bonus. Difficulty also determines XP growth. Settings can be reset to defaults from the same screen.

## Saving and loading

At camp, press **S** to choose one of three save slots. Text and binary saves are separate; select the format with the left and right arrow keys. Load a slot from the title screen. Replacing a save requires confirmation.

## Code guide

- `src/game/model/GameCharacter.java`, `Student.java`, `Enemy.java`, `Hero.java` and the `Combatant` / `Targetable` interfaces define combatants and their shared behavior.
- `src/game/model/Battle.java`, `EncounterRules.java` and `GameProgress.java` manage battle state, encounter rules and progression.
- `src/game/model/Item.java`, `ItemBag.java`, `Inventory.java` and `Equipment.java` implement the item hierarchy, the generic shared bag and equipment. `ItemNotFoundException` handles unknown item IDs.
- `src/game/io/SaveStore.java` and `SaveException.java` handle persistence and save errors.
- `src/game/view/` contains Swing screens for the route, battle, camp, inventory, settings and menus. `src/game/Main.java` starts the application.

The assignment's full requirements and final deliverables still need to be checked against the current implementation, including the party-first requirement and the requested UML documentation.

## Tests

After building, compile the regression checks and run an individual check, for example:

```sh
mkdir -p build/test-classes
javac -cp build/capstone-fantasy.jar -d build/test-classes tests/*.java
java -Djava.awt.headless=true -cp build/capstone-fantasy.jar:build/test-classes EquipmentSystemTest
```

Additional checks are in `tests/`; some UI checks may require a graphical environment. Automated checks do not replace an in-game playtest.

## Optional classic fonts

Headings use Eight Bit Dragon by Chequered Ink and body labels use Gome Pixel by Fitrah Type when installed locally. Neither font is included in this repository or the game archive. On macOS, install the original `EightBitDragon-anqx.ttf` and `GomePixel-Regular.otf` in `~/Library/Fonts/`. Consult the application font settings for alternative paths.

[Eight Bit Dragon](https://www.fontspace.com/eight-bit-dragon-font-f30428) has a non-commercial license restricting redistribution. [Gome Pixel](https://www.fontspace.com/gome-pixel-font-f119163) is a personal-use demo. Review their licenses before distributing the game or using it commercially.

## Direction

The game is intended for a school audience. Its battle layout takes inspiration from classic JRPGs and [Sara's Turn Based Battle System](https://sarajs.itch.io/saras-turn-based-battle-system).

## Art references and assets

- **Title logo:** `assets/ui/title-logo-v1.png`, generated with OpenAI's built-in image tool. Prompt: "Transparent title logo for a 16-bit inspired school-friendly fantasy JRPG. Exact text, clearly spelled, two centered lines: CAPSTONE and FANTASY. Elegant chunky readable pixel-art fantasy lettering, warm ivory and gold edges with deep navy outline, a small luminous cyan crystal integrated between lines, restrained decorative flourishes. Cohesive with a blue nighttime campus adventure. Transparent background, text only plus subtle crystal, no other lettering, no watermark, no copyrighted game logos, family-friendly. Wide composition."
- **Title background:** `assets/backgrounds/title-campus-v1.png`, generated with OpenAI's built-in image tool. Prompt: "16:9 full-screen background for an original wholesome 16-bit pixel-art student JRPG main menu. A serene old university campus courtyard at twilight, a small glowing cyan crystal near the center, elegant blue-gray stone architecture and warm windows, moonlit trees and paved ground; nostalgic 16-bit pixel-art, crisp defined pixels, deep navy, teal and muted gold, magical but welcoming, no people, no weapons, no scary imagery, no text, no logo, no watermark. Keep middle and lower center dark and visually calm so selectable menu text can be placed on top. School appropriate."
- **Campus route background:** `assets/backgrounds/campus-route-v1.png`, generated with OpenAI's built-in image tool using the title background as a style reference. Prompt: "Use the provided campus image only as a palette and 16-bit pixel-art style reference. Create a NEW wide overhead 3/4-view campus world-map background for an original wholesome JRPG. Show a compact university campus at twilight with clusters of small blue-gray stone academic buildings, trees, courtyards and lamps, arranged across the scene from left to right, with a subtle glowing crystal structure toward the far right. Deep navy, teal and warm gold matching the reference; crisp defined pixel art. Leave a clear unobstructed horizontal central band for a separate interactive route and markers that will be drawn by code. No characters, no roads, no map nodes, no lettering, no UI, no logos, no watermark. Friendly school-appropriate mood. Wide 16:9 landscape."
- **Camp background:** `assets/backgrounds/camp-courtyard-v1.png`, generated with OpenAI's built-in image tool. Prompt: "16:9 background for the camp screen of an original, wholesome 16-bit pixel-art student JRPG. Quiet sheltered university courtyard at twilight, blue-gray stone arches and benches, a gentle small lantern glow, trees, distant cyan crystal accents. No people, no text, no watermark, no fire, no weapons, no dark or frightening imagery. Deep navy-blue, teal and warm gold. Keep center fairly dark and calm to allow three high-contrast party status panels to overlay it. Crisp retro pixel art, school-appropriate, harmonious with a classic fantasy game."
- **Student party concept:** AI-generated design reference, not yet a production sprite sheet. Prompt: "Create an original cohesive character reference sheet for a wholesome school-themed 16-bit JRPG, four separate full-body pixel art student heroes aligned left to right on a simple neutral background with ample separation, no text. Zack: chubby otaku student with staff, mage; Bogart: first-year ROTC student with military buzz cut, modest uniform and sword; Charice: confident student hunter with two hand crossbows; Angel: muscular male bouncer monk healer, friendly expressive demeanor and modest clothing. All characters adults, fully clothed, SFW, friendly adventurous tone, consistent crisp pixel art aesthetic, distinct silhouettes, no copyrighted characters, no logos, no lettering. Concept sheet for later sprite production."
- **Student idle artwork:** `assets/sprites/student-party-idle-v1.png`, AI-generated transparent sheet containing four individual idle figures displayed separately by the selection screen. Prompt: "Create one game-ready pixel art sprite sheet for an original wholesome school-themed 16-bit JRPG. Four distinct full-body adult student heroes in a single horizontal row, each separated by generous empty transparent space, same height and same consistent scale, facing slightly to the right in idle standing poses. Zack chubby otaku mage with staff and glasses, Bogart ROTC first-year with buzz cut and sword, Charice confident hunter holding two hand crossbows, Angel muscular male monk healer, flamboyant friendly smile, modest clothing. Clear readable silhouettes, sprite game art, crisp hard-edged pixels, simple outlines and limited colors, no gradients, no text, no labels, no additional characters, SFW. True transparent background."
- **Campus courtyard:** `assets/backgrounds/campus-courtyard-v1.png`, AI-generated adaptation using Kenney's Tiny Dungeon sample as an input. Prompt: "Adapt this CC0 Kenney Tiny Dungeon sample into an original, clean, side-view 16-bit JRPG school campus stone courtyard backdrop for a character selection screen. Preserve its crisp pixel-art material language and muted blue stone colors, transform dungeon structures into school stone walls, a few windows, floor paving and a small glowing crystal. Wide landscape composition, subdued dark navy tones, central area calm and uncluttered for UI cards, no people, no writing, no logos, no watermark. Fully SFW."
- **Library Wisp:** `assets/sprites/library-wisp-v1.png`, generated with OpenAI's built-in image tool. Prompt: "An original wholesome fantasy JRPG enemy sprite: a small floating Library Wisp, a round glowing blue spirit with expressive eyes and a tiny floating book, clear silhouette, no scary details, school-appropriate, 16-bit pixel art, isolated full figure, transparent background, crisp pixel edges, no text, no shadows outside subject, no other characters."
- **Library Wisp revision:** `assets/sprites/library-wisp-v2.png`, edited from v1 with OpenAI's built-in image tool. Prompt: "Make it a formidable magical guardian for a school-friendly JRPG. Preserve the floating blue spectral flame identity, magical book and transparent background, but replace the cute smiling face with focused, determined glowing eyes, an imposing swept flame silhouette, and a restrained aura of magical energy. Clean 16-bit pixel art, legible at small size, isolated sprite, no text, no gore, no horror, no skulls, no symbols of evil, no profanity, fully SFW."
- **Library Wisp battle-facing revision:** `assets/sprites/library-wisp-v3.png`, edited from v2 with OpenAI’s built-in image tool to face the party to the left while preserving its blue flames and floating book.
- **Stone Sentinel:** `assets/sprites/stone-sentinel-v1.png`, generated with OpenAI's built-in image tool. Prompt: "Original formidable stone sentinel guardian enemy for a wholesome 16-bit school fantasy JRPG, full-body standing idle sprite, made of blue-gray carved stone with sturdy blocky armor and subtle cyan crystal light, confident guardian stance, readable at small scale, facing left, transparent background, crisp pixel art, no gore, no horror, no sinister symbols, no text, no watermark, one character only, SFW."
- **Tiny Dungeon:** Kenney, [source](https://kenney.nl/assets/tiny-dungeon), CC0 1.0. The downloaded pack and its license are in `assets/kenney-tiny-dungeon/`.
- **Battle layout reference:** [Sara's Turn Based Battle System](https://sarajs.itch.io/saras-turn-based-battle-system). No assets or code from that project are included.
