# Capstone Fantasy

A student-themed, turn-based Java RPG for Object-Oriented Programming at the University of San Jose–Recoletos. The project is in its foundation stage.

## Build and run

Install JDK 17 or newer. From the project folder, run:

```sh
sh build.sh
java -jar build/capstone-fantasy.jar
```

The current build opens a fixed-size 960 × 540 (16:9) game window. **New Game** opens character selection: click three students, click a selected student to remove them, and use Back to return to the menu. Continue starts a battle against one Library Wisp. During battle choose Attack, Ability, Item, or Defend. Angel can attack normally or use Healing Kiss to heal a living student. At victory or defeat, return to the main menu. **Load Game** is not implemented; **Quit** closes the window. A graphical desktop is required.

## Direction

The game is intended to be suitable for a school audience, without profanity, innuendo, sexual content, or immoral themes. The battle presentation is inspired by [Sara's Turn Based Battle System](https://sarajs.itch.io/saras-turn-based-battle-system); the application is being built in Java Swing.

## AI assistance

OpenAI Codex helped plan the game and draft the Java Swing main menu, character-selection and first-battle screens, battle logic, and build script. OpenAI's built-in image generation tool created the student concept, idle artwork, and Library Wisp sprite, and adapted a Kenney sample into the selection background. This disclosure will be updated as work progresses. The student is responsible for understanding and explaining the submitted work.

## Art references and assets

- **Student party concept:** AI-generated design reference, not yet a production sprite sheet. Prompt: "Create an original cohesive character reference sheet for a wholesome school-themed 16-bit JRPG, four separate full-body pixel art student heroes aligned left to right on a simple neutral background with ample separation, no text. Zack: chubby otaku student with staff, mage; Bogart: first-year ROTC student with military buzz cut, modest uniform and sword; Charice: confident student hunter with two hand crossbows; Angel: muscular male bouncer monk healer, friendly expressive demeanor and modest clothing. All characters adults, fully clothed, SFW, friendly adventurous tone, consistent crisp pixel art aesthetic, distinct silhouettes, no copyrighted characters, no logos, no lettering. Concept sheet for later sprite production."
- **Student idle artwork:** `assets/sprites/student-party-idle-v1.png`, AI-generated transparent sheet containing four individual idle figures displayed separately by the selection screen. Prompt: "Create one game-ready pixel art sprite sheet for an original wholesome school-themed 16-bit JRPG. Four distinct full-body adult student heroes in a single horizontal row, each separated by generous empty transparent space, same height and same consistent scale, facing slightly to the right in idle standing poses. Zack chubby otaku mage with staff and glasses, Bogart ROTC first-year with buzz cut and sword, Charice confident hunter holding two hand crossbows, Angel muscular male monk healer, flamboyant friendly smile, modest clothing. Clear readable silhouettes, sprite game art, crisp hard-edged pixels, simple outlines and limited colors, no gradients, no text, no labels, no additional characters, SFW. True transparent background."
- **Campus courtyard:** `assets/backgrounds/campus-courtyard-v1.png`, AI-generated adaptation using Kenney's Tiny Dungeon sample as an input. Prompt: "Adapt this CC0 Kenney Tiny Dungeon sample into an original, clean, side-view 16-bit JRPG school campus stone courtyard backdrop for a character selection screen. Preserve its crisp pixel-art material language and muted blue stone colors, transform dungeon structures into school stone walls, a few windows, floor paving and a small glowing crystal. Wide landscape composition, subdued dark navy tones, central area calm and uncluttered for UI cards, no people, no writing, no logos, no watermark. Fully SFW."
- **Library Wisp:** `assets/sprites/library-wisp-v1.png`, generated with OpenAI's built-in image tool. Prompt: "An original wholesome fantasy JRPG enemy sprite: a small floating Library Wisp, a round glowing blue spirit with expressive eyes and a tiny floating book, clear silhouette, no scary details, school-appropriate, 16-bit pixel art, isolated full figure, transparent background, crisp pixel edges, no text, no shadows outside subject, no other characters."
- **Library Wisp revision:** `assets/sprites/library-wisp-v2.png`, edited from v1 with OpenAI's built-in image tool. Prompt: "Make it a formidable magical guardian for a school-friendly JRPG. Preserve the floating blue spectral flame identity, magical book and transparent background, but replace the cute smiling face with focused, determined glowing eyes, an imposing swept flame silhouette, and a restrained aura of magical energy. Clean 16-bit pixel art, legible at small size, isolated sprite, no text, no gore, no horror, no skulls, no symbols of evil, no profanity, fully SFW."
- **Tiny Dungeon:** Kenney, [source](https://kenney.nl/assets/tiny-dungeon), CC0 1.0. The downloaded pack and its license are in `assets/kenney-tiny-dungeon/`.
- **Battle layout reference:** [Sara's Turn Based Battle System](https://sarajs.itch.io/saras-turn-based-battle-system). No assets or code from that project are included.

## Known limitations

Only the first encounter is implemented. Camp, equipment, the remaining encounters, a full inventory, and saving are not implemented yet. The party starts the first battle with three First-Aid Kits. Stats and balance are provisional, the artwork has no animation frames yet, and the title screen's campus and crystal remain simple shapes drawn by the application.
