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
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(7,10,22));
        getWindow().setNavigationBarColor(Color.rgb(7,10,22));
        getWindow().getDecorView().setSystemUiVisibility(0);
        setContentView(new GameView(this));
    }
    @Override public void onBackPressed() {
        View v = findViewById(android.R.id.content);
        if (v instanceof GameView) ((GameView)v).back();
        else super.onBackPressed();
    }

    static class GameView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rng = new Random();
        final ToneGenerator sound = new ToneGenerator(AudioManager.STREAM_MUSIC, 65);
        final int bg=Color.rgb(7,10,22), panel=Color.rgb(17,23,45), wall=Color.rgb(37,49,83),
            cyan=Color.rgb(43,242,224), orange=Color.rgb(255,142,68), white=Color.rgb(245,248,255),
            muted=Color.rgb(139,153,186), pink=Color.rgb(255,69,127), gold=Color.rgb(255,211,82);
        int cols=11, rows=31, level=1, score=0, lives=3, px=1, py=1, coinsTaken=0;
        int[][] map;
        ArrayList<Point> coins=new ArrayList<>(), traps=new ArrayList<>();
        boolean started=false, paused=false, dead=false, won=false;
        float cell, ox, oy, downX, downY, animX, animY, targetX, targetY;
        long lastAnim=0, invincibleUntil=0;
        GameView(Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null); setFocusable(true); makeLevel(); }
        void makeLevel() {
            map=new int[rows][cols];
            for(int y=0;y<rows;y++) for(int x=0;x<cols;x++)
                map[y][x]=(x==0||y==0||x==cols-1||y==rows-1)?1:0;
            // Alternating cross-walls create a readable, solvable vertical run.
            for(int y=2;y<rows-2;y+=2) {
                int gap=1+rng.nextInt(cols-2);
                for(int x=1;x<cols-1;x++) if(x!=gap) map[y][x]=1;
                if(y+1<rows-1) for(int x=1;x<cols-1;x++) map[y+1][x]=0;
            }
            for(int x=1;x<cols-1;x++) { map[1][x]=0; map[rows-2][x]=0; }
            px=1; py=1; animX=px; animY=py; coins.clear(); traps.clear(); coinsTaken=0;
            HashSet<String> used=new HashSet<>();
            for(int i=0;i<Math.min(7+level,24);i++) {
                int x=1+rng.nextInt(cols-2), y=2+rng.nextInt(rows-4);
                if(map[y][x]==0 && !(x==px&&y==py) && used.add(x+","+y)) coins.add(new Point(x,y));
            }
            for(int i=0;i<Math.min(2+level/2,8);i++) {
                int x=1+rng.nextInt(cols-2), y=2+rng.nextInt(rows-4);
                if(map[y][x]==0 && used.add(x+","+y)) traps.add(new Point(x,y));
            }
            map[rows-2][cols-2]=2; won=false; dead=false; paused=false; invalidate();
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); float w=getWidth(), h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h,Color.rgb(8,15,35),Color.rgb(29,12,37),Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p); p.setShader(null);
            drawHeader(c,w);
            float top=h*.155f, bottom=h*.755f;
            cell=Math.min((w-34)/cols,(bottom-top)/11.3f);
            ox=(w-cell*cols)/2;
            float visibleRows=(bottom-top)/cell;
            float camera=Math.max(0,Math.min(rows-visibleRows,py-visibleRows*.43f));
            oy=top-camera*cell;
            drawMaze(c,top,bottom);
            drawControls(c,w,h);
            if(!started) overlay(c,w,h,"MASKRUN","TOQUE PARA COMEÇAR","DESLIZE PARA CORRER");
            else if(won) overlay(c,w,h,"SETOR LIMPO!","VOCÊ SOBREVIVEU","TOQUE PARA PRÓXIMA FASE");
            else if(dead) overlay(c,w,h,"FIM DE JOGO","AS ARMADILHAS VENCERAM","TOQUE PARA TENTAR NOVAMENTE");
            else if(paused) overlay(c,w,h,"PAUSADO","RESPIRA. PLANEJA. CORRE.","TOQUE PARA CONTINUAR");
            if(started&&!paused&&!won&&!dead) postInvalidateDelayed(16);
        }
        void drawHeader(Canvas c,float w) {
            text(c,"MASKRUN",22,30,white,true,20,Paint.Align.LEFT);
            text(c,"NEON ESCAPE",23,47,cyan,true,9,Paint.Align.LEFT);
            round(c,w-116,14,w-18,48,16,panel);
            text(c,"SETOR "+String.format(Locale.US,"%02d",level),w-67,34,white,true,12,Paint.Align.CENTER);
            round(c,16,57,w-16,94,13,Color.rgb(16,22,43));
            text(c,"SCORE",28,73,muted,true,9,Paint.Align.LEFT);
            text(c,String.format(Locale.US,"%06d",score),28,87,gold,true,15,Paint.Align.LEFT);
            text(c,"◆ "+coinsTaken,w*.53f,80,gold,true,12,Paint.Align.CENTER);
            text(c,"♥ "+lives,w-30,80,pink,true,13,Paint.Align.RIGHT);
        }
        void drawMaze(Canvas c,float top,float bottom) {
            c.save(); c.clipRect(8,top-4,getWidth()-8,bottom+4);
            round(c,ox-5,oy-5,ox+cell*cols+5,oy+cell*rows+5,16,Color.rgb(13,18,38));
            for(int y=0;y<rows;y++) for(int x=0;x<cols;x++) {
                float l=ox+x*cell,t=oy+y*cell;
                if(t>bottom+cell||t+cell<top-cell) continue;
                if(map[y][x]==1) {
                    round(c,l+1,t+1,l+cell-1,t+cell-1,cell*.18f,wall);
                    p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1);
                    p.setColor(Color.rgb(57,75,119)); c.drawRoundRect(l+3,t+3,l+cell-3,t+cell-3,cell*.15f,cell*.15f,p); p.setStyle(Paint.Style.FILL);
                    p.setColor(Color.argb(35,43,242,224)); c.drawLine(l+cell*.2f,t+cell*.2f,l+cell*.8f,t+cell*.2f,p);
                } else {
                    p.setColor(Color.rgb(12,17,35)); c.drawRect(l,t,l+cell,t+cell,p);
                    p.setColor(Color.rgb(25,34,58)); c.drawCircle(l+cell/2,t+cell/2,1.2f,p);
                }
            }
            for(Point a:coins) {
                float x=ox+(a.x+.5f)*cell,y=oy+(a.y+.5f)*cell;
                if(y<top-cell||y>bottom+cell) continue;
                p.setColor(Color.argb(45,255,211,82)); c.drawCircle(x,y,cell*.38f,p);
                p.setColor(gold); c.drawCircle(x,y,cell*.17f,p);
                p.setColor(white); c.drawCircle(x-cell*.045f,y-cell*.045f,cell*.045f,p);
            }
            for(Point a:traps) {
                float x=ox+(a.x+.5f)*cell,y=oy+(a.y+.5f)*cell;
                if(y<top-cell||y>bottom+cell) continue;
                p.setColor(Color.argb(45,255,69,127)); c.drawCircle(x,y,cell*.37f,p);
                p.setColor(pink); Path path=new Path(); path.moveTo(x,y-cell*.27f); path.lineTo(x+cell*.25f,y+cell*.21f); path.lineTo(x-cell*.25f,y+cell*.21f); path.close(); c.drawPath(path,p);
            }
            float ex=ox+(cols-2+.5f)*cell,ey=oy+(rows-2+.5f)*cell;
            if(ey>=top-cell&&ey<=bottom+cell) {
                p.setColor(Color.argb(55,43,242,224)); c.drawCircle(ex,ey,cell*.46f,p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(cyan); c.drawCircle(ex,ey,cell*.29f,p); p.setStyle(Paint.Style.FILL);
                text(c,"EXIT",ex,ey+cell*.58f,cyan,true,8,Paint.Align.CENTER);
            }
            float now=System.currentTimeMillis();
            float ease=Math.min(1f,(now-lastAnim)/115f);
            if(lastAnim==0) ease=1;
            animX += (targetX-animX)*Math.max(.12f,ease);
            animY += (targetY-animY)*Math.max(.12f,ease);
            float x=ox+(animX+.5f)*cell,y=oy+(animY+.5f)*cell;
            float pulse=(float)(1+Math.sin(now/130.0)*.08);
            p.setColor(Color.argb(60,43,242,224)); c.drawCircle(x,y,cell*.49f*pulse,p);
            p.setColor(white); c.drawCircle(x,y,cell*.31f,p);
            p.setColor(cyan); c.drawCircle(x,y,cell*.23f,p);
            p.setColor(bg); c.drawCircle(x-cell*.07f,y-cell*.04f,cell*.045f,p); c.drawCircle(x+cell*.07f,y-cell*.04f,cell*.045f,p);
            c.restore();
        }
        void drawControls(Canvas c,float w,float h) {
            float cy=h*.855f,r=Math.min(w*.095f,39);
            control(c,w/2,cy-r*1.03f,r,"↑"); control(c,w/2-r*1.14f,cy,r,"←");
            control(c,w/2,cy,r,"↓"); control(c,w/2+r*1.14f,cy,r,"→");
            text(c,"DESLIZE PARA SE MOVER",w/2,h-17,muted,false,9,Paint.Align.CENTER);
            round(c,w-58,101,w-17,139,12,Color.rgb(27,35,65));
            text(c,paused?"▶":"Ⅱ",w-37,126,white,true,16,Paint.Align.CENTER);
        }
        void control(Canvas c,float x,float y,float r,String s) {
            p.setColor(Color.rgb(24,33,61)); c.drawCircle(x,y,r,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.4f); p.setColor(Color.rgb(62,85,126)); c.drawCircle(x,y,r,p); p.setStyle(Paint.Style.FILL);
            text(c,s,x,y+6,cyan,true,18,Paint.Align.CENTER);
        }
        void overlay(Canvas c,float w,float h,String title,String subtitle,String hint) {
            p.setColor(Color.argb(220,5,8,20)); c.drawRect(0,h*.32f,w,h*.62f,p);
            round(c,18,h*.35f,w-18,h*.59f,22,Color.rgb(17,24,47));
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1); p.setColor(Color.rgb(42,91,113)); c.drawRoundRect(18,h*.35f,w-18,h*.59f,22,22,p); p.setStyle(Paint.Style.FILL);
            text(c,title,w/2,h*.425f,white,true,24,Paint.Align.CENTER);
            text(c,subtitle,w/2,h*.465f,cyan,true,11,Paint.Align.CENTER);
            text(c,hint,w/2,h*.525f,muted,false,10,Paint.Align.CENTER);
        }
        void round(Canvas c,float l,float t,float r,float b,float rad,int color) { p.setShader(null); p.setColor(color); c.drawRoundRect(l,t,r,b,rad,rad,p); }
        void text(Canvas c,String s,float x,float y,int color,boolean bold,float size,Paint.Align align) {
            p.setShader(null); p.setColor(color); p.setTextSize(size); p.setTextAlign(align);
            p.setTypeface(bold?Typeface.create("sans-serif-black",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));
            c.drawText(s,x,y,p);
        }
        void back() { if(started&&!paused&&!won&&!dead) paused=true; else {started=false;paused=false;} invalidate(); }
        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            float x=e.getX(),y=e.getY();
            if(e.getAction()==MotionEvent.ACTION_DOWN) { downX=x; downY=y; return true; }
            if(e.getAction()==MotionEvent.ACTION_UP) {
                float dx=x-downX,dy=y-downY;
                if(!started) { started=true; paused=false; invalidate(); return true; }
                if(won) { level++; makeLevel(); started=true; invalidate(); return true; }
                if(dead) { score=0;level=1;lives=3;makeLevel();started=true;invalidate();return true; }
                if(Math.abs(dx)<18&&Math.abs(dy)<18&&x>getWidth()-75&&y<155) { paused=!paused; invalidate(); return true; }
                if(paused) { paused=false; invalidate(); return true; }
                if(Math.max(Math.abs(dx),Math.abs(dy))>18) {
                    if(Math.abs(dx)>Math.abs(dy)) move(dx>0?1:-1,0); else move(0,dy>0?1:-1);
                } else {
                    float w=getWidth(),h=getHeight(),cy=h*.855f,r=Math.min(w*.095f,39);
                    if(y>h*.75f) {
                        if(Math.abs(x-w/2)<r*1.8f) move(0,y<cy-r*.45f?-1:1);
                        else if(x<w/2) move(-1,0); else move(1,0);
                    }
                }
                invalidate(); return true;
            }
            return true;
        }
        void move(int dx,int dy) {
            if(paused||won||dead) return;
            int x=px,y=py;
            ArrayList<Point> path=new ArrayList<>();
            while(true) {
                int nx=x+dx,ny=y+dy;
                if(nx<0||nx>=cols||ny<0||ny>=rows||map[ny][nx]==1) break;
                x=nx;y=ny; path.add(new Point(x,y));
                if(map[y][x]==2) break;
                boolean hitTrap=false;
                for(Point t:traps) if(t.x==x&&t.y==y) { hitTrap=true; break; }
                if(hitTrap) break;
            }
            if(path.isEmpty()) { sound.startTone(ToneGenerator.TONE_PROP_NACK,45); return; }
            px=x;py=y; targetX=px;targetY=py; lastAnim=System.currentTimeMillis();
            for(Point step:path) {
                for(Iterator<Point> it=coins.iterator();it.hasNext();) {
                    Point a=it.next();
                    if(a.x==step.x&&a.y==step.y) { it.remove();score+=100;coinsTaken++;sound.startTone(ToneGenerator.TONE_PROP_ACK,45); }
                }
                for(Iterator<Point> it=traps.iterator();it.hasNext();) {
                    Point a=it.next();
                    if(a.x==step.x&&a.y==step.y) {
                        it.remove();
                        if(System.currentTimeMillis()>invincibleUntil) {
                            lives--;invincibleUntil=System.currentTimeMillis()+700;
                            sound.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,180);
                        }
                        if(lives<=0) {dead=true;sound.startTone(ToneGenerator.TONE_SUP_ERROR,300);}
                    }
                }
            }
            if(map[py][px]==2 || (px==cols-2&&py==rows-2)) { won=true;score+=500+coins.size()*20;sound.startTone(ToneGenerator.TONE_PROP_ACK,180); }
        }
    }
}