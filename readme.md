# Random Item Challenge (RIC)

Random Item Challenge is a Fabric Minecraft mini-game mod where every player receives a random item at a fixed interval. Use whatever you get to survive, fight, and outplay everyone else. When only one player (or one team) remains alive, they win.

The mod is fully configurable and supports free-for-all and team-based gameplay.

---

## How the Game Works

- Every **15 seconds** (default), **each player** receives a random item
- Items are selected from a configurable list
- The drop delay can be changed via config or commands
- The game continues until **all but one player is dead**
- The last surviving player wins

You can experiment with different delays, team modes, and settings to tune difficulty or chaos.

---

## Commands

### Start / Stop

```
/go
```
Starts the game

```
/halt
```
Stops the game immediately

---

### Settings

```
/ricsettings delay <seconds>
```
Sets the time between item drops

```
/ricsettings testmode <enabled|disabled>
```
Enables test mode for experimentation

```
/ricsettings resumemode <enabled|disabled>
```
Allows the game to resume after interruptions

```
/ricsettings teammode <enabled|disabled>
```
Enables team-based gameplay

```
/ricsettings numberperteam <number>
```
Sets the number of players per team

---

### Team Commands

```
/go teams
```
Starts the game in team mode

```
/go teams choose
```
Allows players to manually choose teams

---

## Configuration

- Item pools are configurable
- Drop delay can be configured and changed live with commands
- Works in both singleplayer and multiplayer
- Supports free-for-all and team modes

---

## Building the Mod (Gradle)

### Requirements

- Java 21 or newer
- Git
- Gradle (or the Gradle wrapper)

### Build Steps

Clone the repository:
```
git clone https://github.com/coolbou0427/random-item-challenge.git
cd random-item-challenge
```

Build the mod:
```
./gradlew build
```

On Windows:
```
gradlew build
```

The compiled mod jar will be located in:
```
build/libs/
```

Place the jar into your Minecraft `mods` folder. Fabric Loader is required.