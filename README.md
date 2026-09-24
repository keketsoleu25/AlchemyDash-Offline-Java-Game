# Neon Tank Siege

A fully offline, original top-down tank defense game inspired by classic arcade tank games. Built with Java's standard AWT and Swing libraries. No assets or dependencies.

## Run on Windows

Install JDK 11 or newer. Download the repository ZIP and extract it. In the extracted folder, click File Explorer's address bar, type `cmd`, press Enter, and run:

```cmd
javac NeonTankSiege.java
java NeonTankSiege
```

Press **Enter** to begin. Move with **WASD** or **arrow keys** and hold **Space** to shoot. Player and enemy bullets cancel each other when they collide. Destroy the enemy tanks before they hit the gold base: **one hit on the base ends the game**. Brick walls break section by section; steel and water block tanks, while foliage hides them. Clear a wave to advance to a refreshed stage. Press **R** after game over to restart.

Enemy tanks drop rewards. Drive over the glowing icon to collect it:

- **Spade:** rebuild and fortify the base walls with steel for 15 seconds, then restore brick walls. It never places a wall on a tank.
- **Star:** upgrade your firing rate, up to three levels.
- **Helmet:** temporary protection from enemy bullets.
- **Clock:** temporarily freeze enemies.
- **Bomb:** clear the enemies currently on the field.

Run `java NeonTankSiege --self-test` to check movement, brick damage, the spade and base collision without opening a window.

The older experimental 3D game remains available: compile `Vector3D.java Mesh3D.java GameObject.java Display.java GameEngine.java`, then run `java GameEngine`.
