package android.content;

import java.util.HashMap;
import java.util.Map;

public class Intent {
    private final String action;
    private final Map<String, Boolean> booleanExtras = new HashMap<>();

    public Intent(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void putExtra(String name, boolean value) {
        booleanExtras.put(name, value);
    }

    public boolean getBooleanExtra(String name, boolean defaultValue) {
        Boolean value = booleanExtras.get(name);
        return value != null ? value : defaultValue;
    }
}
