import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

/** Offline survival shooter. Run with: java AlchemyDashV2.java */
public class AlchemyDashV2 extends JPanel implements ActionListener, KeyListener, MouseListener {
    static final int W=800,H=700;
    static final Color BG=new Color(9,14,30), CYAN=new Color(54,219,250), GOLD=new Color(255,207,84), RED=new Color(255,76,108);
    final javax.swing.Timer timer=new javax.swing.Timer(16,this);
    final Random rng=new Random();
    final List<Thing> enemies=new ArrayList<>(), bullets=new ArrayList<>(), pickups=new ArrayList<>(), particles=new ArrayList<>();
    final Preferences prefs=Preferences.userNodeForPackage(AlchemyDashV2.class);
    boolean left,right,up,down,fire,playing,paused,dead;
    double x=W/2.0,y=H-95,t,spawn,shot,invuln,shield,rapid,shake;
    int score,best,lives=3,combo,kills,wave=1;
    static class Thing {
        double x,y,vx,vy,r,hp,age; int type;
        Thing(double x,double y,double vx,double vy,double r,int type,double hp){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.r=r;this.type=type;this.hp=hp;}
    }
    AlchemyDashV2(){setPreferredSize(new Dimension(W,H));setFocusable(true);addKeyListener(this);addMouseListener(this);best=prefs.getInt("bestV2",0);timer.start();}
    void start(){enemies.clear();bullets.clear();pickups.clear();particles.clear();x=W/2.0;y=H-95;t=spawn=shot=invuln=shield=rapid=shake=0;score=kills=combo=0;lives=3;wave=1;playing=true;paused=dead=false;requestFocusInWindow();}
    void burst(double px,double py,Color color,int count){for(int i=0;i<count;i++){double a=rng.nextDouble()*Math.PI*2,s=30+rng.nextDouble()*230;Thing p=new Thing(px,py,Math.cos(a)*s,Math.sin(a)*s,2+rng.nextDouble()*3,0,1);p.age=color.getRGB();particles.add(p);}}
    void score(int n){score+=n;if(score>best){best=score;prefs.putInt("bestV2",best);}}
    void spawnEnemy(){int kind=rng.nextInt(10)<2?1:0;double ex=35+rng.nextInt(W-70);enemies.add(new Thing(ex,-40,kind==1?(rng.nextBoolean()?115:-115):0,105+wave*18+rng.nextInt(70),kind==1?21:17,kind,kind==1?3:1));}
    void tick(double dt){if(!playing||paused||dead)return;t+=dt;wave=1+(int)(t/20);invuln=Math.max(0,invuln-dt);shield=Math.max(0,shield-dt);rapid=Math.max(0,rapid-dt);shake=Math.max(0,shake-dt);shot-=dt;spawn+=dt;
        double speed=350*dt;if(left)x-=speed;if(right)x+=speed;if(up)y-=speed;if(down)y+=speed;x=Math.max(25,Math.min(W-25,x));y=Math.max(135,Math.min(H-35,y));
        if(fire&&shot<=0){shot=rapid>0?.10:.22;bullets.add(new Thing(x,y-23,0,-660,5,0,1));if(rapid>0){bullets.add(new Thing(x-14,y-15,-95,-620,4,0,1));bullets.add(new Thing(x+14,y-15,95,-620,4,0,1));}}
        while(spawn>Math.max(.35,1.10-wave*.075)){spawn-=Math.max(.35,1.10-wave*.075);spawnEnemy();}
        for(Iterator<Thing> it=bullets.iterator();it.hasNext();){Thing b=it.next();b.x+=b.vx*dt;b.y+=b.vy*dt;if(b.y<0||b.x<0||b.x>W){it.remove();continue;}for(Iterator<Thing> ei=enemies.iterator();ei.hasNext();){Thing e=ei.next();if(Math.hypot(b.x-e.x,b.y-e.y)<b.r+e.r){it.remove();e.hp--;burst(b.x,b.y,CYAN,3);if(e.hp<=0){ei.remove();kills++;combo++;score(10+Math.min(combo,20)*2);burst(e.x,e.y,e.type==1?GOLD:RED,16);if(rng.nextDouble()<.13)pickups.add(new Thing(e.x,e.y,0,115,13,rng.nextInt(3),1));}break;}}}
        for(Iterator<Thing> it=enemies.iterator();it.hasNext();){Thing e=it.next();e.x+=e.vx*dt;e.y+=e.vy*dt;if(e.type==1&&(e.x<25||e.x>W-25))e.vx=-e.vx;if(e.y>H+40){it.remove();combo=0;continue;}if(Math.hypot(x-e.x,y-e.y)<e.r+17){it.remove();burst(e.x,e.y,RED,20);shake=.25;combo=0;if(shield>0){shield=0;}else if(invuln<=0){lives--;invuln=1.4;if(lives<=0)dead=true;}}}
        for(Iterator<Thing> it=pickups.iterator();it.hasNext();){Thing p=it.next();p.y+=p.vy*dt;if(p.y>H+20){it.remove();continue;}if(Math.hypot(x-p.x,y-p.y)<30){it.remove();if(p.type==0)shield=12;else if(p.type==1)rapid=10;else lives=Math.min(5,lives+1);burst(p.x,p.y,GOLD,20);score(25);}}
        for(Iterator<Thing> it=particles.iterator();it.hasNext();){Thing p=it.next();p.x+=p.vx*dt;p.y+=p.vy*dt;p.vx*=.96;p.vy*=.96;p.hp-=dt*1.7;if(p.hp<=0)it.remove();}
    }
    public void actionPerformed(ActionEvent e){tick(.016);repaint();}
    void center(Graphics2D g,String s,int yy,int size,Color c){g.setFont(new Font("SansSerif",Font.BOLD,size));g.setColor(c);g.drawString(s,(W-g.getFontMetrics().stringWidth(s))/2,yy);}
    protected void paintComponent(Graphics gg){super.paintComponent(gg);Graphics2D g=(Graphics2D)gg.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setPaint(new GradientPaint(0,0,new Color(19,29,57),0,H,BG));g.fillRect(0,0,W,H);if(shake>0)g.translate(rng.nextInt(9)-4,rng.nextInt(9)-4);
        g.setColor(new Color(68,105,150,40));for(int i=0;i<W;i+=50)g.drawLine(i,110,i,H);for(int j=110;j<H;j+=50)g.drawLine(0,j,W,j);
        g.setColor(CYAN);g.setFont(new Font("SansSerif",Font.BOLD,25));g.drawString("ALCHEMY DASH  //  OVERDRIVE",20,39);g.setFont(new Font("Monospaced",Font.BOLD,18));g.setColor(Color.WHITE);g.drawString("SCORE "+score,20,76);g.drawString("BEST "+best,240,76);g.drawString("LIVES "+lives,435,76);g.drawString("WAVE "+wave,630,76);
        if(combo>=3){g.setColor(GOLD);g.drawString("COMBO x"+combo,20,120);}if(shield>0){g.setColor(CYAN);g.drawString("SHIELD "+(int)Math.ceil(shield)+"s",340,120);}if(rapid>0){g.setColor(GOLD);g.drawString("RAPID "+(int)Math.ceil(rapid)+"s",590,120);}
        for(Thing b:bullets){g.setColor(CYAN);g.fillRoundRect((int)b.x-3,(int)b.y-12,6,19,5,5);}
        for(Thing e:enemies){g.setColor(e.type==1?GOLD:RED);g.fill(new Ellipse2D.Double(e.x-e.r,e.y-e.r,e.r*2,e.r*2));g.setColor(BG);g.fillOval((int)e.x-7,(int)e.y-5,5,5);g.fillOval((int)e.x+3,(int)e.y-5,5,5);if(e.type==1){g.setColor(Color.WHITE);g.drawOval((int)(e.x-e.r-4),(int)(e.y-e.r-4),(int)(2*e.r+8),(int)(2*e.r+8));}}
        for(Thing p:pickups){g.setColor(p.type==0?CYAN:p.type==1?GOLD:new Color(118,255,137));g.fillOval((int)p.x-14,(int)p.y-14,28,28);g.setColor(BG);g.setFont(new Font("SansSerif",Font.BOLD,17));g.drawString(p.type==0?"S":p.type==1?"R":"+",(int)p.x-6,(int)p.y+6);}
        for(Thing p:particles){g.setColor(new Color((int)p.age));g.fillOval((int)p.x,(int)p.y,(int)p.r*2,(int)p.r*2);}
        if(invuln<=0||((int)(t*12)%2)==0){if(shield>0){g.setColor(CYAN);g.drawOval((int)x-30,(int)y-30,60,60);}g.setColor(CYAN);Path2D ship=new Path2D.Double();ship.moveTo(x,y-25);ship.lineTo(x-18,y+19);ship.lineTo(x,y+10);ship.lineTo(x+18,y+19);ship.closePath();g.fill(ship);g.setColor(Color.WHITE);g.fillOval((int)x-4,(int)y-5,8,8);}
        if(!playing||paused||dead){g.setColor(new Color(5,9,22,225));g.fillRect(0,0,W,H);center(g,dead?"GAME OVER":paused?"PAUSED":"OVERDRIVE",272,54,CYAN);center(g,dead?"Score "+score+"  |  Best "+best:"Shoot enemies. Survive escalating waves.",324,22,Color.WHITE);center(g,dead?"ENTER  -  try again":paused?"P  -  resume":"ENTER  -  launch",390,28,GOLD);center(g,"WASD / arrows move   |   SPACE shoots   |   P pauses",455,18,Color.LIGHT_GRAY);center(g,"S = shield     R = rapid fire     + = extra life",489,17,Color.LIGHT_GRAY);}
        g.dispose();}
    public void keyPressed(KeyEvent e){int k=e.getKeyCode();if(k==KeyEvent.VK_ENTER&&(!playing||dead))start();if(k==KeyEvent.VK_P&&playing&&!dead)paused=!paused;if(k==KeyEvent.VK_LEFT||k==KeyEvent.VK_A)left=true;if(k==KeyEvent.VK_RIGHT||k==KeyEvent.VK_D)right=true;if(k==KeyEvent.VK_UP||k==KeyEvent.VK_W)up=true;if(k==KeyEvent.VK_DOWN||k==KeyEvent.VK_S)down=true;if(k==KeyEvent.VK_SPACE)fire=true;}
    public void keyReleased(KeyEvent e){int k=e.getKeyCode();if(k==KeyEvent.VK_LEFT||k==KeyEvent.VK_A)left=false;if(k==KeyEvent.VK_RIGHT||k==KeyEvent.VK_D)right=false;if(k==KeyEvent.VK_UP||k==KeyEvent.VK_W)up=false;if(k==KeyEvent.VK_DOWN||k==KeyEvent.VK_S)down=false;if(k==KeyEvent.VK_SPACE)fire=false;}
    public void keyTyped(KeyEvent e){}public void mouseClicked(MouseEvent e){requestFocusInWindow();}public void mousePressed(MouseEvent e){}public void mouseReleased(MouseEvent e){}public void mouseEntered(MouseEvent e){}public void mouseExited(MouseEvent e){}
    public static void main(String[] args){SwingUtilities.invokeLater(()->{JFrame f=new JFrame("Alchemy Dash: Overdrive");AlchemyDashV2 game=new AlchemyDashV2();f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);f.setContentPane(game);f.pack();f.setResizable(false);f.setLocationRelativeTo(null);f.setVisible(true);game.requestFocusInWindow();});}
}
