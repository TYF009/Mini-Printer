package studio.foli.android;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Original small outline icons; interactive parents supply accessible labels. */
final class FoliIcon extends View {
    final String kind;final int color;
    private final Paint p=new Paint(3);
    private final Path path=new Path();
    public FoliIcon(Context c){this(c,"note",MainActivity.INK);}
    FoliIcon(Context c,String kind,int color){super(c);this.kind=kind;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/24f,getHeight()/24f);
        p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        path.reset();
        switch(kind){
            case "home":path.moveTo(3,11);path.lineTo(12,3);path.lineTo(21,11);path.lineTo(21,21);path.lineTo(15,21);path.lineTo(15,15);path.lineTo(9,15);path.lineTo(9,21);path.lineTo(3,21);path.close();break;
            case "photo":c.drawRect(3,4,21,20,p);c.drawCircle(16,8,1,p);path.moveTo(3,17);path.lineTo(9,11);path.lineTo(14,16);path.lineTo(17,13);path.lineTo(21,17);break;
            case "grid":for(int i=0;i<2;i++)for(int j=0;j<2;j++)c.drawRoundRect(3+i*11,3+j*11,10+i*11,10+j*11,1,1,p);break;
            case "printer":c.drawRoundRect(3,8,21,17,2,2,p);c.drawRect(6,3,18,8,p);c.drawRect(6,13,18,22,p);c.drawPoint(17,11,p);break;
            case "back":path.moveTo(15,5);path.lineTo(8,12);path.lineTo(15,19);break;
            case "undo":path.moveTo(8,5);path.lineTo(3,10);path.lineTo(8,15);path.moveTo(3,10);path.lineTo(14,10);path.cubicTo(23,10,23,22,13,21);break;
            case "arrow":path.moveTo(4,12);path.lineTo(20,12);path.moveTo(14,6);path.lineTo(20,12);path.lineTo(14,18);break;
            case "battery":c.drawRect(2,6,20,18,p);c.drawLine(22,10,22,14,p);c.drawLine(5,10,14,10,p);c.drawLine(5,14,14,14,p);break;
            case "heart":path.moveTo(12,20);path.cubicTo(-4,10,5,-2,12,6);path.cubicTo(19,-2,28,10,12,20);break;
            case "rotate":c.drawArc(4,4,21,21,220,300,false,p);path.moveTo(3,2);path.lineTo(3,8);path.lineTo(9,8);break;
            case "style":for(int x=5;x<=19;x+=7)c.drawLine(x,3,x,21,p);c.drawLine(2,8,8,8,p);c.drawLine(9,16,15,16,p);c.drawLine(16,10,22,10,p);break;
            case "bluetooth":path.moveTo(7,6);path.lineTo(17,17);path.lineTo(12,22);path.lineTo(12,2);path.lineTo(17,7);path.lineTo(7,18);break;
            case "mark":path.moveTo(7,4);path.lineTo(17,4);path.quadTo(21,4,21,8);path.lineTo(21,18);path.quadTo(21,22,17,22);path.lineTo(6,22);path.lineTo(6,10);path.quadTo(6,7,10,7);path.lineTo(16,7);path.moveTo(10,12);path.lineTo(17,12);path.moveTo(10,17);path.lineTo(15,17);path.moveTo(6,22);path.quadTo(1,22,1,18);path.lineTo(6,18);break;
            default:c.drawRect(5,3,19,22,p);c.drawLine(9,9,16,9,p);c.drawLine(9,14,16,14,p);c.drawLine(9,18,14,18,p);
        }
        c.drawPath(path,p);c.restore();
    }
}
