package studio.foli.android;

import android.content.Context;
import android.graphics.*;
import android.view.*;

/** Full-screen inspection of the actual output bitmap; never mutates print data. */
final class ZoomPaperView extends View {
    private final Bitmap bitmap;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF destination=new RectF();
    private final ScaleGestureDetector scale;
    private final GestureDetector gestures;
    private float zoom=1,offsetX,offsetY;
    private boolean moved;
    public ZoomPaperView(Context context){this(context,null);}
    ZoomPaperView(Context context,Bitmap bitmap){
        super(context);this.bitmap=bitmap;setContentDescription("Print preview. Pinch to zoom, drag to pan, double tap to reset.");
        scale=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector d){moved=true;zoom=Math.max(1,Math.min(6,zoom*d.getScaleFactor()));if(zoom==1)offsetX=offsetY=0;invalidate();return true;}
        });
        gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent event){return true;}
            @Override public boolean onScroll(MotionEvent first,MotionEvent current,float dx,float dy){moved=true;if(zoom>1&&!scale.isInProgress()){offsetX-=dx;offsetY-=dy;invalidate();}return true;}
            @Override public boolean onDoubleTap(MotionEvent event){zoom=zoom>1?1:2.5f;offsetX=offsetY=0;invalidate();return true;}
        });setFocusable(true);
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean onTouchEvent(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_DOWN)moved=false;scale.onTouchEvent(event);gestures.onTouchEvent(event);if(event.getActionMasked()==MotionEvent.ACTION_UP&&!moved)performClick();return true;}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(bitmap==null)return;float margin=24*getResources().getDisplayMetrics().density;
        float fit=Math.min((getWidth()-margin*2)/bitmap.getWidth(),(getHeight()-margin*2)/bitmap.getHeight());if(fit<=0)return;
        float w=bitmap.getWidth()*fit*zoom,h=bitmap.getHeight()*fit*zoom;
        float maxX=Math.max(0,(w-getWidth())/2+margin),maxY=Math.max(0,(h-getHeight())/2+margin);
        offsetX=Math.max(-maxX,Math.min(maxX,offsetX));offsetY=Math.max(-maxY,Math.min(maxY,offsetY));
        float left=(getWidth()-w)/2+offsetX,top=(getHeight()-h)/2+offsetY;
        destination.set(left,top,left+w,top+h);canvas.drawBitmap(bitmap,null,destination,paint);
    }
}
