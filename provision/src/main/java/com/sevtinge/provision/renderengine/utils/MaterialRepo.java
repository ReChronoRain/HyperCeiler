package com.sevtinge.provision.renderengine.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import miuix.mgl.MglContext;
import miuix.mgl.PngParser;
import miuix.mgl.Primitive;
import miuix.mgl.RenderMaterial;
import miuix.mgl.Shader;
import miuix.mgl.Texture;
import miuix.mgl.Texture2D;
import miuix.mgl.WebpParser;
import miuix.mgl.ZstcParser;

public class MaterialRepo {
    private MglContext mglContext;
    private Primitive primitive;
    private Primitive primitiveCity;
    private Primitive primitiveReverseUV;
    private float primitiveSourceAspect;
    private Map<MKey, Shader> shaderMap = new HashMap();
    private Map<Integer, Texture2D> texture2DMap = new HashMap();
    private static final float[] VERTEX_POS = {-1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f};
    private static final float[] VERTEX_UV = {0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f};
    private static final float[] VERTEX_UV_RE = {0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};
    private static final byte[] INDEX = {0, 1, 2, 3, 0, 2};

    public void initContext(MglContext mglContext) {
        this.mglContext = mglContext;
    }

    public Shader getShader(MKey mKey) {
        Shader shader = this.shaderMap.get(mKey);
        if (shader != null) {
            return shader;
        }
        int i = AnonymousClass1.$SwitchMap$miuix$mgl$Shader$ShaderType[mKey.type.ordinal()];
        if (i == 1) {
            shader = Shader.Builder.create().type(mKey.type).vertexSource(ShaderTextReader.readTextFileFromResource(mKey.vertId)).fragmentSource(ShaderTextReader.readTextFileFromResource(mKey.fragId)).build(this.mglContext);
        } else if (i == 2) {
            shader = Shader.Builder.create().type(mKey.type).computeSource(ShaderTextReader.readTextFileFromResource(mKey.compId)).build(this.mglContext);
        }
        this.shaderMap.put(mKey, shader);
        return shader;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$miuix$mgl$Shader$ShaderType;

        static {
            int[] iArr = new int[Shader.ShaderType.values().length];
            $SwitchMap$miuix$mgl$Shader$ShaderType = iArr;
            try {
                iArr[Shader.ShaderType.VERTEX_FRAGMENT.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$miuix$mgl$Shader$ShaderType[Shader.ShaderType.COMPUTE.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
        }
    }

    public RenderMaterial getRenderMaterial(MKey mKey) {
        return RenderMaterial.create(getShader(mKey));
    }

    public Texture2D getTexture2DPng(int i) {
        return getTexture2DPng(i, 0);
    }

    public Texture2D getTexture2DPng(int i, int i2) {
        Texture2D texture2D = this.texture2DMap.get(Integer.valueOf(i));
        if (texture2D != null) {
            return texture2D;
        }
        PngParser create = PngParser.create(i2);
        create.parseFromRes(i, PublicParam.getContext().getResources());
        Texture2D build = Texture2D.Builder.create().width(create.getWidth()).height(create.getHeight()).format(create.getTextureFormat()).build(this.mglContext);
        build.setDataFromParser(0, create);
        create.destroy();
        build.setWrapMod(Texture.TextureWrapMod.CLAMP_TO_EDGE);
        this.texture2DMap.put(Integer.valueOf(i), build);
        return build;
    }

    public Texture2D getTexture2DWebp(int i) {
        Texture2D texture2D = this.texture2DMap.get(Integer.valueOf(i));
        if (texture2D != null) {
            return texture2D;
        }
        WebpParser create = WebpParser.create();
        create.parseFromRes(i, PublicParam.getContext().getResources());
        Texture2D build = Texture2D.Builder.create().width(create.getWidth()).height(create.getHeight()).format(create.getTextureFormat()).build(this.mglContext);
        build.setDataFromParser(0, create);
        create.destroy();
        build.setWrapMod(Texture.TextureWrapMod.CLAMP_TO_EDGE);
        this.texture2DMap.put(Integer.valueOf(i), build);
        return build;
    }

    public Texture2D getTexture2DZstc(int i) {
        Texture2D texture2D = this.texture2DMap.get(Integer.valueOf(i));
        if (texture2D != null) {
            return texture2D;
        }
        ZstcParser create = ZstcParser.create();
        create.parseFromRes(i, PublicParam.getContext().getResources());
        Texture2D build = Texture2D.Builder.create().width(create.getWidth()).height(create.getHeight()).format(create.getTextureFormat()).build(this.mglContext);
        build.setDataFromParser(0, create);
        create.destroy();
        build.setWrapMod(Texture.TextureWrapMod.CLAMP_TO_EDGE);
        this.texture2DMap.put(Integer.valueOf(i), build);
        return build;
    }

    public void destroyTexture2D(int i, boolean z) {
        Texture2D texture2D = this.texture2DMap.get(Integer.valueOf(i));
        if (texture2D != null) {
            texture2D.destroy(z);
            this.texture2DMap.remove(Integer.valueOf(i));
        }
    }

    public void destroyTexture2D(int i) {
        destroyTexture2D(i, true);
    }

    public Primitive getDefaultPrimitive() {
        if (this.primitive == null) {
            Primitive.Builder create = Primitive.Builder.create(4);
            float[] fArr = VERTEX_POS;
            Primitive.VertexElementType vertexElementType = Primitive.VertexElementType.FLOAT;
            Primitive.ComponentSize componentSize = Primitive.ComponentSize.TWO;
            this.primitive = create.vertexAttribute(1, fArr, vertexElementType, componentSize, false).vertexAttribute(0, VERTEX_UV, vertexElementType, componentSize, false).indices(INDEX).primitiveType(Primitive.PrimitiveType.TRIANGLES).build(this.mglContext, Primitive.Builder.Mod.ONE);
        }
        return this.primitive;
    }

    public Primitive getDefaultPrimitiveReverseUV() {
        if (this.primitiveReverseUV == null) {
            Primitive.Builder create = Primitive.Builder.create(4);
            float[] fArr = VERTEX_POS;
            Primitive.VertexElementType vertexElementType = Primitive.VertexElementType.FLOAT;
            Primitive.ComponentSize componentSize = Primitive.ComponentSize.TWO;
            this.primitiveReverseUV = create.vertexAttribute(1, fArr, vertexElementType, componentSize, false).vertexAttribute(0, VERTEX_UV_RE, vertexElementType, componentSize, false).indices(INDEX).primitiveType(Primitive.PrimitiveType.TRIANGLES).build(this.mglContext, Primitive.Builder.Mod.ONE);
        }
        return this.primitiveReverseUV;
    }

    public Primitive getCityPrimitive(float f) {
        Primitive primitive = this.primitiveCity;
        if (primitive == null || this.primitiveSourceAspect != f) {
            if (primitive != null) {
                primitive.destroy(true);
                this.primitiveCity = null;
            }
            this.primitiveSourceAspect = f;
            float f2 = (1.0f - (f / 2.2222223f)) * 0.5f;
            float f3 = 1.0f - f2;
            float[] fArr = {0.0f, f2, 1.0f, f2, 1.0f, f3, 0.0f, f3};
            Primitive.Builder create = Primitive.Builder.create(4);
            float[] fArr2 = VERTEX_POS;
            Primitive.VertexElementType vertexElementType = Primitive.VertexElementType.FLOAT;
            Primitive.ComponentSize componentSize = Primitive.ComponentSize.TWO;
            this.primitiveCity = create.vertexAttribute(1, fArr2, vertexElementType, componentSize, false).vertexAttribute(0, fArr, vertexElementType, componentSize, false).indices(INDEX).primitiveType(Primitive.PrimitiveType.TRIANGLES).build(this.mglContext, Primitive.Builder.Mod.ONE);
        }
        return this.primitiveCity;
    }

    public void clear() {
        Primitive primitive = this.primitive;
        if (primitive != null) {
            primitive.destroy(false);
        }
        Primitive primitive2 = this.primitiveReverseUV;
        if (primitive2 != null) {
            primitive2.destroy(false);
        }
        this.primitive = null;
        this.primitiveReverseUV = null;
        this.texture2DMap.forEach(new BiConsumer<Integer, Texture2D>() {
            @Override
            public void accept(Integer integer, Texture2D texture2D) {
                texture2D.destroy(false);

            }
        });
        this.texture2DMap.clear();
        this.shaderMap.forEach(new BiConsumer<MKey, Shader>() {
            @Override
            public void accept(MKey mKey, Shader shader) {
                shader.destroy(false);
            }
        });
        this.shaderMap.clear();
    }

    static class MKey {
        public int compId;
        public int fragId;
        public Shader.ShaderType type;
        public int vertId;

        public MKey(int i, int i2) {
            this.vertId = i;
            this.fragId = i2;
            this.type = Shader.ShaderType.VERTEX_FRAGMENT;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            MKey mKey = (MKey) obj;
            return this.vertId == mKey.vertId && this.fragId == mKey.fragId && this.compId == mKey.compId && Objects.equals(this.type, mKey.type);
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.vertId), Integer.valueOf(this.fragId), Integer.valueOf(this.compId), this.type);
        }

        public MKey(int i) {
            this.compId = i;
            this.type = Shader.ShaderType.COMPUTE;
        }
    }
}
