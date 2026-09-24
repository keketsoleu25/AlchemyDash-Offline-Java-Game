# Neon Tank Siege

A fully offline, original top-down tank defense game inspired by classic arcade tank games. Built with Java's standard AWT and Swing libraries. No assets or dependencies.

## Run on Windows

Install JDK 11 or newer. Download the repository ZIP and extract it. In the extracted folder, click File Explorer's address bar, type `cmd`, press Enter, and run:

```cmd
javac NeonTankSiege.java
java NeonTankSiege
```

Press **Enter** to begin. Move with **WASD** or **arrow keys** and hold **Space** to shoot. Destroy enemy tanks, break brick walls, and defend the gold base. Steel walls cannot be destroyed. Enemies drop glowing spade rewards; drive over one to rebuild destroyed bricks around the base. The centre entrance stays open, and the spade will not place a wall on a tank. Clear a wave for bonus points and a base repair. Press **R** after game over to restart.

The older experimental 3D game remains available: compile `Vector3D.java Mesh3D.java GameObject.java Display.java GameEngine.java`, then run `java GameEngine`.
