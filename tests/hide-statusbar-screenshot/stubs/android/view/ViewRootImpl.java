package android.view;

public class ViewRootImpl {
    private SurfaceControl surface = new SurfaceControl();

    public SurfaceControl getSurfaceControl() {
        if (!surface.isValid()) {
            surface = new SurfaceControl();
        }
        return surface;
    }
}
