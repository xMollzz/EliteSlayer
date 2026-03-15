# EliteSlayer

Modular OSRS Slayer bot with behaviour-tree AI for [DreamBot 3](https://dreambot.org/).

## Features

- **Behaviour-tree AI** — 12 prioritised nodes (safety, breaks, eating, potions, prayer, special attacks, muling, banking, GE trading, cannon, looting, combat)
- **Anti-ban engine** — 17 weighted human-like actions with per-action cooldowns
- **Entropy monitor** — Shannon entropy detection to avoid repetitive patterns
- **Crowd tracker** — pauses in crowded areas (rolling 3-minute window)
- **Break scheduler** — Poisson-process breaks with session-fatigue scaling
- **Stuck detector** — 3-tier watchdog (nudge → teleport → stop)
- **Crash-resume** — persists kills, GP, and tasks to disk
- **Discord webhooks** — async session start/stop notifications
- **In-game HUD** — real-time overlay with HP, entropy, kills/hr, gp/hr, and more
- **16 supported monsters** — Abyssal Demons, Gargoyles, Hydras, and more

## Prerequisites

- **Java 11+** (JDK)
- **DreamBot 3** client installed — download from [dreambot.org](https://dreambot.org/)

## Setup & Build

1. **Clone the repository**
   ```bash
   git clone https://github.com/xMollzz/EliteSlayer.git
   cd EliteSlayer
   ```

2. **Copy the DreamBot client JAR** into the `libs/` folder  
   Create the folder if it doesn't exist, then copy `client.jar` from your DreamBot installation:
   ```bash
   mkdir -p libs

   # Windows (Git Bash / PowerShell)
   cp "$USERPROFILE/DreamBot/BotData/client.jar" libs/

   # macOS / Linux
   cp ~/DreamBot/BotData/client.jar libs/
   ```

3. **Build the script JAR**
   ```bash
   ./gradlew jar          # macOS / Linux
   gradlew.bat jar        # Windows
   ```
   The output JAR will be at `build/libs/EliteSlayer-1.0.jar`.

4. **Install into DreamBot**  
   Copy the built JAR into your DreamBot scripts directory:
   ```bash
   # macOS / Linux
   cp build/libs/EliteSlayer-1.0.jar ~/DreamBot/Scripts/

   # Windows (Git Bash / PowerShell)
   cp build/libs/EliteSlayer-1.0.jar "$USERPROFILE/DreamBot/Scripts/"
   ```

5. **Launch DreamBot**, open the Script Manager, and start **EliteSlayer**.

## Usage

1. The configuration GUI opens on start — select your target monster, set combat thresholds, and optionally configure Discord webhooks, GE restocking, cannon, and muling.
2. Click **Start** to begin the slayer session.
3. The in-game HUD displays live stats (kills/hr, gp/hr, entropy, crowd count, etc.).

## Project Structure

```
src/eliteslayer/
├── EliteSlayer.java          # Main script entry point
├── behavior/                 # Behaviour-tree framework (Node, Selector, Sequence, Status)
├── game/                     # Monster definitions & database
├── nodes/                    # 12 behaviour-tree leaf nodes
├── systems/                  # Anti-ban, breaks, crowd, entropy, stuck detection
├── ui/                       # Config GUI & in-game HUD
└── util/                     # Discord, file state, navigator, price cache, telemetry
```
