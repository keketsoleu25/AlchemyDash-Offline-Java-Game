# NEON RYDER 3D

A standalone, offline, first-person cyberpunk highway dodger written in raw Java.
The renderer uses `Canvas` triple buffering, hand-written perspective projection,
near-plane clipping and depth sorting. No engine, external library, assets or network access.

## Run

Install Java 11 or newer. Download the repository ZIP, extract it, open a terminal
inside the folder containing the five `.java` files, and run:

```cmd
javac Vector3D.java Mesh3D.java GameObject.java Display.java GameEngine.java
java GameEngine
```

Press **Enter** to launch. Steer with **WASD** or **arrow keys**. Fly through
lime rings for points. Avoid cyan cubes and magenta pyramids. Each hit costs
25% shields; **R** restarts after game over.

The fixed-step simulation targets 60 updates per second. FPS/TPS appear in the HUD.
