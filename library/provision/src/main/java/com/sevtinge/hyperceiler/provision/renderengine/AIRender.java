/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.provision.renderengine;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Looper;

import com.sevtinge.hyperceiler.provision.renderengine.painter.MeshPainter;
import com.sevtinge.hyperceiler.provision.renderengine.utils.RenderContext;
import com.sevtinge.hyperceiler.provision.renderengine.utils.PublicParam;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AIRender implements GLSurfaceView.Renderer {

    private RenderContext mRenderContext;
    MeshPainter meshPainter;

    public AIRender(Context context) {
        PublicParam.setContext(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Looper.prepare();
        mRenderContext = new RenderContext();
        meshPainter = new MeshPainter(mRenderContext);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        PublicParam.setResolution(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        meshPainter.draw();

    }
}
