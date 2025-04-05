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
