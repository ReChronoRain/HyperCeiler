package android.content;

public class IntentFilter {
    private final String action;

    public IntentFilter(String action) {
        this.action = action;
    }

    public String getAction(int index) {
        return action;
    }
}
