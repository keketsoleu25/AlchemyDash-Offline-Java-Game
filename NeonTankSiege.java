import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.JFrame;

/** Offline, original top-down tank defense game. Java 11+. */
public final class NeonTankSiege extends Canvas implements Runnable, KeyListener {
    private static final long serialVersionUID=1L;
    private static final int TILE=32, GRID=20, FIELD=GRID*TILE, WIDTH=850, HEIGHT=640;
    private static final int EMPTY=0, BRICK=1, STEEL=2, WATER=3, FOLIAGE=4;
    private static final double STEP=1.0/60.0;
    private static final Color BG=new Color(12,19,30), CYAN=new Color(51,226,243),
        RED=new Color(255,94,112), GOLD=new Color(255,205,90), BRICK_COLOR=new Color(198,102,91);
    private final int[][] map=new int[GRID][GRID];
    private final int[][] brickMask=new int[GRID][GRID];
    private final List<Tank> enemies=new ArrayList<>();
    private final List<Bullet> bullets=new ArrayList<>();
    private final List<Particle> sparks=new ArrayList<>();
    private final List<Pickup> pickups=new ArrayList<>();
    private final Random random=new Random();
    private volatile int input;
    private volatile boolean startPressed, restartPressed;
    private Tank player;
    private int state=0, lives=3, wave=1, score, kills, remaining, playerLevel;
    private double spawnTimer, waveDelay, flash, playerInvulnerable;
    private double fortifyTimer, shieldTimer, freezeTimer;
    private double rewardMessageTimer;
    private String rewardMessage="";
    private boolean running=true;

    private static final class Tank {
        double x,y,speed,shot,decision;
        int dir,hp;
        final boolean enemy;
        Tank(double x,double y,boolean enemy,int hp){
            this.x=x;this.y=y;this.enemy=enemy;this.hp=hp;
            this.speed=enemy?66:145;this.dir=enemy?2:0;
        }
    }
    private static final class Bullet {
        double x,y,vx,vy;
        final boolean enemy;
        Bullet(double x,double y,int dir,boolean enemy){
            this.x=x;this.y=y;this.enemy=enemy;
            double s=enemy?250:365;
            vx=dir==1?s:dir==3?-s:0;
            vy=dir==2?s:dir==0?-s:0;
        }
    }
    private static final class Particle {
        double x,y,vx,vy,life;
        Color color;
        Particle(double x,double y,double vx,double vy,Color color){
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.color=color;life=.35;
        }
    }
    private static final class Pickup {
        double x,y,life=12;
        int kind;
        Pickup(double x,double y,int kind){this.x=x;this.y=y;this.kind=kind;}
    }

    private NeonTankSiege(){this(false);}

