package android.os;

/** Host stub: both query workers only ask whether the panel is interactive. */
public class PowerManager {
    /** Fixtures drive the cadence; the default reproduces the pre-existing behaviour. */
    public static boolean interactive = true;

    public boolean isInteractive() {
        return interactive;
    }
}
