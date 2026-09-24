import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class GameEngine implements Runnable, KeyListener {
    public enum State { START_MENU, PLAYING, GAME_OVER }
    private static final double STEP=1.0/60.0;
    private final Display display;
    private final Random random=new Random();
    private final List<GameObject> objects=new ArrayList<GameObject>();
    private volatile int inputMask;
    private volatile State state=State.START_MENU;
    private double camX,camY,camZ,tilt,speed,invulnerable;
    private int score,shields,fps,tps;
    private boolean running=true;

    private GameEngine(){
        display=new Display();
        display.addKeyListener(this);
        reset();
        state=State.START_MENU;
    }

    private void reset(){
        objects.clear();camX=camY=camZ=tilt=invulnerable=0;
        speed=42;score=0;shields=100;
        objects.add(new GameObject(GameObject.Type.PLAYER,new Vector3D(0,0,0),0));
        for(int i=0;i<36;i++){
            GameObject.Type type=i%3==0?GameObject.Type.RING:GameObject.Type.OBSTACLE;
            GameObject o=new GameObject(type,new Vector3D((random.nextInt(5)-2)*4.5,
                    (random.nextInt(3)-1)*3.4,45+i*12),random.nextInt(2));
            objects.add(o);
        }
    }

    private static int keyBit(int key){
        switch(key){
            case KeyEvent.VK_A:return 1;
            case KeyEvent.VK_LEFT:return 2;
            case KeyEvent.VK_D:return 4;
            case KeyEvent.VK_RIGHT:return 8;
            case KeyEvent.VK_W:return 16;
            case KeyEvent.VK_UP:return 32;
            case KeyEvent.VK_S:return 64;
            case KeyEvent.VK_DOWN:return 128;
            default:return 0;
        }
    }
    private boolean down(int mask){return (inputMask&mask)!=0;}
    private void update(){
        if(state!=State.PLAYING)return;
        double previousZ=camZ;
        speed=Math.min(112,speed+STEP*.75);
        camZ+=speed*STEP;
        double horizontal=(down(4|8)?1:0)-(down(1|2)?1:0);
        double vertical=(down(16|32)?1:0)-(down(64|128)?1:0);
        camX=Math.max(-9.8,Math.min(9.8,camX+horizontal*12*STEP));
        camY=Math.max(-4.8,Math.min(5.8,camY+vertical*10*STEP));
        tilt+=(horizontal*-.045-tilt)*Math.min(1,STEP*7);
        invulnerable=Math.max(0,invulnerable-STEP);
        Vector3D player=new Vector3D(camX,camY,camZ);
        GameObject ship=objects.get(0);ship.position=player;ship.mesh.position=player;
        for(int i=1;i<objects.size();i++){
            GameObject o=objects.get(i);
            o.update(STEP);
            if(o.position.z+o.halfDepth<previousZ-2){o.recycle(random,camZ);continue;}
            if(!o.active)continue;
            boolean crossed=o.position.z+o.halfDepth>=previousZ && o.position.z-o.halfDepth<=camZ+1.2;
            if(crossed){
                if(o.type==GameObject.Type.RING){
                    if(o.insideRing(player)){score+=100;o.active=false;}
                }else if(invulnerable<=0 && o.overlaps(player)){
                    shields=Math.max(0,shields-25);
                    invulnerable=1.0;
                    o.active=false;
                    if(shields==0)state=State.GAME_OVER;
                }
            }
            if(o.position.z<camZ-4)o.recycle(random,camZ);
        }
    }

    public void run(){
        long last=System.nanoTime(),stats=last;
        double accumulator=0;int frameCount=0,tickCount=0;
        while(running){
            long now=System.nanoTime();
            double elapsed=Math.min(.25,(now-last)/1_000_000_000.0);last=now;
            accumulator+=elapsed;
            int steps=0;
            while(accumulator>=STEP && steps<8){update();accumulator-=STEP;tickCount++;steps++;}
            // Drop excessive backlog after a pause, rather than spiralling indefinitely.
            if(steps==8)accumulator=0;
            display.render(this);frameCount++;
            if(now-stats>=1_000_000_000L){fps=frameCount;tps=tickCount;frameCount=tickCount=0;stats=now;}
            try{Thread.sleep(1);}catch(InterruptedException ex){Thread.currentThread().interrupt();running=false;}
        }
    }

    public Vector3D camera(){return new Vector3D(camX,camY,camZ);}
    public double tilt(){return tilt;}
    public List<GameObject> objects(){return Collections.unmodifiableList(objects);}
    public State state(){return state;}
    public double speed(){return speed;}
    public int score(){return score;}
    public int shields(){return shields;}
    public int fps(){return fps;}
    public int tps(){return tps;}

    public void keyPressed(KeyEvent e){
        int key=e.getKeyCode();inputMask|=keyBit(key);
        if(key==KeyEvent.VK_ENTER && state==State.START_MENU)state=State.PLAYING;
        if(key==KeyEvent.VK_R && state==State.GAME_OVER){reset();state=State.PLAYING;}
    }
    public void keyReleased(KeyEvent e){inputMask&=~keyBit(e.getKeyCode());}
    public void keyTyped(KeyEvent e){}

    public static void main(String[] args){
        GameEngine game=new GameEngine();
        Thread thread=new Thread(game,"neon-ryder-loop");
        thread.start();
    }
}
