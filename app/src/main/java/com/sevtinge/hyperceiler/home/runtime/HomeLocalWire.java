/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.home.runtime;

import com.sevtinge.hyperceiler.libhook.provider.HomeRecentsHapticPrefs;
import com.sevtinge.hyperceiler.libhook.provider.HomeDockPrefs;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** Network-order framing; the embedded HCL4 Parcel remains unchanged. */
public final class HomeLocalWire {
    public static final int VERSION=7, ACK=0x48434143;
    public static final int CONTROL_VERSION=10;
    public static final int ACK_ACCEPTED=1, ACK_NATIVE_CARDS=2;
    public static final int HAPTIC_REQUEST=0x48434852, HAPTIC_ACK=0x48434841;
    public static final int HAPTIC_PREPARE=0x48434850, HAPTIC_WAVE=0x48434857, MAX_HAPTIC_WAVE=8192;
    public static final int PROFILE_BYTES=12, DOCK_BYTES=40, MAX_PAYLOAD=256*1024;
    private HomeLocalWire() {}
    public record ConfigurationAck(long sequence,int flags) {
        public boolean accepted(){return (flags&ACK_ACCEPTED)!=0;}
        public boolean nativeCards(){return (flags&ACK_NATIVE_CARDS)!=0;}
    }
    public static ConfigurationAck readConfigurationAck(DataInputStream input,java.util.function.LongSupplier published,
                                                        ConfigurationAck previous) throws IOException {
        if(input.readInt()!=ACK)throw new IOException("Configuration acknowledgement mismatch");
        long sequence=input.readLong();int flags=input.readInt();
        if(sequence<=0||sequence>published.getAsLong()||(flags!=0&&flags!=ACK_ACCEPTED&&flags!=(ACK_ACCEPTED|ACK_NATIVE_CARDS)))
            throw new IOException("Invalid configuration acknowledgement");
        if(previous!=null&&(sequence<previous.sequence()||(sequence==previous.sequence()
            &&((flags^previous.flags())!=ACK_NATIVE_CARDS||(flags&ACK_ACCEPTED)==0))))
            throw new IOException("Stale configuration acknowledgement");
        return new ConfigurationAck(sequence,flags);
    }
    public static void writeSnapshot(DataOutputStream output,byte[] layout,
                                     HomeRecentsHapticPrefs.Profile profile,Set<String> cards,HomeDockPrefs.Profile dock,long sequence,int version) throws IOException {
        writeSnapshot(output,layout,profile,cards,dock,null,sequence,version);
    }
    public static void writeSnapshot(DataOutputStream output,byte[] layout,HomeRecentsHapticPrefs.Profile profile,
                                     Set<String> cards,HomeDockPrefs.Profile dock,byte[][] waves,long sequence,int version) throws IOException {
        if(version<4||version>VERSION)throw new IllegalArgumentException("Unsupported configuration channel");
        // Selection was validated at the physical preference boundary. Sort for
        // deterministic bytes; null must not be confused with an empty reset.
        var names=cards==null?java.util.List.<String>of():cards.stream().sorted().toList();
        int dockBytes=version>=5?DOCK_BYTES:0;
        int base=PROFILE_BYTES+4+dockBytes+(version>=7?4:0);
        int metadata=base;
        for(String name:names)metadata+=4+name.length(); // Valid packages are ASCII.
        boolean includeCards=cards!=null&&metadata<=MAX_PAYLOAD-layout.length;
        int payloadMetadata=includeCards?metadata:base;
        boolean includeWaves=version>=7&&waves!=null;
        if(includeWaves) {
            int expected=profile.enabled()?8:profile.close()?1:0;
            if(waves.length!=expected||expected==0)throw new IllegalArgumentException("Invalid waveform count");
            int bytes=0;
            for(byte[] wave:waves) {
                if(wave==null||wave.length==0||wave.length>MAX_HAPTIC_WAVE||(wave.length&3)!=0)
                    throw new IllegalArgumentException("Invalid waveform length");
                bytes+=4+wave.length;
            }
            // The optional waveforms do not displace layout or selected cards.
            includeWaves=bytes<=MAX_PAYLOAD-layout.length-payloadMetadata;
            if(includeWaves)payloadMetadata+=bytes;
        }
        if(layout.length==0||layout.length>MAX_PAYLOAD-payloadMetadata||sequence<=0)
            throw new IllegalArgumentException("Configuration packet exceeds protocol bounds");
        output.writeInt(0x48435043);output.writeInt(version);
        output.writeInt(layout.length+payloadMetadata);output.writeLong(sequence);
        output.writeInt((profile.enabled()?1:0)|(profile.close()?2:0));
        output.writeInt(profile.type());output.writeInt(profile.strength());
        // An extension exceeding the frame budget is unavailable, not a reason
        // to suppress independent layout and feedback updates.
        output.writeInt(includeCards?names.size():-1);
        for(String name:includeCards?names:java.util.List.<String>of()) {
            byte[] bytes=name.getBytes(StandardCharsets.US_ASCII);
            output.writeInt(bytes.length);output.write(bytes);
        }
        if(version>=5) {
            output.writeInt(dock==null?0:1);
            if(dock==null)for(int i=0;i<9;i++)output.writeInt(0);
            else {
                // V6 adds the capsule flag without changing the Dock block length.
                output.writeInt((dock.enabled()?1:0)|(version>=6&&dock.capsule()?2:0));
                output.writeInt(dock.mode());output.writeInt(dock.color());
                output.writeInt(dock.height());output.writeInt(dock.margin());output.writeInt(dock.bottom());
                output.writeInt(dock.radius());output.writeInt(dock.night());output.writeInt(dock.style());
            }
        }
        if(version>=7) {
            output.writeInt(includeWaves?waves.length:0);
            if(includeWaves)for(byte[] wave:waves){output.writeInt(wave.length);output.write(wave);}
        }
        output.write(layout);output.flush();
    }
    public record HapticRequest(int profile,int operation,long timestamp,int count,boolean prepare) {}
    public static HapticRequest readHaptic(DataInputStream input) throws IOException {
        int magic=input.readInt();
        if(magic!=HAPTIC_REQUEST&&magic!=HAPTIC_PREPARE)throw new IOException("Haptic request magic mismatch");
        return new HapticRequest(input.readInt(),input.readInt(),input.readLong(),input.readInt(),magic==HAPTIC_PREPARE);
    }
    public static void writeHapticReply(DataOutputStream output,long timestamp,boolean performed) throws IOException {
        output.writeInt(HAPTIC_ACK);output.writeLong(timestamp);output.writeInt(performed?1:0);output.flush();
    }
    public static void writeHapticWave(DataOutputStream output,long timestamp,byte[] wave) throws IOException {
        if(wave!=null&&(wave.length==0||wave.length>MAX_HAPTIC_WAVE||(wave.length&3)!=0))
            throw new IllegalArgumentException("Invalid haptic wave size");
        output.writeInt(HAPTIC_WAVE);output.writeLong(timestamp);output.writeInt(wave==null?0:wave.length);
        if(wave!=null)output.write(wave);
        output.flush();
    }
}
