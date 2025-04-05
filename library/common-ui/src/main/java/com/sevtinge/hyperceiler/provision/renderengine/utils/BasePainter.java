package com.sevtinge.hyperceiler.provision.renderengine.utils;

import com.sevtinge.hyperceiler.R;

import miuix.mgl.Primitive;
import miuix.mgl.RenderMaterial;
import miuix.mgl.Shader;

public class BasePainter {
    boolean autoInit;
    protected int compId;
    protected int fragId;
    protected MaterialRepo.MKey key;
    protected RenderMaterial material;
    protected Primitive primitive;
    protected RenderContext renderContext;
    protected Shader shader;
    protected int vertId;

    protected int getCompId() {
        return 0;
    }

    protected int getFragId() {
        return 0;
    }

    public void destroy(boolean z) {
        RenderMaterial renderMaterial = this.material;
        if (renderMaterial != null) {
            renderMaterial.destroy(z);
            this.material = null;
        }
    }

    public static class Builder {
        protected int vertId = -1;
        protected int fragId = -1;
        protected int compId = -1;
        RenderMaterial.BlendFuncFactor blendSRC = RenderMaterial.BlendFuncFactor.SRC_ALPHA;
        RenderMaterial.BlendFuncFactor blendDST = RenderMaterial.BlendFuncFactor.ONE_MINUS_SRC_ALPHA;

        public static Builder create() {
            return new Builder();
        }

        public Builder vertId(int i) {
            this.vertId = i;
            return this;
        }

        public Builder fragId(int i) {
            this.fragId = i;
            return this;
        }

        public Builder blendFunc(RenderMaterial.BlendFuncFactor blendFuncFactor, RenderMaterial.BlendFuncFactor blendFuncFactor2) {
            this.blendSRC = blendFuncFactor;
            this.blendDST = blendFuncFactor2;
            return this;
        }

        public Builder compId(int i) {
            this.compId = i;
            return this;
        }

        public BasePainter build(RenderContext renderContext) {
            return new BasePainter(renderContext, this);
        }
    }

    public BasePainter(RenderContext renderContext) {
        this.autoInit = true;
        this.renderContext = renderContext;
        init();
    }

    public BasePainter(RenderContext renderContext, boolean z) {
        this.autoInit = z;
        this.renderContext = renderContext;
        init();
    }

    public BasePainter(RenderContext renderContext, Builder builder) {
        this.autoInit = true;
        this.renderContext = renderContext;
        int i = builder.vertId;
        if (i != -1) {
            this.vertId = i;
        }
        int i2 = builder.fragId;
        if (i2 != -1) {
            this.fragId = i2;
        }
        int i3 = builder.compId;
        if (i3 != -1) {
            this.compId = i3;
        }
        this.key = new MaterialRepo.MKey(this.vertId, this.fragId);
        this.shader = renderContext.getMaterialRepo().getShader(this.key);
        this.material = renderContext.getMaterialRepo().getRenderMaterial(this.key);
        this.primitive = renderContext.getMaterialRepo().getDefaultPrimitive();
        this.material.setBlendFunc(builder.blendSRC, builder.blendDST);
    }

    private void init() {
        if (this.autoInit) {
            initMKey();
            this.shader = this.renderContext.getMaterialRepo().getShader(this.key);
            this.material = this.renderContext.getMaterialRepo().getRenderMaterial(this.key);
            this.primitive = initPrimitive();
        }
    }

    protected void initMKey() {
        this.key = new MaterialRepo.MKey(getVertId(), getFragId());
    }

    protected int getVertId() {
        return R.raw.vertex_shader;
    }

    protected Primitive initPrimitive() {
        return this.renderContext.getMaterialRepo().getDefaultPrimitive();
    }

    public RenderMaterial getMaterial() {
        return this.material;
    }

    public Primitive getPrimitive() {
        return this.primitive;
    }
}
