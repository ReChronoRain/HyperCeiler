/* SPDX-License-Identifier: AGPL-3.0-or-later */
import com.sevtinge.hyperceiler.libhook.rules.home.dock.HomeDockBackendOS4;
import java.util.ArrayList;
import java.util.function.Consumer;

/** Tests the production admission state; physical WMS retirement is a separate device check. */
public final class BackendCoordinatorTest {
    public static void main(String[] args) {
        HomeDockBackendOS4 backend=new HomeDockBackendOS4();
        Object window=new Object();
        ArrayList<Consumer<Boolean>> commits=new ArrayList<>();
        ArrayList<Boolean> events=new ArrayList<>();
        ArrayList<Object> resumed=new ArrayList<>();
        HomeDockBackendOS4.Legacy driver=new HomeDockBackendOS4.Legacy() {
            public void retire(Object owner,Consumer<Boolean> complete) {
                if(owner!=window)throw new AssertionError("Wrong window");
                commits.add(complete);
            }
            public void resume(Object owner) {resumed.add(owner);}
        };
        if(backend.acquire(window,events::add)!=null)throw new AssertionError("Absent controller admitted native");
        backend.install(driver);
        var first=backend.acquire(window,events::add);
        if(first==null||first.ready()||!backend.suppressed()||backend.window()!=window||!events.isEmpty())
            throw new AssertionError("Admission before retirement commit");
        if(backend.acquire(window,events::add)!=null)throw new AssertionError("Two native owners");
        commits.get(0).accept(true);
        if(!first.ready()||!events.equals(java.util.List.of(true)))throw new AssertionError("Commit not published");
        first.close();first.close();
        if(first.ready()||backend.suppressed()||resumed.size()!=1||!events.equals(java.util.List.of(true,false)))
            throw new AssertionError("Release failed");
        var cancelled=backend.acquire(window,events::add);cancelled.close();
        var next=backend.acquire(window,events::add);
        commits.get(1).accept(true);
        if(next.ready()||!backend.suppressed())throw new AssertionError("Cancelled retirement admitted next owner");
        commits.get(2).accept(false);
        if(next.ready()||backend.suppressed()||resumed.size()!=3)throw new AssertionError("Failed retirement did not restore legacy");
        var stopped=backend.acquire(window,events::add);backend.uninstall(driver);
        commits.get(3).accept(true);
        if(stopped.ready()||backend.suppressed()||backend.acquire(window,events::add)!=null)
            throw new AssertionError("Removed controller retained ownership");
        System.out.println("PASS production Dock backend admission, deferred commit, cancellation, failure and removal");
    }
}
