/* SPDX-License-Identifier: AGPL-3.0-or-later */
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeExtraGestureHandoff;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeExtraGestureWire;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
public final class ControlGolden {
    public static void main(String[] args) throws IOException {
        var output=new DataOutputStream(System.out);output.writeInt(Integer.reverseBytes(18));
        for(int i=0;i<18;i++) {
            var buffer=new ByteArrayOutputStream();var packet=new DataOutputStream(buffer);
            int status=i==0?0:i==1?2:1;
            HomeExtraGestureWire.writeGrant(new HomeExtraGestureWire.Writer(){
                public void integer(int value){try{packet.writeInt(Integer.reverseBytes(value));}catch(IOException impossible){throw new AssertionError(impossible);}}
                public void wide(long value){try{packet.writeLong(Long.reverseBytes(value));}catch(IOException impossible){throw new AssertionError(impossible);}}
                public void scalar(float value){throw new AssertionError("No float in control frame");}
            },status,status==1?new HomeExtraGestureHandoff.Grant(90,2,i,2000+i):null);
            byte[] bytes=buffer.toByteArray();output.writeInt(Integer.reverseBytes(bytes.length));output.write(bytes);
        }
    }
}
