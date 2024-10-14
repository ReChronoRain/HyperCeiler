package android.os;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(UserHandle.class)
public class UserHandleHidden {
    public static final int USER_ALL = -1;
    public static final int USER_CURRENT = -2;

    public static final UserHandle CURRENT = null /*new UserHandle(USER_CURRENT) */;

    public static UserHandle of(int userId) {
        throw new RuntimeException("Stub!");
    }

}
