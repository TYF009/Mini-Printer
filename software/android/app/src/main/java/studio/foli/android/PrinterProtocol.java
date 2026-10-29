package studio.foli.android;

/** Pure Java encoding shared by the app and host-side regression tests. */
public final class PrinterProtocol {
    public static final int WIDTH = 384;
    public static final int ROW_BYTES = 48;
    // Legacy queue holds 1000 rows and accidentally enqueues the A6 command.
    // Leave headroom, never truncate silently, and never split a 48-byte row.
    public static final int MAX_ROWS = 768;
    private PrinterProtocol() {}

    public static byte[] density(int level) {
        if (level < 1 || level > 3) throw new IllegalArgumentException("Density must be 1, 2 or 3");
        return new byte[]{(byte)0xA5,(byte)0xA5,(byte)0xA5,(byte)0xA5,(byte)level};
    }
    public static byte[] finish() {
        return new byte[]{(byte)0xA6,(byte)0xA6,(byte)0xA6,(byte)0xA6,1};
    }
    public static byte[][] encode(int[] argb, int width, int height, boolean dither, int brightness) {
        if (width != WIDTH || height < 1 || height > MAX_ROWS || argb.length != width * height)
            throw new IllegalArgumentException("Image must be 384 pixels wide and at most 768 rows");
        if (brightness < -80 || brightness > 80) throw new IllegalArgumentException("Brightness out of range");
        byte[][] rows = new byte[height][ROW_BYTES];
        float[] current = new float[width + 2], next = new float[width + 2];
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                int p=argb[y*width+x], alpha=p>>>24;
                float luminance=(((p>>16)&255)*0.299f+((p>>8)&255)*0.587f+(p&255)*0.114f);
                float value=Math.max(0,Math.min(255,(luminance*alpha+255*(255-alpha))/255f+brightness+(dither?current[x+1]:0)));
                boolean black=value<128;
                if (black) rows[y][x/8]|=(byte)(0x80>>(x%8));
                if (dither) {
                    float error=value-(black?0:255);
                    current[x+2]+=error*7/16f;
                    next[x]+=error*3/16f; next[x+1]+=error*5/16f; next[x+2]+=error/16f;
                }
            }
            float[] old=current; current=next; next=old; java.util.Arrays.fill(next,0);
        }
        return rows;
    }
    public static int[] decodeStatus(byte[] bytes) {
        if (bytes == null || bytes.length != 4) return null;
        int[] s=new int[]{bytes[0]&255,bytes[1]&255,bytes[2]&255,bytes[3]&255};
        if (s[0]>100 || s[2]>1 || s[3]>3) return null;
        return s;
    }
}
