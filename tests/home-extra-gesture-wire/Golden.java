/* SPDX-License-Identifier: AGPL-3.0-or-later */
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeExtraGestureState;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeExtraGestureWire;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
public final class Golden {
    private static final ArrayList<byte[]> packets=new ArrayList<>();
    private static byte[] encode(HomeExtraGestureWire.Snapshot snapshot,boolean closed) {
        var bytes=new ByteArrayOutputStream();var stream=new DataOutputStream(bytes);
        HomeExtraGestureWire.write(new HomeExtraGestureWire.Writer(){
            @Override public void integer(int value){try{stream.writeInt(Integer.reverseBytes(value));}catch(IOException impossible){throw new AssertionError(impossible);}}
            @Override public void wide(long value){try{stream.writeLong(Long.reverseBytes(value));}catch(IOException impossible){throw new AssertionError(impossible);}}
            @Override public void scalar(float value){integer(Float.floatToRawIntBits(value));}
        },snapshot,closed);
        return bytes.toByteArray();
    }
    public static void main(String[] args) throws IOException {
        packets.add(encode(null,false));packets.add(encode(null,true));
        packets.add(encode(new HomeExtraGestureWire.Snapshot(90,1,1000,0,12,300,320,null),false));
        var handle=new HomeExtraGestureState.Region(350,2360,730,2400);
        for(int rotation=0;rotation<4;rotation++)for(int mask=0;mask<32;mask++) {
            var geometry=new HomeExtraGestureState.Geometry(1080,2400,rotation,3,2320,72,
                mask%2==0?null:handle,Long.valueOf(7+rotation));
            packets.add(encode(new HomeExtraGestureWire.Snapshot(90+rotation,2+mask,1000,mask,12,300,320,geometry),false));
        }
        boolean refused=false;
        try{new HomeExtraGestureWire.Snapshot(90,1,1000,32,12,300,320,null);}catch(IllegalArgumentException invalid){refused=true;}
        if(!refused)throw new AssertionError("Invalid mask accepted");
        refused=false;
        try{new HomeExtraGestureWire.Snapshot(90,1,1000,1,Float.NaN,300,320,null);}catch(IllegalArgumentException invalid){refused=true;}
        if(!refused)throw new AssertionError("Invalid timing accepted");
        var output=new DataOutputStream(System.out);output.writeInt(Integer.reverseBytes(packets.size()));
        for(var packet:packets){output.writeInt(Integer.reverseBytes(packet.length));output.write(packet);}
    }
}
