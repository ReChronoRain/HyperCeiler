/* SPDX-License-Identifier: AGPL-3.0-or-later */
import dalvik.system.PathClassLoader;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.HomeWindowVisibilityContract;
public final class PlatformVisibilityDeviceTest {
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Actual system-server classpath required");
        var loader=new PathClassLoader(args[0],ClassLoader.getSystemClassLoader());
        var window=Class.forName("com.android.server.wm.WindowState",false,loader);
        var contract=HomeWindowVisibilityContract.resolve(window);
        if(contract.edges().isEmpty())throw new AssertionError("No actual platform edges");
        System.out.println("Actual predicate "+contract.visible().toGenericString());
        for(var edge:contract.edges())System.out.println("Actual edge "+edge.toGenericString());
        System.out.println("PASS production visibility resolver against this device's uninitialized real WindowState class, no test substitutes or system injection");
    }
}
