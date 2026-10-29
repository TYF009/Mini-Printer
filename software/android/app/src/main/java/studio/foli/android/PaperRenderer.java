package studio.foli.android;

import android.graphics.*;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

final class PaperRenderer {
    static final class Paper {
        final Bitmap bitmap;
        final byte[][] rows;
        Paper(Bitmap bitmap,byte[][] rows){this.bitmap=bitmap;this.rows=rows;}
    }
    static Bitmap note(String title,String content,int size,boolean checklist){
        return note(title,content,size,checklist,0,0);
    }
    static Bitmap note(String title,String content,int size,boolean checklist,int style,int alignment){
        TextPaint body=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(Color.BLACK);body.setTextSize(size);body.setTypeface(Typeface.create(style==1?"serif":"sans-serif",Typeface.NORMAL));
        String value=content.trim();
        if(checklist){
            StringBuilder out=new StringBuilder();
            for(String s:value.split("\n"))if(!s.trim().isEmpty()){
                if(out.length()>0)out.append('\n');
                out.append(s.trim());
            }
            value=out.toString();
        }
        StaticLayout layout=StaticLayout.Builder.obtain(value,0,value.length(),body,checklist?296:320)
                .setAlignment(!checklist&&alignment==1?Layout.Alignment.ALIGN_CENTER:Layout.Alignment.ALIGN_NORMAL).setLineSpacing(9,1.05f).setIncludePad(false).build();
        int height=Math.max(300,132+layout.getHeight()+56);
        if(height>PrinterProtocol.MAX_ROWS)throw new IllegalArgumentException("This note is too long. Shorten the text or choose a smaller type size.");
        Bitmap bitmap=Bitmap.createBitmap(384,height,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);c.drawColor(Color.WHITE);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setTextSize(12);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        String eyebrow=style==1?"EVERYDAY, ON PAPER":style==2?"A NOTE FOR YOU":checklist?"A LITTLE FOCUS":"A LITTLE NOTE";
        c.drawText(eyebrow,32,37,p);p.setTextSize(27);p.setTypeface(Typeface.create(style==1?"serif":"sans-serif",Typeface.BOLD));
        // Keep the title inside the printable content area, including wide unicode titles.
        float width=p.measureText(title);if(width>320)p.setTextSize(27*320/width);
        c.drawText(title,32,79,p);p.setStrokeWidth(style==1?2:1);c.drawLine(32,101,352,101,p);
        if(style==2){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);c.drawRoundRect(15,15,369,height-14,8,8,p);p.setStyle(Paint.Style.FILL);}
        c.save();c.translate(checklist?56:32,124);layout.draw(c);c.restore();
        if(checklist){
            // Draw real raster geometry: font fallback glyphs can lose thin edges
            // when the final print image is thresholded on older Android builds.
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);
            for(int i=0;i<layout.getLineCount();i++){
                int start=layout.getLineStart(i);
                if(start>=value.length())continue;
                if(i==0||value.charAt(start-1)=='\n'){
                    float y=124+layout.getLineBaseline(i)-size*0.72f;
                    c.drawRect(32,y,44,y+12,p);
                }
            }
            p.setStyle(Paint.Style.FILL);
        }
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(11);c.drawText("Made with FOLI",32,height-22,p);
        return bitmap;
    }
    static Bitmap photo(Bitmap source,int crop,int turns){
        Bitmap working=source;
        if(turns%4!=0){Matrix m=new Matrix();m.postRotate((turns%4)*90);working=Bitmap.createBitmap(source,0,0,source.getWidth(),source.getHeight(),m,true);}
        int w=working.getWidth(),h=working.getHeight();
        if(crop>0){float ratio=crop==1?1f:0.75f;int cw=w,ch=h;
            if(w/(float)h>ratio)cw=Math.max(1,(int)(h*ratio));else ch=Math.max(1,(int)(w/ratio));
            working=Bitmap.createBitmap(working,(w-cw)/2,(h-ch)/2,cw,ch);w=cw;h=ch;
        }
        int targetH=Math.max(1,(int)Math.round(h*384.0/w));
        float scale=Math.min(1f,(PrinterProtocol.MAX_ROWS-40)/(float)targetH);
        int contentW=Math.max(1,Math.round(384*scale)),contentH=Math.max(1,Math.round(targetH*scale));
        Bitmap output=Bitmap.createBitmap(384,contentH+40,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(output);c.drawColor(Color.WHITE);
        c.drawBitmap(working,null,new Rect((384-contentW)/2,20,(384+contentW)/2,contentH+20),new Paint(Paint.FILTER_BITMAP_FLAG));
        return output;
    }
    static Paper finish(Bitmap source,boolean dither,int brightness){
        int width=source.getWidth(),height=source.getHeight();int[] pixels=new int[width*height];
        source.getPixels(pixels,0,width,0,0,width,height);
        byte[][] rows=PrinterProtocol.encode(pixels,width,height,dither,brightness);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)pixels[y*width+x]=(rows[y][x/8]&(0x80>>(x%8)))!=0?Color.BLACK:Color.WHITE;
        Bitmap output=Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888);
        return new Paper(output,rows);
    }
}
