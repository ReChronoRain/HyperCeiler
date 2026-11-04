/* SPDX-License-Identifier: AGPL-3.0-or-later */
package android.window;

import android.app.ActivityManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.SurfaceControl;

import java.util.List;

/** Compile-only hidden API stub. Runtime uses the framework implementation. */
@SuppressWarnings("unused")
public final class TransitionInfo {
    private TransitionInfo() { }

    public int getDebugId() { throw new UnsupportedOperationException("stub"); }
    public int getType() { throw new UnsupportedOperationException("stub"); }
    public int getFlags() { throw new UnsupportedOperationException("stub"); }
    public int getRootCount() { throw new UnsupportedOperationException("stub"); }
    public Root getRoot(int index) { throw new UnsupportedOperationException("stub"); }
    public List<Change> getChanges() { throw new UnsupportedOperationException("stub"); }

    public static final class Root {
        private Root() { }
        public int getDisplayId() { throw new UnsupportedOperationException("stub"); }
        public Point getOffset() { throw new UnsupportedOperationException("stub"); }
        public SurfaceControl getLeash() { throw new UnsupportedOperationException("stub"); }
    }

    public static final class Change {
        private Change() { }
        public ActivityManager.RunningTaskInfo getTaskInfo() { throw new UnsupportedOperationException("stub"); }
        public SurfaceControl getLeash() { throw new UnsupportedOperationException("stub"); }
        public boolean hasFlags(int flags) { throw new UnsupportedOperationException("stub"); }
        public int getMode() { throw new UnsupportedOperationException("stub"); }
        public int getEndDisplayId() { throw new UnsupportedOperationException("stub"); }
        public IBinder getParent() { throw new UnsupportedOperationException("stub"); }
        public IBinder getLastParent() { throw new UnsupportedOperationException("stub"); }
        public Rect getStartAbsBounds() { throw new UnsupportedOperationException("stub"); }
        public Rect getEndAbsBounds() { throw new UnsupportedOperationException("stub"); }
    }
}
