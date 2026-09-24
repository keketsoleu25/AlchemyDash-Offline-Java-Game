import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

/** Offline desktop arcade game. Run: java AlchemyDash.java */
public class AlchemyDash extends JPanel implements ActionListener, KeyListener, MouseMotionListener {
    static final int W = 720, H = 820;
    static final Color BG = new Color(12, 18, 28), LIME = new Color(183, 247, 104);
    final javax.swing.Timer timer = new javax.swing.Timer(16, this);
    final Random random = new Random();
    final List<Drop> drops = new ArrayList<>();
    final Preferences preferences = Preferences.userNodeForPackage(AlchemyDash.class);
    boolean left, right, mouseControl, playing, paused, gameOver;
    double playerX = W / 2.0, elapsed, spawnClock, flash;
    int score, lives = 3, highScore;

    static class Drop {
        double x, y, speed;
        boolean crystal;
        Drop(double x, double y, double speed, boolean crystal) {
            this.x = x; this.y = y; this.speed = speed; this.crystal = crystal;
        }
    }

    AlchemyDash() {
        setPreferredSize(new Dimension(W, H));
        setBackground(BG);
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(this);
        try { highScore = preferences.getInt("highScore", 0); } catch (Exception ignored) {}
        timer.start();
    }

    void start() {
        drops.clear(); playerX = W / 2.0; elapsed = spawnClock = flash = 0;
        score = 0; lives = 3; playing = true; paused = gameOver = false;
        requestFocusInWindow();
    }

    void update(double dt) {
        if (!playing || paused || gameOver) return;
        elapsed += dt;
        flash = Math.max(0, flash - dt);
        if (!mouseControl) {
            if (left) playerX -= 420 * dt;
            if (right) playerX += 420 * dt;
        }
        playerX = Math.max(35, Math.min(W - 35, playerX));
        spawnClock += dt;
        double interval = Math.max(.31, .76 - elapsed * .005);
        while (spawnClock >= interval) {
            spawnClock -= interval;
            drops.add(new Drop(35 + random.nextInt(W - 70), -30,
                    190 + Math.min(240, elapsed * 2.5) + random.nextInt(75), random.nextDouble() < .68));
        }
        for (Iterator<Drop> it = drops.iterator(); it.hasNext();) {
            Drop d = it.next();
            d.y += d.speed * dt;
            if (Math.abs(d.x - playerX) < 35 && Math.abs(d.y - 714) < 32) {
                if (d.crystal) {
                    score += 10;
                    if (score > highScore) {
                        highScore = score;
                        try { preferences.putInt("highScore", highScore); } catch (Exception ignored) {}
                    }
                } else if (flash <= 0) {
                    lives--; flash = 1.1;
                    if (lives <= 0) gameOver = true;
                }
                it.remove();
            } else if (d.y > H + 40) it.remove();
        }
    }

