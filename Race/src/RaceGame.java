import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class RaceGame{
	public static int WID = 1200;
	public static int HEI = 800;
	public static void main(String[] args){

		JFrame frame = new JFrame();
		Base base = new Base();
		frame.add(base);
//        frame.setPreferredSize(new Dimension(WID, HEI));
//        frame.setFocusable(true);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}

class Base extends JPanel
	implements Runnable, KeyListener{
	Field field;
	public Base() {
		this.setPreferredSize(new Dimension(RaceGame.WID, RaceGame.HEI));
		this.setFocusable(true);
		this.addKeyListener(this);
		init();
		Thread thread = new Thread(this);
		thread.start();
	}
	
	void init(){
		field = new Field("filename", this);
		keyState = 0;
	}
	
	void update(){
		field.update();
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		field.draw(g);
	}
	
	@Override
	public void run() {
		while(true){
			update();
			repaint();
			sleep(33);
		}
	}
	
	void sleep(long time){
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	int getKeyState(){
		return keyState;
	}
	
	final static int LEFT = 1;
	final static int RIGHT = 2;
	final static int ACCEL = 4;
	final static int BRAKE = 8;
	int keyState;
	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		if(key==KeyEvent.VK_LEFT){
			keyState |= LEFT;
		}else if(key==KeyEvent.VK_RIGHT){
			keyState |= RIGHT;
		}else if(key==KeyEvent.VK_Z){
			keyState |= ACCEL;
		}else if(key==KeyEvent.VK_X){
			keyState |= BRAKE;
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		int key = e.getKeyCode();
		if(key==KeyEvent.VK_LEFT){
			keyState &= ~LEFT;
		}else if(key==KeyEvent.VK_RIGHT){
			keyState &= ~RIGHT;
		}else if(key==KeyEvent.VK_Z){
			keyState &= ~ACCEL;
		}else if(key==KeyEvent.VK_X){
			keyState &= ~BRAKE;
		}
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		
	}
}

class Field{
	Car[] car;
	Base base;
	int[] courceXout;
	int[] courceYout;
	int[] courceXin;
	int[] courceYin;
	Line[] cource;
	public Field(String filename, Base base) {
		this.base = base;
		loadField(filename);
	}
	
	void loadField(String filename){
		car = new Car[]{new Car(200, 100, 0, 0, new Player(base), this)};
		
		courceXout = new int[]{10, RaceGame.WID- 50, RaceGame.WID- 10,               50};
		courceYout = new int[]{50,               10, RaceGame.HEI- 50, RaceGame.HEI- 10};
		courceXin = new int[]{200, RaceGame.WID-200, RaceGame.WID-200,              200};
		courceYin = new int[]{200,              200, RaceGame.HEI-200, RaceGame.HEI-200};
		
		int n = courceXout.length;
		cource = new Line[2*n];
		for(int i=0; i<n; i++){
			cource[i] = new Line(courceXout[i], courceYout[i], courceXout[(i+1)%n], courceYout[(i+1)%n]);
			cource[i+n] = new Line(courceXin[i], courceYin[i], courceXin[(i+1)%n], courceYin[(i+1)%n]);
		}
	}
	
	// l.p1を移動ベクトルの始点とする
	Pos dbg = null;
	Pos dbg2 = null;
	Line dbg3 = null;
	Line dbg4 = null;
	static final double REBOUND = 0.4;
	Line collision(Line l, int r){
		double min = Integer.MAX_VALUE;
		int minID = -1;
		for(int i=0; i<cource.length; i++){
			double d = cource[i].dist(l);
			if(d<min){
				min = d;
				minID = i;
			}
		}
		if(min>r) return null;
		Line ml = cource[minID];
		double t = l.crossPosT(ml);
		if(t==-1){
			if(l.ccw(ml.p1)*l.ccw(ml.p2)>=0 && ml.ccw(l.p1)*ml.ccw(l.p2)<=0){
				Pos s;
				if(l.dist(ml.p1)<l.dist(ml.p2)) s = ml.p1.copy();
				else s = ml.p2.copy();
				int sign = (int)Math.signum(l.ccw(ml.p1));
				Pos n = new Pos(sign*l.vec.y+s.x, sign*l.vec.x+s.y);
				t = l.crossPosT(new Line(s, n));
			}else t = 1;
		}
		double left = 0;
		double right = t;
		for(int i=0; i<20; i++){
			t = (left+right)/2;
			double d = ml.dist(new Pos(l.p1.x+(l.p2.x-l.p1.x)*t, l.p1.y+(l.p2.y-l.p1.y)*t));
			if(d<r) right = t;
			else left = t;
		}
		Pos cp = new Pos(l.p1.x+(l.p2.x-l.p1.x)*t, l.p1.y+(l.p2.y-l.p1.y)*t);
		Pos p = ml.nearPoint(cp);
		Pos hv = new Pos(l.p2.x-cp.x, l.p2.y-cp.y);
		Pos pcp = new Pos(cp.x-p.x, cp.y-p.y).resizeVec(1);
		double hvcos = hv.x*pcp.x+hv.y*pcp.y;
		pcp.mult(-hvcos*(1+REBOUND));
		hv.add(pcp);//.add(pcp.mult(-1));
//		dbg2 = cp;
//		dbg = p;
		Line res = new Line(cp, hv.add(cp));
//		dbg3 = new Line(cp, pcp.add(cp));
//		dbg4 = l;
		return res;
	}
	
	void update(){
		for(Car c: car) c.update();
	}
	
	void draw(Graphics g){
		// draw field
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, RaceGame.WID, RaceGame.HEI);
		
		g.setColor(Color.ORANGE);
//		g.drawPolygon(courceXout, courceYout, courceXout.length);
//		g.drawPolygon(courceXin, courceYin, courceXin.length);
		for(Line l: cource) l.draw(g);
		
		// draw cars
		g.setColor(Color.WHITE);
		for(Car c: car) c.draw(g);
		
		if(dbg != null){
			drawCircle(dbg, 3, g);
			drawCircle(dbg2, Car.R, g);
			dbg3.draw(g);
			dbg4.draw(g);
		}
	}
	
	void drawCircle(Pos p, int r, Graphics g){
		g.drawOval((int)p.x-r, (int)p.y-r, 2*r, 2*r);
	}
}

class Car{
	double dir;
	double x, y;
	double vx, vy;
	final static double ACC = 1;
	final static double DEC = 0.92;
	final static double HANDLE_MAX = Math.PI/50;
	final static double DIR_MAX = Math.PI*2;
	final static double AIR = 0.02;
	int id;
	AI ai;
	Field field;
	public Car(double x, double y, double dir, int id, AI ai, Field f) {
		this.x = x;
		this.y = y;
		this.dir = dir;
		this.id = id;
		this.ai = ai;
		field = f;
	}
	
	void accel(){
		vx += ACC*Math.cos(dir);
		vy += ACC*Math.sin(dir);
	}
	
	void brake(){
		vx *= DEC;
		vy *= DEC;
	}
	
	void handle(double val){
		if(val>HANDLE_MAX) val = HANDLE_MAX;
		else if(val<-HANDLE_MAX) val = -HANDLE_MAX;
		dir += val;
		if(dir < 0) dir += DIR_MAX;
		else if(dir > DIR_MAX) dir -= DIR_MAX;
	}
	
	final static double DEC_BOUND = 0.5;
	void update(){
		ai.update(this);
//		speed -= AIR*speed;
		vx -= AIR*vx;
		vy -= AIR*vy;
		Line vec = new Line(x, y, x+vx, y+vy);
		if(vx!=0||vy!=0){
			Line newvec = field.collision(vec, R);
			if(newvec != null){
				x = newvec.p1.x+newvec.vec.x;
				y = newvec.p1.y+newvec.vec.y;
				Pos v = newvec.vec.resizeVec(Math.sqrt(vx*vx+vy*vy)*DEC_BOUND);
				vx = v.x;
				vy = v.y;
			}
		}
		x += vx;
		y += vy;
	}
	
	final static int[] triX = {10, -10, -10};
	final static int[] triY = {0, -6, 6};
	final static int R = 5;
	int[] drawX = new int[3];
	int[] drawY = new int[3];
	void draw(Graphics g){
		for(int i=0; i<3; i++){
			drawX[i] = (int)(rotateX(triX[i], triY[i], dir)+x);
			drawY[i] = (int)(rotateY(triX[i], triY[i], dir)+y);
		}
		g.drawPolygon(drawX, drawY, 3);
	}
	
	double rotateX(double x, double y, double dir){
		return x*Math.cos(dir)-y*Math.sin(dir);
	}
	
	double rotateY(double x, double y, double dir){
		return x*Math.sin(dir)+y*Math.cos(dir);
	}
}

abstract class AI{
	abstract void update(Car c);
}

class Player extends AI{
	int key;
	Base base;
	public Player(Base base) {
		this.base = base;
	}
	void setKey(){
		this.key = base.getKeyState();
	}
	@Override
	void update(Car c) {
		setKey();
		if((key&Base.LEFT)>0){
			c.handle(-1);
		}
		if((key&Base.RIGHT)>0){
			c.handle(1);
		}
		if((key&Base.ACCEL)>0){
			c.accel();
		}
		if((key&Base.BRAKE)>0){
			c.brake();
		}
	}
}

class Pos implements Comparable<Pos>{
	static final double EPS = 1e-10;
	double x, y;
	public Pos(double x, double y){
		this.x = x;
		this.y = y;
	}
	public double dist2(Pos p){
		return (x-p.x)*(x-p.x) + (y-p.y)*(y-p.y);
	}
	public Pos copy(){
		return new Pos(x, y);
	}
	public Pos resizeVec(double size){
		double sq = Math.sqrt(x*x+y*y);
		return new Pos(x*size/sq, y*size/sq);
	}
	public Pos mult(double v){
		x *= v;
		y *= v;
		return this;
	}
	public Pos add(Pos v){
		x += v.x;
		y += v.y;
		return this;
	}
	@Override
	public int compareTo(Pos o) {
		if(Math.abs(x-o.x) >= EPS) return Double.compare(x, o.x);
		if(Math.abs(y-o.y) >= EPS) return Double.compare(y, o.y);
		return 0;
	}
}

class Line{
	static final double EPS = 1e-10;
	Pos p1, p2;
	Pos vec;
	public Line(Pos p1, Pos p2){
		this.p1 = p1;
		this.p2 = p2;
		vec = new Pos(p2.x-p1.x, p2.y-p1.y);
	}
	public Line(double x1, double y1, double x2, double y2){
		p1 = new Pos(x1, y1);
		p2 = new Pos(x2, y2);
		vec = new Pos(x2-x1, y2-y1);
	}
	Pos getNVec(){
		return new Pos(-vec.y, vec.x);
	}
	Pos crossPos(Line l){
		if(!cross(l)) return null;
		double d1 = Math.abs(cross(l.vec.x, l.vec.y, p1.x-l.p1.x, p1.y-l.p1.y));
		double d2 = Math.abs(cross(l.vec.x, l.vec.y, p2.x-l.p1.x, p2.y-l.p1.y));
		double t = d1/(d1+d2);
		return new Pos(p1.x+(p2.x-p1.x)*t, p1.y+(p2.y-p1.y)*t);
	}
	double crossPosT(Line l){
		if(!cross(p1, p2, l.p1, l.p2)) return -1;
		double d1 = Math.abs(cross(l.vec.x, l.vec.y, p1.x-l.p1.x, p1.y-l.p1.y));
		double d2 = Math.abs(cross(l.vec.x, l.vec.y, p2.x-l.p1.x, p2.y-l.p1.y));
		return d1/(d1+d2);
	}
	void draw(Graphics g){
		g.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
	}
	boolean link(Line l){
		return p1.compareTo(l.p1) == 0
				|| p1.compareTo(l.p2) == 0
				|| p2.compareTo(l.p1) == 0
				|| p2.compareTo(l.p2) == 0;
	}
	boolean cross(Line l){
		return cross(p1, p2, l.p1, l.p2);
	}
	static boolean cross(Pos p1, Pos p2, Pos p3, Pos p4){
		return ccw(p1, p2, p3) * ccw(p1, p2, p4) < 0 && ccw(p3, p4, p1) * ccw(p3, p4, p2) < 0;
	}
	static double cross(Pos a, Pos b){
		return a.x*b.y - b.x*a.y;
	}
	public static double cross(double x1, double y1, double x2, double y2){
		return x1*y2 - x2*y1;
	}
	// 上正座標で反時計回りが正
	// 下正なら当然逆になる
	static double ccw(Pos a, Pos b, Pos c){
		double dx1 = b.x - a.x;
		double dy1 = b.y - a.y;
		double dx2 = c.x - a.x;
		double dy2 = c.y - a.y;
		return dx1*dy2 - dx2*dy1;
	}
	double ccw(Pos p){
		double dx1 = p2.x - p1.x;
		double dy1 = p2.y - p1.y;
		double dx2 = p.x - p1.x;
		double dy2 = p.y - p1.y;
		return dx1*dy2 - dx2*dy1;
	}
	public static double dist(Pos a, Pos b, Pos c){
        // a->b からcへの距離
        if(dot(b.x-a.x, b.y-a.y, c.x-a.x, c.y-a.y) < EPS)
            return Math.sqrt(a.dist2(c));
        if(dot(a.x-b.x, a.y-b.y, c.x-b.x, c.y-b.y) < EPS)
            return Math.sqrt(b.dist2(c));
        return Math.abs(ccw(a, b, c))/Math.sqrt(a.dist2(b));
    }
	public double dist(Pos p){
        // this からpへの距離
        if(dot(p2.x-p1.x, p2.y-p1.y, p.x-p1.x, p.y-p1.y) < EPS)
            return Math.sqrt(p1.dist2(p));
        if(dot(p1.x-p2.x, p1.y-p2.y, p.x-p2.x, p.y-p2.y) < EPS)
            return Math.sqrt(p2.dist2(p));
        return Math.abs(ccw(p1, p2, p))/Math.sqrt(p1.dist2(p2));
	}
	public double dist(Line l){
		if(cross(l)) return 0;
		double res1 = Math.min(dist(p1, p2, l.p1), dist(p1, p2, l.p2));
		double res2 = Math.min(dist(l.p1, l.p2, p1), dist(l.p1, l.p2, p2));
		return Math.min(res1, res2);
	}
	public Pos nearPoint(Pos p){
		if(dot(p2.x-p1.x, p2.y-p1.y, p.x-p1.x, p.y-p1.y) < EPS)
            return p1;
        if(dot(p1.x-p2.x, p1.y-p2.y, p.x-p2.x, p.y-p2.y) < EPS)
            return p2;
        return getNVec().resizeVec(1).mult(-ccw(p1, p2, p)/Math.sqrt(p1.dist2(p2))).add(p);
	}
	public static double dot(double x, double y, double x1, double y2){
		return x*x1 + y*y2;
	}
}