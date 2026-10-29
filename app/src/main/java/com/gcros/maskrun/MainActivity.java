package com.gcros.maskrun;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(10, 12, 26));
        getWindow().setNavigationBarColor(Color.rgb(10, 12, 26));
        getWindow().getDecorView().setSystemUiVisibility(0);
        setContentView(new GameView(this));
    }

    static class GameView extends View {
        final Paint p = new Paint(3);
        final Random random = new Random(4);
        final ToneGenerator tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 55);
        final int bg=Color.rgb(9,12,28), wall=Color.rgb(37,42,76), cyan=Color.rgb(42,238,225),
                  orange=Color.rgb(255,139,62), white=Color.rgb(244,247,255), muted=Color.rgb(137,149,183),
                  pink=Color.rgb(255,70,135), gold=Color.rgb(255,207,74);
        int[][] map;
        int rows=13, cols=9, px=1, py=1, score=0, level=1, lives=3;
        boolean started=false, won=false, dead=false, paused=false;
        float cell, ox, oy, downX, downY;
        long lastMove=0;
        ArrayList<Point> coins = new ArrayList<>();
        ArrayList<Point> traps = new ArrayList<>();

        GameView(Context c) { super(c); setFocusable(true); makeLevel(); }
        void makeLevel() {
            map=new int[rows][cols];
            for(int y=0;y<rows;y++) for(int x=0;x<cols;x++)
                map[y][x]=(x==0||y==0||x==cols-1||y==rows-1)?1:0;
            // Deterministic, solvable-ish maze corridors.
            for(int y=2;y<rows-1;y+=2) for(int x=1;x<cols-1;x++) {
                if(x!=cols-2 && random.nextFloat()>.24f) map[y][x]=1;
            }
            // Keep a winding path open from spawn to exit.
            for(int x=1;x<cols-1;x++) map[1][x]=0;
            for(int y=1;y<rows-1;y++) map[y][cols-2]=0;
            for(int x=2;x<cols-2;x++) map[rows-2][x]=0;
            map[1][1]=0; map[rows-2][cols-2]=2;
            px=1; py=1; coins.clear(); traps.clear();
            for(int i=0;i<7+level;i++) {
                int x=1+random.nextInt(cols-2), y=1+random.nextInt(rows-2);
                if(map[y][x]==0 && !(x==px&&y==py)) coins.add(new Point(x,y));
            }
            for(int i=0;i<Math.min(2+level,7);i++) {
                int x=1+random.nextInt(cols-2), y=1+random.nextInt(rows-2);
                if(map[y][x]==0 && !(x==px&&y==py)) traps.add(new Point(x,y));
            }
            won=false; dead=false; paused=false; invalidate();
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.drawColor(bg);
            float w=getWidth(), h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h,Color.rgb(13,18,42),Color.rgb(25,12,39),Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p); p.setShader(null);
            drawHeader(c,w);
            float boardTop=h*.19f, boardBottom=h*.76f;
            cell=Math.min((w-34)/cols,(boardBottom-boardTop)/rows);
            ox=(w-cell*cols)/2; oy=boardTop+(boardBottom-boardTop-cell*rows)/2;
            drawBoard(c);
            drawControls(c,w,h);
            if(!started) overlay(c,w,h,"MASKRUN","DESLIZE PARA JOGAR");
            else if(won) overlay(c,w,h,"FASE CONCLUÍDA!","TOQUE PARA CONTINUAR");
            else if(dead) overlay(c,w,h,"OPS!","TOQUE PARA TENTAR DE NOVO");
            else if(paused) overlay(c,w,h,"PAUSADO","TOQUE PARA CONTINUAR");
        }
        void drawHeader(Canvas c,float w) {
            text(c,"MASK",22,28,cyan,true); text(c,"RUN",22+measure("MASK",22),28,orange,true);
            text(c,"FASE "+level,w-24,27,white,true,Paint.Align.RIGHT);
            round(c,16,43,w-16,83,18,Color.rgb(24,29,57));
            text(c,"PONTOS",28,60,muted,true); text(c,String.valueOf(score),28,76,gold,true);
            text(c,"♥  "+lives,w-28,68,pink,true,Paint.Align.RIGHT);
        }
        void drawBoard(Canvas c) {
            round(c,ox-5,oy-5,ox+cell*cols+5,oy+cell*rows+5,18,Color.rgb(21,25,49));
            for(int y=0;y<rows;y++) for(int x=0;x<cols;x++) {
                float l=ox+x*cell,t=oy+y*cell;
                if(map[y][x]==1) {
                    round(c,l+1,t+1,l+cell-1,t+cell-1,5,wall);
                    p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1);
                    p.setColor(Color.rgb(63,73,119)); c.drawRoundRect(l+3,t+3,l+cell-3,t+cell-3,4,4,p); p.setStyle(Paint.Style.FILL);
                } else {
                    p.setColor(Color.rgb(17,21,42)); c.drawRect(l,t,l+cell,t+cell,p);
                    p.setColor(Color.rgb(27,33,61)); c.drawCircle(l+cell/2,t+cell/2,1.2f,p);
                }
            }
            for(Point a:coins) {
                float x=ox+(a.x+.5f)*cell,y=oy+(a.y+.5f)*cell;
                p.setColor(Color.argb(70,255,207,74)); c.drawCircle(x,y,cell*.34f,p);
                p.setColor(gold); c.drawCircle(x,y,cell*.15f,p);
            }
            for(Point a:traps) {
                float x=ox+(a.x+.5f)*cell,y=oy+(a.y+.5f)*cell;
                p.setColor(pink); Path path=new Path(); path.moveTo(x,y-cell*.25f); path.lineTo(x+cell*.22f,y+cell*.2f); path.lineTo(x-cell*.22f,y+cell*.2f); path.close(); c.drawPath(path,p);
            }
            float ex=ox+(cols-2+.5f)*cell, ey=oy+(rows-2+.5f)*cell;
            p.setColor(Color.argb(70,42,238,225)); c.drawCircle(ex,ey,cell*.42f,p);
            p.setColor(cyan); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); c.drawCircle(ex,ey,cell*.25f,p); p.setStyle(Paint.Style.FILL);
            float x=ox+(px+.5f)*cell,y=oy+(py+.5f)*cell;
            p.setColor(Color.argb(75,42,238,225)); c.drawCircle(x,y,cell*.44f,p);
            p.setColor(white); c.drawCircle(x,y,cell*.28f,p);
            p.setColor(cyan); c.drawCircle(x,y,cell*.19f,p);
            p.setColor(bg); c.drawCircle(x-cell*.055f,y-cell*.03f,cell*.045f,p); c.drawCircle(x+cell*.055f,y-cell*.03f,cell*.045f,p);
        }
        void drawControls(Canvas c,float w,float h) {
            float cy=h*.86f, r=Math.min(w*.105f,44);
            button(c,w/2,cy-r*1.08f,r,"↑");
            button(c,w/2-r*1.15f,cy,r,"←"); button(c,w/2,cy,r,"↓"); button(c,w/2+r*1.15f,cy,r,"→");
            text(c,"DESLIZE OU USE AS SETAS",w/2,h-18,muted,false,Paint.Align.CENTER);
            round(c,w-57,95,w-17,132,12,Color.rgb(34,39,72)); text(c,paused?"▶":"Ⅱ",w-37,119,white,true,Paint.Align.CENTER);
        }
        void button(Canvas c,float x,float y,float r,String label) {
            p.setColor(Color.rgb(31,37,70)); c.drawCircle(x,y,r,p);
            p.setColor(Color.rgb(64,78,121)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f); c.drawCircle(x,y,r,p); p.setStyle(Paint.Style.FILL);
            text(c,label,x,y+8,cyan,true,Paint.Align.CENTER);
        }
        void overlay(Canvas c,float w,float h,String title,String sub) {
            p.setColor(Color.argb(205,7,9,24)); c.drawRect(0,h*.34f,w,h*.62f,p);
            text(c,title,w/2,h*.445f,white,true,Paint.Align.CENTER,25);
            text(c,sub,w/2,h*.50f,cyan,true,Paint.Align.CENTER,12);
            text(c,"MOEDAS • ARMADILHAS • LABIRINTOS",w/2,h*.55f,muted,false,Paint.Align.CENTER,10);
        }
        void round(Canvas c,float l,float t,float r,float b,float rad,int color) { p.setColor(color); c.drawRoundRect(l,t,r,b,rad,rad,p); }
        void text(Canvas c,String s,float x,float y,int color,boolean bold) { text(c,s,x,y,color,bold,Paint.Align.LEFT,14); }
        void text(Canvas c,String s,float x,float y,int color,boolean bold,Paint.Align align) { text(c,s,x,y,color,bold,align,14); }
        void text(Canvas c,String s,float x,float y,int color,boolean bold,Paint.Align align,float size) {
            p.setShader(null); p.setColor(color); p.setTextSize(size); p.setTypeface(bold?Typeface.create("sans-serif-condensed",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL)); p.setTextAlign(align); c.drawText(s,x,y,p); p.setTextAlign(Paint.Align.LEFT);
        }
        float measure(String s,float size) { p.setTextSize(size); p.setTypeface(Typeface.DEFAULT_BOLD); return p.measureText(s); }
        boolean blocked(int x,int y) { return x<0||y<0||x>=cols||y>=rows||map[y][x]==1; }
        void move(int dx,int dy) {
            if(!started){started=true;invalidate();return;}
            if(won||dead){if(dead) lives=3; else level++; makeLevel(); started=true; return;}
            if(paused)return;
            if(System.currentTimeMillis()-lastMove<85)return; lastMove=System.currentTimeMillis();
            int x=px,y=py;
            while(!blocked(x+dx,y+dy)) { x+=dx; y+=dy; }
            if(x==px&&y==py){tones.startTone(ToneGenerator.TONE_PROP_BEEP,45);return;}
            px=x;py=y;
            for(Iterator<Point> it=coins.iterator();it.hasNext();) { Point a=it.next(); if(a.x==px&&a.y==py){score+=10;it.remove();tones.startTone(ToneGenerator.TONE_PROP_ACK,70);} }
            for(Point a:traps) if(a.x==px&&a.y==py){lives--;tones.startTone(ToneGenerator.TONE_PROP_NACK,150);if(lives<=0)dead=true;else {px=1;py=1;}break;}
            if(px==cols-2&&py==rows-2){won=true;score+=100;}
            invalidate();
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            float x=e.getX(),y=e.getY();
            if(e.getAction()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}
            if(e.getAction()==MotionEvent.ACTION_UP){
                if(!started||won||dead||paused){if(x>getWidth()-75&&y<150&&started&&!won&&!dead){paused=!paused;invalidate();}else move(0,0);return true;}
                if(x>getWidth()-75&&y<150){paused=!paused;invalidate();return true;}
                float dx=x-downX,dy=y-downY;
                if(Math.abs(dx)>24||Math.abs(dy)>24){if(Math.abs(dx)>Math.abs(dy))move(dx>0?1:-1,0);else move(0,dy>0?1:-1);}
                else {
                    float w=getWidth(),h=getHeight(),cy=h*.86f,r=Math.min(w*.105f,44);
                    if(y>cy-r*2.5f&&y<cy-r*.25f&&Math.abs(x-w/2)<r)move(0,-1);
                    else if(y>cy-r*.8f&&Math.abs(x-w/2)<r)move(0,1);
                    else if(Math.abs(y-cy)<r&&x<w/2)move(-1,0);
                    else if(Math.abs(y-cy)<r&&x>w/2)move(1,0);
                    else if(!started||won||dead||paused) { if(dead)lives=3; else if(won)level++; makeLevel();started=true; }
                }
                invalidate(); return true;
            }
            return true;
        }
    }
}
