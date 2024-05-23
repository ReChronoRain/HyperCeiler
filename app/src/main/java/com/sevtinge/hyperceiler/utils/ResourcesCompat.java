package com.sevtinge.hyperceiler.utils;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.Discouraged;

public class ResourcesCompat {

    public static final String TYPE_LAYOUT = "layout";
    public static final String TYPE_XML = "xml";


    public static int getXml(Context context, String name) {
        return getIdentifier(context, name, TYPE_XML);
    }

    public static int getIdentifier(Context context, String name, String defType) {
        Resources res = context.getResources();
        String defPackage = context.getPackageName();
        return getIdentifier(res, name, defType, defPackage);
    }

    /**
     * Return a resource identifier for the given resource name.  A fully
     * qualified resource name is of the form "package:type/entry".  The first
     * two components (package and type) are optional if defType and
     * defPackage, respectively, are specified here.
     *
     * <p>Note: use of this function is discouraged.  It is much more
     * efficient to retrieve resources by identifier than by name.
     *
     * @param name The name of the desired resource.
     * @param defType Optional default resource type to find, if "type/" is
     *                not included in the name.  Can be null to require an
     *                explicit type.
     * @param defPackage Optional default package to find, if "package:" is
     *                   not included in the name.  Can be null to require an
     *                   explicit package.
     *
     * @return int The associated resource identifier.  Returns 0 if no such
     *         resource was found.  (0 is not a valid resource ID.)
     */
    @Discouraged(message = "Use of this function is discouraged because resource reflection makes "
            + "it harder to perform build optimizations and compile-time "
            + "verification of code. It is much more efficient to retrieve "
            + "resources by identifier (e.g. `R.foo.bar`) than by name (e.g. "
            + "`getIdentifier(\"bar\", \"foo\", null)`).")
    public static int getIdentifier(Resources res, String name, String defType, String defPackage) {
        return res.getIdentifier(name, defType, defPackage);
    }
}
