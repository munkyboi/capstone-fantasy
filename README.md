# Capstone Fantasy

A student-themed, turn-based Java RPG for Object-Oriented Programming at the University of San Jose–Recoletos. The project is in its foundation stage.

## Build and run

Install JDK 17 or newer. From the project folder, run:

```sh
sh build.sh
java -jar build/capstone-fantasy.jar
```

The current build opens a fixed-size 960 × 540 (16:9) game window. Use **Up/Down** and **Enter** or **Space** on the main menu. On character selection, use **Left/Right** and **Enter** or **Space** to choose or remove students; choose three, then press **Down** and **Enter** to continue. **Escape** goes back. The first encounter has two Library Wisps; after victory, press Enter to visit camp. At camp, use **Up/Down** to choose a student, **Enter** to use a First-Aid Kit, and **C** to continue to the Wisp and Stone Sentinel encounter. In battle use **Up/Down** and **Enter** or **Space** to choose Attack, Ability, Item, or Defend. A cursor shows the target on the battlefield: use the arrow keys to move among living allies or enemies, Enter or Space to confirm, and Escape to cancel without using a turn or item. Zack's Arcane Burst targets all living enemies. Use **Page Up/Page Down** to scroll longer battle messages. Angel can attack normally or use Healing Kiss. After the second victory, Enter opens the completion screen; press Enter again to return to the menu. **Load Game** is not implemented. A graphical desktop is required.

## Direction

The game is intended to be suitable for a school audience, without profanity, innuendo, sexual content, or immoral themes. Its battle layout takes inspiration from classic 16-bit JRPGs and [Sara's Turn Based Battle System](https://sarajs.itch.io/saras-turn-based-battle-system); the application is being built in Java Swing. The battlefield and student portraits are rendered at low resolution and scaled with nearest-neighbor interpolation for a pixel-art appearance.

## AI assistance

OpenAI Codex helped plan the game and draft the Java Swing main menu, character-selection, battle and camp screens, progression and battle logic, and build script. OpenAI's built-in image generation tool created the student concept, idle artwork, Library Wisp and Stone Sentinel sprites, and adapted a Kenney sample into the selection background. This disclosure will be updated as work progresses. The student is responsible for understanding and explaining the submitted work.

## Art references and assets

- **Student party concept:** AI-generated design reference, not yet a production sprite sheet. Prompt: "Create an original cohesive character reference sheet for a wholesome school-themed 16-bit JRPG, four separate full-body pixel art student heroes aligned left to right on a simple neutral background with ample separation, no text. Zack: chubby otaku student with staff, mage; Bogart: first-year ROTC student with military buzz cut, modest uniform and sword; Charice: confident student hunter with two hand crossbows; Angel: muscular male bouncer monk healer, friendly expressive demeanor and modest clothing. All characters adults, fully clothed, SFW, friendly adventurous tone, consistent crisp pixel art aesthetic, distinct silhouettes, no copyrighted characters, no logos, no lettering. Concept sheet for later sprite production."
- **Student idle artwork:** `assets/sprites/student-party-idle-v1.png`, AI-generated transparent sheet containing four individual idle figures displayed separately by the selection screen. Prompt: "Create one game-ready pixel art sprite sheet for an original wholesome school-themed 16-bit JRPG. Four distinct full-body adult student heroes in a single horizontal row, each separated by generous empty transparent space, same height and same consistent scale, facing slightly to the right in idle standing poses. Zack chubby otaku mage with staff and glasses, Bogart ROTC first-year with buzz cut and sword, Charice confident hunter holding two hand crossbows, Angel muscular male monk healer, flamboyant friendly smile, modest clothing. Clear readable silhouettes, sprite game art, crisp hard-edged pixels, simple outlines and limited colors, no gradients, no text, no labels, no additional characters, SFW. True transparent background."
- **Campus courtyard:** `assets/backgrounds/campus-courtyard-v1.png`, AI-generated adaptation using Kenney's Tiny Dungeon sample as an input. Prompt: "Adapt this CC0 Kenney Tiny Dungeon sample into an original, clean, side-view 16-bit JRPG school campus stone courtyard backdrop for a character selection screen. Preserve its crisp pixel-art material language and muted blue stone colors, transform dungeon structures into school stone walls, a few windows, floor paving and a small glowing crystal. Wide landscape composition, subdued dark navy tones, central area calm and uncluttered for UI cards, no people, no writing, no logos, no watermark. Fully SFW."
- **Library Wisp:** `assets/sprites/library-wisp-v1.png`, generated with OpenAI's built-in image tool. Prompt: "An original wholesome fantasy JRPG enemy sprite: a small floating Library Wisp, a round glowing blue spirit with expressive eyes and a tiny floating book, clear silhouette, no scary details, school-appropriate, 16-bit pixel art, isolated full figure, transparent background, crisp pixel edges, no text, no shadows outside subject, no other characters."
- **Library Wisp revision:** `assets/sprites/library-wisp-v2.png`, edited from v1 with OpenAI's built-in image tool. Prompt: "Make it a formidable magical guardian for a school-friendly JRPG. Preserve the floating blue spectral flame identity, magical book and transparent background, but replace the cute smiling face with focused, determined glowing eyes, an imposing swept flame silhouette, and a restrained aura of magical energy. Clean 16-bit pixel art, legible at small size, isolated sprite, no text, no gore, no horror, no skulls, no symbols of evil, no profanity, fully SFW."
- **Stone Sentinel:** `assets/sprites/stone-sentinel-v1.png`, generated with OpenAI's built-in image tool. Prompt: "Original formidable stone sentinel guardian enemy for a wholesome 16-bit school fantasy JRPG, full-body standing idle sprite, made of blue-gray carved stone with sturdy blocky armor and subtle cyan crystal light, confident guardian stance, readable at small scale, facing left, transparent background, crisp pixel art, no gore, no horror, no sinister symbols, no text, no watermark, one character only, SFW."
- **Tiny Dungeon:** Kenney, [source](https://kenney.nl/assets/tiny-dungeon), CC0 1.0. The downloaded pack and its license are in `assets/kenney-tiny-dungeon/`.
- **Battle layout reference:** [Sara's Turn Based Battle System](https://sarajs.itch.io/saras-turn-based-battle-system). No assets or code from that project are included.

## Known limitations

Two encounters and a basic camp are implemented. HP, knockouts, and remaining First-Aid Kits carry between them; kits cannot revive knocked-out students. Equipment, the remaining encounters, a full inventory, and saving are not implemented yet. Stats and balance are provisional, the artwork has no animation frames yet, and the title screen's campus and crystal remain simple shapes drawn by the application.
