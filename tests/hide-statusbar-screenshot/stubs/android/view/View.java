package android.view;

import android.content.Context;

public class View {
    public static final int VISIBLE = 0x00000000;
    public static final int INVISIBLE = 0x00000004;
    public static final int GONE = 0x00000008;

    private final Context context;
    private final ViewRootImpl viewRootImpl = new ViewRootImpl();
    private int visibility = VISIBLE;
    private int visibilityWrites;

    public View(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public int getVisibility() {
        return visibility;
    }

    public void setVisibility(int value) {
        visibility = value;
        visibilityWrites++;
    }

    /** 规则通过反射调用，与真机 View 上被隐藏的同名方法对应。 */
    public ViewRootImpl getViewRootImpl() {
        return viewRootImpl;
    }

    public int getVisibilityWrites() {
        return visibilityWrites;
    }
}
