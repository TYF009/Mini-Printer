import studio.foli.android.PrinterProtocol;
import java.util.Arrays;

/** Run with javac/java, without Android or a third-party test framework. */
public final class ProtocolTest {
    static int assertions;
    static void check(boolean value,String name){assertions++;if(!value)throw new AssertionError(name);}
    static void rejects(Runnable action,String name){try{action.run();throw new AssertionError(name);}catch(IllegalArgumentException expected){assertions++;}}
    public static void main(String[] args){
        int[] pixels=new int[384];Arrays.fill(pixels,0xFFFFFFFF);
        byte[][] white=PrinterProtocol.encode(pixels,384,1,false,0);
        check(white.length==1&&white[0].length==48,"Full 48-byte row");
        for(byte b:white[0])check(b==0,"White does not heat");
        Arrays.fill(pixels,0xFF000000);byte[][] black=PrinterProtocol.encode(pixels,384,1,false,0);
        for(byte b:black[0])check((b&255)==255,"All black bits set");
        Arrays.fill(pixels,0xFFFFFFFF);pixels[0]=pixels[7]=pixels[8]=pixels[383]=0xFF000000;
        byte[] edge=PrinterProtocol.encode(pixels,384,1,false,0)[0];
        check((edge[0]&255)==0x81&&(edge[1]&255)==0x80&&(edge[47]&255)==1,"MSB-first, exact boundary positions");
        Arrays.fill(pixels,0x00000000);for(byte b:PrinterProtocol.encode(pixels,384,1,true,0)[0])check(b==0,"Transparent means white");
        Arrays.fill(pixels,0xFF808080);byte[] gray=PrinterProtocol.encode(pixels,384,1,true,0)[0];
        int dots=0;for(byte b:gray)dots+=Integer.bitCount(b&255);check(dots>130&&dots<255,"Dither distributes gray ink");
        check(Arrays.equals(PrinterProtocol.density(2),new byte[]{(byte)165,(byte)165,(byte)165,(byte)165,2}),"Density command uses 1/2/3 mapping");
        check(Arrays.equals(PrinterProtocol.finish(),new byte[]{(byte)166,(byte)166,(byte)166,(byte)166,1}),"A6 terminator is a separate 5-byte command");
        check(PrinterProtocol.decodeStatus(new byte[]{84,28,0,3})[3]==3,"Status completion enum");
        check(PrinterProtocol.decodeStatus(new byte[]{(byte)255,28,0,3})==null,"Reject impossible battery");
        check(PrinterProtocol.decodeStatus(new byte[]{50,28,2,3})==null,"Reject unknown paper enum");
        check(PrinterProtocol.decodeStatus(PrinterProtocol.finish())==null,"Command cannot be mistaken for status");
        rejects(()->PrinterProtocol.encode(new int[383],383,1,false,0),"Reject fragmented rows");
        rejects(()->PrinterProtocol.encode(new int[384*769],384,769,false,0),"Reject oversized jobs before sending");
        rejects(()->PrinterProtocol.encode(new int[1],384,1,false,0),"Reject bad bitmap size");
        rejects(()->PrinterProtocol.density(30),"Do not confuse density parameter with percent");
        rejects(()->PrinterProtocol.encode(new int[384],384,1,false,81),"Reject invalid brightness");
        System.out.println("PASS: "+assertions+" protocol assertions");
    }
}