    @Override public void actionPerformed(ActionEvent e) { update(.016); repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(19, 31, 41), 0, H, BG));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(48, 70, 65, 80));
        for (int y = 90; y < H; y += 72) g.drawLine(0, y, W, y);
        for (int x = 55; x < W; x += 72) g.drawLine(x, 70, x, H);
        g.setColor(LIME); g.setFont(new Font("SansSerif", Font.BOLD, 23));
        g.drawString("ALCHEMY DASH", 34, 43);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("Score  " + score, 35, 91);
        g.drawString("Lives  " + lives, 275, 91);
        g.drawString("Best  " + highScore, 515, 91);
        g.setColor(new Color(183, 247, 104, 35));
        g.fillRoundRect(16, 670, W - 32, 105, 26, 26);
        for (Drop d : drops) {
            if (d.crystal) {
                g.setColor(new Color(183, 247, 104, 50));
                g.fillOval((int)d.x - 24, (int)d.y - 24, 48, 48);
                g.setColor(LIME);
                int[] xs = {(int)d.x, (int)d.x + 17, (int)d.x, (int)d.x - 17};
                int[] ys = {(int)d.y - 22, (int)d.y, (int)d.y + 22, (int)d.y};
                g.fillPolygon(xs, ys, 4);
            } else {
                g.setColor(new Color(232, 110, 98));
                g.fill(new Ellipse2D.Double(d.x - 19, d.y - 19, 38, 38));
                g.setColor(new Color(110, 45, 48));
                g.fillOval((int)d.x - 11, (int)d.y - 8, 9, 9);
                g.fillOval((int)d.x + 3, (int)d.y + 2, 7, 7);
            }
        }
        if (flash <= 0 || ((int)(flash * 10) % 2 == 0)) {
            g.setColor(new Color(89, 202, 230, 65));
            g.fillOval((int)playerX - 34, 680, 68, 68);
            g.setColor(new Color(89, 202, 230));
            int[] xs = {(int)playerX, (int)playerX - 23, (int)playerX + 23};
            int[] ys = {684, 734, 734};
            g.fillPolygon(xs, ys, 3);
            g.setColor(Color.WHITE); g.fillOval((int)playerX - 5, 707, 10, 10);
        }
        if (!playing || paused || gameOver) {
            g.setColor(new Color(6, 12, 18, 222)); g.fillRect(0, 0, W, H);
            String title = gameOver ? "RUN COMPLETE" : paused ? "PAUSED" : "ALCHEMY DASH";
            String detail = gameOver ? "Score: " + score + "   •   Best: " + highScore
                    : paused ? "Take a breath. The crystals can wait." : "Collect crystals. Dodge rocks.";
            center(g, title, 300, 42, LIME);
            center(g, detail, 359, 22, Color.WHITE);
            center(g, gameOver ? "Press ENTER to play again" : paused ? "Press P to resume" : "Press ENTER to start", 428, 25, LIME);
            center(g, "Move: ← → or A D   •   Mouse: move   •   P: pause", 482, 18, new Color(174, 185, 194));
            center(g, "Offline • no downloads • no account", 529, 17, new Color(135, 151, 159));
        }
        g.dispose();
    }

    void center(Graphics2D g, String text, int y, int size, Color color) {
        g.setColor(color); g.setFont(new Font("SansSerif", Font.BOLD, size));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (W - fm.stringWidth(text)) / 2, y);
    }

    @Override public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_ENTER && (!playing || gameOver)) start();
        if (k == KeyEvent.VK_P && playing && !gameOver) paused = !paused;
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) { left = true; mouseControl = false; }
        if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) { right = true; mouseControl = false; }
    }
    @Override public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) left = false;
        if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) right = false;
    }
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void mouseMoved(MouseEvent e) { if (playing && !paused) { playerX = e.getX(); mouseControl = true; } }
    @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--preview")) {
            System.setProperty("java.awt.headless", "true");
            AlchemyDash game = new AlchemyDash(); game.timer.stop(); game.start();
            game.score = 120; game.highScore = Math.max(game.highScore, 210);
            game.drops.add(new Drop(150, 240, 0, true));
            game.drops.add(new Drop(410, 310, 0, false));
            game.drops.add(new Drop(300, 480, 0, true));
            game.drops.add(new Drop(590, 560, 0, false));
            game.setSize(W, H);
            BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics(); game.paint(graphics); graphics.dispose();
            try { ImageIO.write(image, "png", new File("AlchemyDash-preview.png")); }
            catch (Exception ex) { throw new RuntimeException(ex); }
            System.out.println("Preview saved: AlchemyDash-preview.png"); return;
        }
        if (args.length > 0 && args[0].equals("--self-test")) {
            System.setProperty("java.awt.headless", "true");
            AlchemyDash game = new AlchemyDash(); game.timer.stop(); game.start();
            game.playerX = -100; game.update(.016);
            if (game.playerX < 35 || game.lives != 3 || game.score != 0) throw new AssertionError("Game state failed");
            System.out.println("Self-test passed"); return;
        }
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Alchemy Dash — Offline Java Game");
            AlchemyDash game = new AlchemyDash();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(game); frame.pack(); frame.setLocationRelativeTo(null);
            frame.setResizable(false); frame.setVisible(true); game.requestFocusInWindow();
        });
    }
}
