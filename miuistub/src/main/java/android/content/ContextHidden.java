package android.content;

import android.os.UserHandle;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(Context.class)
public class ContextHidden {
    public void startActivityAsUser(Intent intent,
                                    UserHandle user) {
        throw new RuntimeException("Stub!");
    }
}
