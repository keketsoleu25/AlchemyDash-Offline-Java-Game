import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JFrame;

public final class Display extends Canvas {
    private static final long serialVersionUID=1L;
    public static final int WIDTH=1024, HEIGHT=768;
    private static final double NEAR=2.0, FOCAL=WIDTH/2.0;
    private static final Color CYAN=new Color(42,244,255), MAGENTA=new Color(255,48,187);
    private static final Color LIME=new Color(177,255,67), DIM=new Color(35,67,88);
    private final JFrame frame;

    public Display() {
        setSize(WIDTH,HEIGHT);
        setPreferredSize(new java.awt.Dimension(WIDTH,HEIGHT));
        setIgnoreRepaint(true);
        setFocusable(true);
        frame=new JFrame("NEON RYDER 3D");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        createBufferStrategy(3);
        requestFocusInWindow();
    }

    private int sx(Vector3D p){return (int)Math.round(WIDTH*.5+FOCAL*p.x/p.z);}
    private int sy(Vector3D p){return (int)Math.round(HEIGHT*.5-FOCAL*p.y/p.z);}

    private void line(Graphics2D g,Vector3D worldA,Vector3D worldB,Vector3D cam) {
        Vector3D a=worldA.subtract(cam), b=worldB.subtract(cam);
        if(a.z<NEAR && b.z<NEAR)return;
        if(a.z<NEAR){double t=(NEAR-a.z)/(b.z-a.z);a=a.add(b.subtract(a).multiply(t));}
        else if(b.z<NEAR){double t=(NEAR-b.z)/(a.z-b.z);b=b.add(a.subtract(b).multiply(t));}
        // Large projected coordinates are harmless to Graphics2D but avoid integer overflow.
        if(Math.abs(a.x/a.z)>10000 || Math.abs(a.y/a.z)>10000 ||
           Math.abs(b.x/b.z)>10000 || Math.abs(b.y/b.z)>10000)return;
        g.drawLine(sx(a),sy(a),sx(b),sy(b));
    }

    private void track(Graphics2D g,Vector3D cam){
        g.setStroke(new BasicStroke(2.0f));
        for(int lane=-2;lane<=2;lane++){
            g.setColor(lane==0?MAGENTA:DIM);
            double xx=lane*4.5;
            line(g,new Vector3D(xx,-6,cam.z+NEAR),new Vector3D(xx,-6,cam.z+450),cam);
        }
        g.setColor(DIM);
        double first=Math.ceil((cam.z+NEAR)/18.0)*18.0;
        for(double z=first;z<cam.z+450;z+=18){
            line(g,new Vector3D(-11,-6,z),new Vector3D(11,-6,z),cam);
            line(g,new Vector3D(-11,-6,z),new Vector3D(-11,7,z),cam);
            line(g,new Vector3D(11,-6,z),new Vector3D(11,7,z),cam);
        }
    }

    private void scene(Graphics2D g,GameEngine engine){
        Vector3D cam=engine.camera();
        g.setColor(Color.BLACK);g.fillRect(0,0,WIDTH,HEIGHT);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform base=g.getTransform();
        g.rotate(engine.tilt(),WIDTH*.5,HEIGHT*.5);
        track(g,cam);
        List<GameObject> visible=new ArrayList<GameObject>();
        for(GameObject o:engine.objects())if(o.active && o.type!=GameObject.Type.PLAYER && o.position.z>cam.z-4)visible.add(o);
        visible.sort(Comparator.comparingDouble((GameObject o)->o.position.z).reversed());
        for(GameObject o:visible){
            double depth=o.position.z-cam.z;
            if(depth>460)continue;
            Color c=o.type==GameObject.Type.RING?LIME:o.shape==0?CYAN:MAGENTA;
            g.setColor(c);
            float thick=(float)Math.max(1.5,Math.min(5.0,170.0/Math.max(30,depth)));
            g.setStroke(new BasicStroke(thick,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            for(int[] edge:o.mesh.edges)line(g,o.mesh.worldVertex(edge[0]),o.mesh.worldVertex(edge[1]),cam);
        }
        g.setTransform(base);
        hud(g,engine);
    }

    private void hud(Graphics2D g,GameEngine e){
        g.setColor(new Color(0,0,0,190));g.fillRect(0,0,WIDTH,84);
        g.setFont(new Font("Monospaced",Font.BOLD,21));g.setColor(CYAN);
        g.drawString("NEON RYDER 3D",24,34);
        g.setFont(new Font("Monospaced",Font.BOLD,17));g.setColor(Color.WHITE);
        g.drawString("SPEED "+(int)e.speed(),24,66);
        g.drawString("SCORE "+e.score(),250,66);
        g.setColor(e.shields()>25?LIME:MAGENTA);
        g.drawString("SHIELDS "+e.shields()+"%",480,66);
        g.setColor(Color.LIGHT_GRAY);g.drawString("FPS "+e.fps()+"  TPS "+e.tps(),785,66);
        g.setColor(CYAN);g.setStroke(new BasicStroke(2));
        g.drawLine(WIDTH/2-13,HEIGHT/2,WIDTH/2-5,HEIGHT/2);
        g.drawLine(WIDTH/2+5,HEIGHT/2,WIDTH/2+13,HEIGHT/2);
        g.drawLine(WIDTH/2,HEIGHT/2-13,WIDTH/2,HEIGHT/2-5);
        g.drawLine(WIDTH/2,HEIGHT/2+5,WIDTH/2,HEIGHT/2+13);
        if(e.state()!=GameEngine.State.PLAYING){
            g.setColor(new Color(0,0,0,215));g.fillRect(0,0,WIDTH,HEIGHT);
            center(g,e.state()==GameEngine.State.GAME_OVER?"SYSTEM FAILURE":"NEON RYDER 3D",300,62,MAGENTA);
            center(g,e.state()==GameEngine.State.GAME_OVER?"SCORE  "+e.score()+"     PRESS R TO RESTART":"PRESS ENTER TO LAUNCH",371,27,CYAN);
            center(g,"WASD / ARROWS TO STEER   |   LIME RINGS = POINTS",430,21,Color.WHITE);
            center(g,"AVOID CYAN CUBES AND MAGENTA PYRAMIDS",466,21,Color.WHITE);
        }
    }

    private void center(Graphics2D g,String text,int y,int size,Color color){
        g.setFont(new Font("Monospaced",Font.BOLD,size));g.setColor(color);
        FontMetrics fm=g.getFontMetrics();g.drawString(text,(WIDTH-fm.stringWidth(text))/2,y);
    }

    public void render(GameEngine engine){
        BufferStrategy buffer=getBufferStrategy();
        if(buffer==null)return;
        do{
            do{
                Graphics2D g=(Graphics2D)buffer.getDrawGraphics();
                try{scene(g,engine);}finally{g.dispose();}
            }while(buffer.contentsRestored());
            buffer.show();
        }while(buffer.contentsLost());
        Toolkit.getDefaultToolkit().sync();
    }
}
