import com.sevtinge.hyperceiler.libhook.rules.home.recent.HomeHapticFirmwareNames;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FirmwareNamesTest {
    public static void main(String[] args)throws Exception{
        var files=List.of("209_ScreenLock_P_RTP.bin","402_cardslide_P_RTP.bin","521_AcousticGuitar_P_RTP.bin",
            "../210_FingerprintUnlock_P_RTP.bin","404_RTP.bin","-1_Bad_P_RTP.bin","2147483648_Bad_P_RTP.bin");
        var names=HomeHapticFirmwareNames.index(files);
        check(names.size()==3&&"ScreenLock".equals(names.get(209))&&"cardslide".equals(names.get(402))
            &&"AcousticGuitar".equals(names.get(521)));
        var duplicates=new ArrayList<>(files);duplicates.add("209_screenlock_p_rtp.bin");
        check(HomeHapticFirmwareNames.index(duplicates).containsKey(209));
        duplicates.add("209_Unrelated_P_RTP.bin");duplicates.add("209_ScreenLock_P_RTP.bin");
        check(!HomeHapticFirmwareNames.index(duplicates).containsKey(209));
        check("ScreenLock".equals(HomeHapticFirmwareNames.index(List.of("777_ScreenLock_P_RTP.bin")).get(777)));
        if(args.length>0){var actual=HomeHapticFirmwareNames.index(Files.readAllLines(Path.of(args[0])));
            check("CameraShutter".equals(actual.get(217))&&"cardslide".equals(actual.get(402)));
            System.out.println("Device firmware filename aliases: "+actual.size());}
        System.out.println("Firmware name contracts PASS: current aliases, changed IDs, conflicts and invalid names");
    }
    private static void check(boolean result){if(!result)throw new AssertionError();}
}