    private NeonTankSiege(boolean headless){
        setPreferredSize(new Dimension(WIDTH,HEIGHT));setIgnoreRepaint(true);
        setFocusable(true);addKeyListener(this);
        if(!headless){
            JFrame frame=new JFrame("NEON TANK SIEGE");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);frame.add(this);frame.pack();
            frame.setLocationRelativeTo(null);frame.setVisible(true);
            createBufferStrategy(3);requestFocusInWindow();
        }
        reset();state=0;
    }

    private void reset(){
        enemies.clear();bullets.clear();sparks.clear();pickups.clear();
        lives=3;wave=1;score=kills=playerLevel=0;flash=playerInvulnerable=0;
        fortifyTimer=shieldTimer=freezeTimer=0;
        rewardMessageTimer=0;rewardMessage="";
        player=new Tank(10*TILE+TILE*.5,17*TILE+TILE*.5,false,1);
        buildMap();beginWave();state=1;
    }

    private void buildMap(){
        for(int y=0;y<GRID;y++)for(int x=0;x<GRID;x++){map[y][x]=EMPTY;brickMask[y][x]=0;}
        // Mirrored cover leaves broad connected routes for both tanks.
        for(int y=3;y<=15;y+=3)for(int x=2;x<=7;x+=3){
            if((y==15&&x==5)||(y==3&&x==2))continue;
            for(int a=0;a<2;a++)for(int b=0;b<2;b++){
                map[y+a][x+b]=BRICK;
                map[y+a][GRID-1-x-b]=BRICK;
            }
        }
        for(int x=8;x<=11;x++)map[7][x]=BRICK;
        for(int x=8;x<=11;x++)map[12][x]=BRICK;
        for(int y=5;y<=6;y++){map[y][5]=STEEL;map[y][14]=STEEL;}
        for(int x=8;x<=11;x++){map[9][x]=WATER;map[10][x]=WATER;}
        for(int x:new int[]{3,4,15,16})map[10][x]=FOLIAGE;
        // Protect the base with breakable walls, leaving an opening above it.
        for(int x=8;x<=12;x++)map[18][x]=BRICK;
        map[18][10]=EMPTY;
        map[19][8]=BRICK;map[19][12]=BRICK;
        // Spawn and player lanes are always clear.
        for(int x:new int[]{2,10,17})for(int y=0;y<3;y++)map[y][x]=EMPTY;
        for(int y=16;y<19;y++)map[y][10]=EMPTY;
        for(int y=0;y<GRID;y++)for(int x=0;x<GRID;x++)
            if(map[y][x]==BRICK)brickMask[y][x]=15;
    }

    private void beginWave(){remaining=12+wave*2;spawnTimer=.3;waveDelay=0;}

    private boolean occupiedTile(int x,int y){
        double cx=x*TILE+TILE*.5,cy=y*TILE+TILE*.5;
        if(Math.abs(player.x-cx)<TILE*.5+12&&Math.abs(player.y-cy)<TILE*.5+12)return true;
        for(Tank enemy:enemies)
            if(Math.abs(enemy.x-cx)<TILE*.5+12&&Math.abs(enemy.y-cy)<TILE*.5+12)return true;
        return false;
    }

    private int fortifyBaseWalls(){
        int restored=0;
        for(int x=8;x<=12;x++){
            if(x==10)continue; // The centre entrance stays open.
            if(!occupiedTile(x,18)){
                if(map[18][x]!=STEEL)restored++;
                map[18][x]=STEEL;brickMask[18][x]=0;
            }
        }
        for(int x:new int[]{8,12})
            if(!occupiedTile(x,19)){
                if(map[19][x]!=STEEL)restored++;
                map[19][x]=STEEL;brickMask[19][x]=0;
            }
        return restored;
    }

    private void restoreBaseBricks(){
        for(int x=8;x<=12;x++){
            if(x==10)continue;
            if(!occupiedTile(x,18)){map[18][x]=BRICK;brickMask[18][x]=15;}
        }
        for(int x:new int[]{8,12})if(!occupiedTile(x,19)){
            map[19][x]=BRICK;brickMask[19][x]=15;
        }
    }

    private void collect(Pickup p){
        score+=50;
        switch(p.kind){
            case 0:fortifyTimer=15;fortifyBaseWalls();rewardMessage="SPADE: STEEL BASE 15s";break;
            case 1:playerLevel=Math.min(3,playerLevel+1);rewardMessage="STAR: FIREPOWER UP";break;
            case 2:shieldTimer=12;rewardMessage="HELMET: SHIELD 12s";break;
            case 3:freezeTimer=8;rewardMessage="CLOCK: ENEMIES FROZEN";break;
            default:
                for(Tank enemy:enemies){score+=100;burst(enemy.x,enemy.y,GOLD,16);}
                enemies.clear();rewardMessage="BOMB: ENEMIES CLEARED";
        }
        rewardMessageTimer=2.5;
        burst(player.x,player.y,GOLD,22);
    }

    private boolean blocked(double x,double y,double radius){
        if(x-radius<0||y-radius<0||x+radius>=FIELD||y+radius>=FIELD)return true;
        int minX=(int)((x-radius)/TILE),maxX=(int)((x+radius)/TILE);
        int minY=(int)((y-radius)/TILE),maxY=(int)((y+radius)/TILE);
        for(int yy=minY;yy<=maxY;yy++)for(int xx=minX;xx<=maxX;xx++){
            if(map[yy][xx]==STEEL||map[yy][xx]==WATER)return true;
            if(map[yy][xx]==BRICK)for(int q=0;q<4;q++){
                if((brickMask[yy][xx]&(1<<q))==0)continue;
                int bx=xx*TILE+(q%2)*(TILE/2),by=yy*TILE+(q/2)*(TILE/2);
                if(x+radius>bx&&x-radius<bx+TILE/2&&
                   y+radius>by&&y-radius<by+TILE/2)return true;
            }
        }
        return false;
    }

    private void move(Tank t,double dx,double dy){
        if(!blocked(t.x+dx,t.y,12)&&!tankAt(t.x+dx,t.y,t))t.x+=dx;
        if(!blocked(t.x,t.y+dy,12)&&!tankAt(t.x,t.y+dy,t))t.y+=dy;
    }

    private boolean tankAt(double x,double y,Tank self){
        if(self!=player&&Math.abs(x-player.x)<25&&Math.abs(y-player.y)<25)return true;
        for(Tank e:enemies)if(e!=self&&Math.abs(x-e.x)<25&&Math.abs(y-e.y)<25)return true;
        return false;
    }

    private void shoot(Tank t){
        if(t.shot>0)return;
        t.shot=t.enemy?1.1:playerLevel>=2?.14:playerLevel>=1?.20:.27;
        double sx=t.x+(t.dir==1?19:t.dir==3?-19:0);
        double sy=t.y+(t.dir==2?19:t.dir==0?-19:0);
        bullets.add(new Bullet(sx,sy,t.dir,t.enemy));
    }

    private void burst(double x,double y,Color color,int n){
        for(int i=0;i<n;i++){
            double a=random.nextDouble()*Math.PI*2,s=45+random.nextDouble()*160;
            sparks.add(new Particle(x,y,Math.cos(a)*s,Math.sin(a)*s,color));
        }
    }

    private void spawnEnemy(){
        int[] columns={2,10,17};
        for(int attempt=0;attempt<3;attempt++){
            int column=columns[(kills+remaining+attempt)%3];
            double px=column*TILE+TILE*.5,py=TILE*.5;
            if(!tankAt(px,py,null)){
                enemies.add(new Tank(px,py,true,wave%3==0?2:1));
                remaining--;return;
            }
        }
    }

    private void update(double dt){
        if(state!=1)return;
        flash=Math.max(0,flash-dt);
        playerInvulnerable=Math.max(0,playerInvulnerable-dt);
        shieldTimer=Math.max(0,shieldTimer-dt);
        freezeTimer=Math.max(0,freezeTimer-dt);
        if(fortifyTimer>0){
            fortifyTimer=Math.max(0,fortifyTimer-dt);
            if(fortifyTimer==0)restoreBaseBricks();
        }
        rewardMessageTimer=Math.max(0,rewardMessageTimer-dt);
        player.shot=Math.max(0,player.shot-dt);
        int h=(input&8)!=0?1:(input&4)!=0?-1:0;
        int v=(input&1)!=0?-1:(input&2)!=0?1:0;
        if(h!=0||v!=0){
            if(v!=0){player.dir=v>0?2:0;move(player,0,v*player.speed*dt);}
            else{player.dir=h>0?1:3;move(player,h*player.speed*dt,0);}
        }
        if((input&16)!=0)shoot(player);
        if(freezeTimer<=0){
            spawnTimer-=dt;
            if(remaining>0&&enemies.size()<5&&spawnTimer<=0){spawnEnemy();spawnTimer=1.4;}
        }
        for(Tank e:enemies){
            if(freezeTimer>0)continue;
            e.shot=Math.max(0,e.shot-dt);e.decision-=dt;
            if(e.decision<=0){
                e.decision=.4+random.nextDouble()*1.2;
                if(random.nextDouble()<.45){
                    double dx=player.x-e.x,dy=player.y-e.y;
                    e.dir=Math.abs(dx)>Math.abs(dy)?(dx>0?1:3):(dy>0?2:0);
                }else e.dir=random.nextInt(4);
            }
            double step=e.speed*dt;
            double ox=e.x,oy=e.y;
            move(e,e.dir==1?step:e.dir==3?-step:0,e.dir==2?step:e.dir==0?-step:0);
            if(ox==e.x&&oy==e.y)e.decision=0;
            if(random.nextDouble()<dt*.85)shoot(e);
        }
        for(Iterator<Bullet> it=bullets.iterator();it.hasNext();){
            Bullet b=it.next();b.x+=b.vx*dt;b.y+=b.vy*dt;
            if(b.x<0||b.y<0||b.x>=FIELD||b.y>=FIELD){it.remove();continue;}
            int col=(int)(b.x/TILE),row=(int)(b.y/TILE);
            int qx=(int)(b.x%TILE)/(TILE/2),qy=(int)(b.y%TILE)/(TILE/2);
            int quadrant=1<<(qy*2+qx);
            if(map[row][col]==STEEL||
                (map[row][col]==BRICK&&(brickMask[row][col]&quadrant)!=0)){
                if(map[row][col]==BRICK){
                    brickMask[row][col]&=~quadrant;
                    if(brickMask[row][col]==0)map[row][col]=EMPTY;
                }
                burst(b.x,b.y,GOLD,5);it.remove();continue;
            }
            if(b.enemy){
                if(Math.abs(b.x-player.x)<14&&Math.abs(b.y-player.y)<14){
                    it.remove();
                    if(playerInvulnerable<=0&&shieldTimer<=0){
                        burst(player.x,player.y,CYAN,18);lives--;flash=.5;
                        playerInvulnerable=2.0;playerLevel=0;
                        player.x=10*TILE+TILE*.5;player.y=17*TILE+TILE*.5;
                        if(lives<=0)state=2;
                    }
                    continue;
                }
            }else{
                boolean hit=false;
                for(Iterator<Tank> ei=enemies.iterator();ei.hasNext();){
                    Tank e=ei.next();
                    if(Math.abs(b.x-e.x)<15&&Math.abs(b.y-e.y)<15){
                        e.hp--;burst(e.x,e.y,RED,8);
                        if(e.hp<=0){
                            ei.remove();kills++;score+=100;burst(e.x,e.y,GOLD,14);
                            if(kills%3==0||random.nextDouble()<.10){
                                int kind=kills%3==0?((kills/3)-1)%5:random.nextInt(5);
                                pickups.add(new Pickup(e.x,e.y,kind));
                            }
                        }
                        hit=true;break;
                    }
                }
                if(hit){it.remove();continue;}
            }
            if(b.y>18*TILE&&Math.abs(b.x-10*TILE-TILE*.5)<17){
                if(b.enemy){burst(10*TILE+TILE*.5,19*TILE,GOLD,22);state=2;}
                it.remove();
            }
        }
        for(Iterator<Particle> it=sparks.iterator();it.hasNext();){
            Particle p=it.next();p.x+=p.vx*dt;p.y+=p.vy*dt;p.life-=dt;
            if(p.life<=0)it.remove();
        }
        for(Iterator<Pickup> it=pickups.iterator();it.hasNext();){
            Pickup p=it.next();p.life-=dt;
            if(p.life<=0){it.remove();continue;}
            if(Math.abs(p.x-player.x)<23&&Math.abs(p.y-player.y)<23){
                it.remove();collect(p);
            }
        }
        if(remaining==0&&enemies.isEmpty()){
            waveDelay+=dt;
            if(waveDelay>2.2){
                wave++;score+=250;
                bullets.clear();pickups.clear();
                player.x=10*TILE+TILE*.5;player.y=17*TILE+TILE*.5;
                player.dir=0;playerInvulnerable=2;
                fortifyTimer=freezeTimer=shieldTimer=0;
                buildMap();beginWave();
            }
        }
    }

    private void drawTank(Graphics2D g,Tank t){
        AffineTransform old=g.getTransform();g.translate(t.x,t.y);g.rotate(t.dir*Math.PI/2);
        g.setColor(t.enemy?RED:CYAN);g.fillRoundRect(-12,-13,24,26,6,6);
        g.setColor(BG);g.fillRect(-10,-9,5,20);g.fillRect(5,-9,5,20);
        g.setColor(t.enemy?GOLD:Color.WHITE);g.fillRoundRect(-4,-20,8,21,3,3);
        g.fillOval(-6,-6,12,12);g.setTransform(old);
    }

    private void center(Graphics2D g,String text,int y,int size,Color color){
        g.setFont(new Font("Monospaced",Font.BOLD,size));g.setColor(color);
        g.drawString(text,(FIELD-g.getFontMetrics().stringWidth(text))/2,y);
    }

    private void draw(Graphics2D g){
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BG);g.fillRect(0,0,WIDTH,HEIGHT);
        g.setColor(new Color(27,41,55));
        for(int v=0;v<FIELD;v+=TILE){g.drawLine(v,0,v,FIELD);g.drawLine(0,v,FIELD,v);}
        for(int y=0;y<GRID;y++)for(int x=0;x<GRID;x++){
            int tile=map[y][x];if(tile==EMPTY)continue;
            int px=x*TILE,py=y*TILE;
            if(tile==BRICK){
                for(int q=0;q<4;q++)if((brickMask[y][x]&(1<<q))!=0){
                    int bx=px+(q%2)*16,by=py+(q/2)*16;
                    g.setColor(BRICK_COLOR);g.fillRect(bx+1,by+1,14,14);
                    g.setColor(new Color(252,157,122));g.drawLine(bx+2,by+4,bx+13,by+4);
                }
            }else if(tile==STEEL){
                g.setColor(new Color(84,130,158));g.fillRect(px+2,py+2,TILE-4,TILE-4);
                g.setColor(CYAN);g.drawRect(px+6,py+6,20,20);
            }else if(tile==WATER){
                g.setColor(new Color(20,74,132));g.fillRect(px,py,TILE,TILE);
                g.setColor(CYAN);g.drawArc(px+4,py+8,23,14,0,180);
            }
        }
        g.setColor(state==2?RED:GOLD);
        g.fillRoundRect(10*TILE-3,19*TILE+3,38,27,5,5);
        g.setColor(BG);g.fillOval(10*TILE+9,19*TILE+9,13,13);
        for(Bullet b:bullets){g.setColor(b.enemy?RED:GOLD);g.fillOval((int)b.x-4,(int)b.y-4,8,8);}
        for(Pickup p:pickups){
            double pulse=1+Math.sin(p.life*8)*.12;
            int radius=(int)(16*pulse);
            g.setColor(new Color(255,205,90,65));
            g.fillOval((int)p.x-radius-5,(int)p.y-radius-5,(radius+5)*2,(radius+5)*2);
            Color rewardColor=p.kind==0?GOLD:p.kind==1?CYAN:p.kind==2?Color.GREEN:p.kind==3?Color.WHITE:RED;
            g.setColor(rewardColor);g.fillOval((int)p.x-radius,(int)p.y-radius,radius*2,radius*2);
            g.setColor(BG);g.setStroke(new BasicStroke(3));
            if(p.kind==0){
                g.drawLine((int)p.x,(int)p.y-9,(int)p.x,(int)p.y+4);
                g.fillRoundRect((int)p.x-8,(int)p.y+2,16,8,3,3);
            }else{
                g.setFont(new Font("Monospaced",Font.BOLD,19));
                g.drawString(new String[]{"","S","H","C","!"}[p.kind],(int)p.x-7,(int)p.y+7);
            }
        }
        for(Tank e:enemies)drawTank(g,e);
        if(flash<=0||((int)(flash*16)&1)==0)drawTank(g,player);
        if(shieldTimer>0||playerInvulnerable>0){
            g.setColor(CYAN);g.setStroke(new BasicStroke(2));
            g.drawOval((int)player.x-19,(int)player.y-19,38,38);
        }
        for(int y=0;y<GRID;y++)for(int x=0;x<GRID;x++)if(map[y][x]==FOLIAGE){
            g.setColor(new Color(21,115,62,210));
            g.fillRect(x*TILE,y*TILE,TILE,TILE);
            g.setColor(new Color(66,198,99));
            g.drawLine(x*TILE+3,y*TILE+6,x*TILE+28,y*TILE+25);
        }
        for(Particle p:sparks){g.setColor(p.color);g.fillRect((int)p.x,(int)p.y,3,3);}
        g.setColor(new Color(5,11,19));g.fillRect(FIELD,0,WIDTH-FIELD,HEIGHT);
        g.setColor(CYAN);g.setFont(new Font("Monospaced",Font.BOLD,25));g.drawString("NEON TANK",FIELD+18,55);
        g.drawString("SIEGE",FIELD+18,87);
        g.setColor(GOLD);g.setFont(new Font("Monospaced",Font.BOLD,18));
        g.drawString("SCORE  "+score,FIELD+18,155);
        g.drawString("WAVE   "+wave,FIELD+18,194);
        g.drawString("LIVES  "+lives,FIELD+18,233);
        g.drawString("BASE   "+(state==2?"LOST":"SAFE"),FIELD+18,272);
        g.drawString("ENEMIES "+(remaining+enemies.size()),FIELD+18,311);
        g.setColor(GOLD);g.drawString("POWER LV "+playerLevel,FIELD+18,354);
        if(fortifyTimer>0)g.drawString("STEEL "+(int)Math.ceil(fortifyTimer)+"s",FIELD+18,382);
        if(rewardMessageTimer>0){
            g.setColor(GOLD);g.setFont(new Font("Monospaced",Font.BOLD,16));
            g.drawString(rewardMessage,18,34);
        }
        g.setColor(Color.LIGHT_GRAY);g.setFont(new Font("Monospaced",Font.PLAIN,15));
        g.drawString("WASD / ARROWS",FIELD+18,430);
        g.drawString("MOVE TANK",FIELD+18,452);
        g.drawString("SPACE  FIRE",FIELD+18,492);
        g.drawString("DEFEND THE BASE",FIELD+18,555);
        if(state!=1){
            g.setColor(new Color(0,0,0,225));g.fillRect(0,0,FIELD,FIELD);
            center(g,state==2?"BASE LOST":"NEON TANK SIEGE",255,38,state==2?RED:CYAN);
            center(g,state==2?"SCORE "+score+"  |  WAVE "+wave:"DEFEND THE BASE. CLEAR THE WAVES.",310,18,Color.WHITE);
            center(g,state==2?"PRESS R TO RESTART":"PRESS ENTER TO START",365,23,GOLD);
            center(g,"WASD / ARROWS MOVE   -   SPACE FIRES",411,16,Color.LIGHT_GRAY);
        }
    }

    private void render(){
        BufferStrategy buffer=getBufferStrategy();
        do{do{
            Graphics2D g=(Graphics2D)buffer.getDrawGraphics();
            try{draw(g);}finally{g.dispose();}
        }while(buffer.contentsRestored());buffer.show();}while(buffer.contentsLost());
        Toolkit.getDefaultToolkit().sync();
    }

    public void run(){
        long previous=System.nanoTime();double accumulator=0;
        while(running){
            long now=System.nanoTime();
            accumulator+=Math.min(.25,(now-previous)/1_000_000_000.0);previous=now;
            if(startPressed){startPressed=false;if(state==0)state=1;}
            if(restartPressed){restartPressed=false;if(state==2)reset();}
            int steps=0;
            while(accumulator>=STEP&&steps<8){update(STEP);accumulator-=STEP;steps++;}
            if(steps==8)accumulator=0;
            render();
            try{Thread.sleep(1);}catch(InterruptedException ex){Thread.currentThread().interrupt();running=false;}
        }
    }

    private static int bit(int key){
        switch(key){
            case KeyEvent.VK_W:case KeyEvent.VK_UP:return 1;
            case KeyEvent.VK_S:case KeyEvent.VK_DOWN:return 2;
            case KeyEvent.VK_A:case KeyEvent.VK_LEFT:return 4;
            case KeyEvent.VK_D:case KeyEvent.VK_RIGHT:return 8;
            case KeyEvent.VK_SPACE:return 16;
            default:return 0;
        }
    }
    public void keyPressed(KeyEvent e){
        input|=bit(e.getKeyCode());
        if(e.getKeyCode()==KeyEvent.VK_ENTER)startPressed=true;
        if(e.getKeyCode()==KeyEvent.VK_R)restartPressed=true;
    }
    public void keyReleased(KeyEvent e){input&=~bit(e.getKeyCode());}
    public void keyTyped(KeyEvent e){}

    public static void main(String[] args){
        if(args.length>0&&"--self-test".equals(args[0])){
            NeonTankSiege game=new NeonTankSiege(true);
            double startX=game.player.x,startY=game.player.y;
            if(game.blocked(startX,startY,12))throw new AssertionError("Spawn overlaps a wall");
            game.move(game.player,-8,0);
            if(game.player.x>=startX)throw new AssertionError("Player cannot move left");
            game.move(game.player,8,0);
            game.move(game.player,0,-8);
            if(game.player.y>=startY)throw new AssertionError("Player cannot move up");
            game.map[18][9]=EMPTY;
            if(game.fortifyBaseWalls()<1||game.map[18][9]!=STEEL)
                throw new AssertionError("Spade did not fortify the wall");
            game.restoreBaseBricks();
            if(game.map[18][9]!=BRICK)
                throw new AssertionError("Steel did not revert to brick");
            game.map[18][9]=EMPTY;
            game.player.x=9*TILE+TILE*.5;game.player.y=18*TILE+TILE*.5;
            game.fortifyBaseWalls();
            if(game.map[18][9]!=EMPTY)throw new AssertionError("Spade trapped the player");
            game.reset();
            game.bullets.add(new Bullet(5*TILE+7,3*TILE+8,1,false));
            game.update(STEP);
            if(game.map[3][5]!=BRICK||game.brickMask[3][5]==15)
                throw new AssertionError("Brick section was not destroyed");
            game.reset();
            game.bullets.add(new Bullet(10*TILE+TILE*.5,18*TILE,2,true));
            game.update(STEP);
            if(game.state!=2)throw new AssertionError("Base did not fall on one hit");
            System.out.println("Movement, walls, rewards and base self-test passed");
            return;
        }
        NeonTankSiege game=new NeonTankSiege();
        new Thread(game,"tank-game-loop").start();
    }
}
