package com.sevtinge.provision.renderengine.painter;

import android.graphics.Color;
import android.util.Log;

import com.sevtinge.provision.R;
import com.sevtinge.provision.renderengine.utils.BasePainter;
import com.sevtinge.provision.renderengine.utils.RenderContext;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;

import miuix.mgl.Material;
import miuix.mgl.MaterialEnums;
import miuix.mgl.Primitive;
import miuix.mgl.RenderMaterial;

public class MeshPainter extends BasePainter {

    float[][] HM;
    float[][] HM_T;
    float animSpeed;
    float[][][][] cColor;
    float[][][][] cColor1;
    float[][][] cOffset;
    float[][][] cOffset1;
    float[][][] cPos;
    float[][][] cPos1;
    int col;
    float colorChangeDuration;
    float[][][] currentColor;
    private float globalStartTime;
    GridPoint[][] gridPoints;
    int[] index;
    float lightProgress;
    private ByteBuffer mColorBuffer;
    private ByteBuffer mPatchIndexBuffer;
    private ByteBuffer mPositionBuffer;
    int meshCount;
    float offsetRange;
    Patch[][] patches;
    int row;
    float[] speeds;
    float time;
    float[] uPatchB;
    float[] uPatchG;
    float[] uPatchR;
    float[] uPatchX;
    float[] uPatchY;

    public float[][] getColorArrays(String color, String color2, String color3) {
        return new float[][] {hexToRgb(color), hexToRgb(color2), hexToRgb(color3)};
    }

    public MeshPainter(final RenderContext renderContext) {
        super(renderContext);
        cPos = new float[][][] {
                {{ -0.14f, -0.03f }, { 0.6f, -0.04f }, { 1.08f, -0.05f }},
                {{ -0.31f, 0.34f }, { 0.39f, 0.42f }, { 1.21f, 0.26f }},
                {{ -0.13f, 0.66f }, { 0.65f, 0.85f }, { 1.3f, 0.66f }},
                {{ -0.52f, 1.06f }, { 0.4f, 1.06f }, { 1.24f, 1.05f }}
        };
        cPos1 = new float[][][] {
                {{ -0.13288257f, -0.10790368f }, { 0.4611737f, -0.1700916f }, { 1.0275089f, -0.091076486f }},
                { { -0.42142856f, 0.33333334f }, { 0.18926783f, 0.2973898f }, { 1.1269336f, 0.30594763f } },
                { { -0.14455517f, 0.77676916f }, { 0.4782918f, 0.8555784f }, { 1.0663207f, 0.82287407f } },
                { { -0.52f, 1.06f }, { 0.26379257f, 1.1222811f }, { 1.0f, 1.3062867f } }
        };
        float[][] f1 = getColorArrays("#D6DDFF","#B8C0FF", "#FFE9E5");
        float[][] f12 = getColorArrays("#FFBDDD","#FFE8E0", "#E6DBFF");

        float[][] f2 = getColorArrays("#FFD9CC","#FFB8C2", "#FFF1E5");
        float[][] f22 = getColorArrays("#FFD9CC", "#A3B2FF","#FFF1E5");
        float[][] f23 = getColorArrays("#FFD9CC", "#D1B8FF","#FFF1E5");

        float[][] f3 = getColorArrays("#CCDDFF", "#FFF8EB", "#FFDFD1");
        float[][] f32 = getColorArrays("#FFECE5", "#FFBAB3", "#FFDFD1");

        float[][] f4 = getColorArrays("#FEE9DA", "#FFEADB","#FFF2EB");

        float[][][] ff = {f1, f2, f3, f4};
        float[][][] ff2 = {f1, f22, f3, f4};
        float[][][] ff3 = {f12, f23, f32, f4};

        cColor = new float[][][][] {ff, ff2, ff3};
        this.cColor1 = new float[][][][] { { { { 0.8392157f, 0.8666667f, 1.0f }, { 0.72156864f, 0.7529412f, 1.0f }, { 1.0f, 0.9137255f, 0.8980392f } }, { { 1.0f, 0.8509804f, 0.8f }, { 1.0f, 0.72156864f, 0.7607843f }, { 1.0f, 0.94509804f, 0.8980392f } }, { { 0.8f, 0.8666667f, 1.0f }, { 1.0f, 0.972549f, 0.92156863f }, { 1.0f, 0.8745098f, 0.81960785f } }, { { 0.99607843f, 0.9137255f, 0.85490197f }, { 1.0f, 0.91764706f, 0.85882354f }, { 1.0f, 0.9490196f, 0.92156863f } } }, { { { 1.0f, 0.94509804f, 0.7764706f }, { 0.72156864f, 0.7529412f, 1.0f }, { 1.0f, 0.9137255f, 0.8980392f } }, { { 1.0f, 0.8509804f, 0.8f }, { 0.6392157f, 0.69803923f, 1.0f }, { 1.0f, 0.94509804f, 0.8980392f } }, { { 0.8f, 0.8666667f, 1.0f }, { 1.0f, 0.972549f, 0.92156863f }, { 1.0f, 0.8745098f, 0.81960785f } }, { { 0.99607843f, 0.9137255f, 0.85490197f }, { 1.0f, 0.91764706f, 0.85882354f }, { 1.0f, 0.9490196f, 0.92156863f } } }, { { { 1.0f, 0.7411765f, 0.8666667f }, { 1.0f, 0.9098039f, 0.8784314f }, { 0.9019608f, 0.85882354f, 1.0f } }, { { 1.0f, 0.8509804f, 0.8f }, { 0.81960785f, 0.72156864f, 1.0f }, { 1.0f, 0.94509804f, 0.8980392f } }, { { 1.0f, 0.9254902f, 0.8980392f }, { 1.0f, 0.7294118f, 0.7019608f }, { 1.0f, 0.8745098f, 0.81960785f } }, { { 0.99607843f, 0.9137255f, 0.85490197f }, { 1.0f, 0.91764706f, 0.85882354f }, { 1.0f, 0.9490196f, 0.92156863f } } } };
        this.cOffset = new float[][][] { { { 0.0f, 0.0f }, { 0.1f, 0.0f }, { 0.0f, 0.0f } }, { { 0.0f, 0.1f }, { 0.55f, 0.22f }, { 0.0f, 0.1f } }, { { 0.04f, 0.19f }, { 0.2f, 0.12f }, { 0.0f, 0.1f } }, { { 0.0f, 0.0f }, { 0.1f, 0.0f }, { 0.0f, 0.0f } } };
        this.cOffset1 = new float[][][] { { { 0.0f, 0.0f }, { 0.1f, 0.0f }, { 0.0f, 0.0f } }, { { 0.0f, 0.1f }, { 0.15f, 0.22f }, { 0.0f, 0.1f } }, { { 0.04f, 0.19f }, { 0.2f, 0.12f }, { 0.0f, 0.1f } }, { { 0.0f, 0.0f }, { 0.1f, 0.0f }, { 0.0f, 0.0f } } };
        this.speeds = new float[] { 1.0f, 1.0f, 1.0f, 1.0f, 0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };
        this.currentColor = new float[4][3][3];
        this.row = 4;
        this.col = 3;
        this.meshCount = 20;
        this.offsetRange = 0.6f;
        this.time = 0.0f;
        this.lightProgress = 1.0f;
        this.colorChangeDuration = 0.8f;
        this.animSpeed = 0.65f;
        this.globalStartTime = 0.0f;
        this.gridPoints = new GridPoint[4][3];
        this.patches = new Patch[4][3];
        this.HM = new float[][] { { 2.0f, -2.0f, 1.0f, 1.0f }, { -3.0f, 3.0f, -2.0f, -1.0f }, { 0.0f, 0.0f, 1.0f, 0.0f }, { 1.0f, 0.0f, 0.0f, 0.0f } };
        this.HM_T = new float[][] { { 2.0f, -3.0f, 0.0f, 1.0f }, { -2.0f, 3.0f, 0.0f, 0.0f }, { 1.0f, -2.0f, 1.0f, 0.0f }, { 1.0f, -1.0f, 0.0f, 0.0f } };
        this.uPatchX = new float[96];
        this.uPatchY = new float[96];
        this.uPatchR = new float[96];
        this.uPatchG = new float[96];
        this.uPatchB = new float[96];
        this.initParam();
        this.generateIndex();
        this.globalStartTime = (float)System.nanoTime();
        final Primitive.Builder create = Primitive.Builder.create(2400);
        final Primitive.VertexElementType float1 = Primitive.VertexElementType.FLOAT;
        final Primitive.ComponentSize three = Primitive.ComponentSize.THREE;
        super.primitive = create.vertexAttributeEmpty(0, float1, three, false).vertexAttributeEmpty(1, float1, three, false).indices(this.index).primitiveType(Primitive.PrimitiveType.TRIANGLES).build(renderContext.getMglContext(), Primitive.Builder.Mod.EACH_ONE);
    }

    public static float cosInRange(float n, final float n2, final float n3, final float n4, final float n5) {
        final float n6 = (n2 - n) / 2.0f;
        n = (n2 + n) / 2.0f;
        return n + n6 * (float)Math.cos(n5 * n4 + n3);
    }

    private static float[] hexToRgb(String colorString) {
        return new float[] {
                Integer.parseInt(colorString.substring(1, 3), 16) / 255.0f,
                Integer.parseInt(colorString.substring(3, 5), 16) / 255.0f,
                Integer.parseInt(colorString.substring(5, 7), 16) / 255.0f
        };
    }

    private static float lerp(final float n, final float n2, final float n3) {
        return n + n3 * (n2 - n);
    }

    public static float sinInRange(float n, final float n2, final float n3, final float n4, final float n5) {
        final float n6 = (n2 - n) / 2.0f;
        n = (n2 + n) / 2.0f;
        return n + n6 * (float)Math.sin(n5 * n4 + n3);
    }

    public void computeColor(float n) {
        n /= this.colorChangeDuration;
        final int n2 = (int)Math.floor(n) % 3;
        for (int i = 0; i < this.row; ++i) {
            for (int j = 0; j < this.col; ++j) {
                for (int k = 0; k < 3; ++k) {
                    final float[] array = this.currentColor[i][j];
                    final float[][][][] cColor1 = this.cColor1;
                    array[k] = lerp(cColor1[n2][i][j][k], cColor1[(n2 + 1) % 3][i][j][k], n % 1.0f);
                }
            }
        }
    }

    public void computeGrid(final float n) {
        final float n2 = 1.0f / (this.row - 1.0f);
        final float n3 = 1.0f / (this.col - 1.0f);
        for (int i = 0; i < this.row; ++i) {
            int n4 = 0;
            while (true) {
                final int col = this.col;
                if (n4 >= col) {
                    break;
                }
                final int n5 = col * i + n4;
                final float[] array = this.cPos1[i][n4];
                final float[] array2 = this.currentColor[i][n4];
                final float[] array3 = this.cOffset1[i][n4];
                final float n6 = array[0];
                final float n7 = array3[0];
                final float n8 = (float)n5;
                this.gridPoints[i][n4].set(n6 + n7 * cosInRange(-1.0f, 1.0f, n8, 1.0f, this.speeds[n5] * n) * this.lightProgress, array3[1] * sinInRange(-1.0f, 1.0f, n8, 1.0f, this.speeds[n5] * n) * this.lightProgress + array[1], array2[0], array2[1], array2[2], n2, n3);
                ++n4;
            }
        }
    }

    public void draw() {
        final float n = (System.nanoTime() - this.globalStartTime) / 1.0E9f;
        System.nanoTime();
        this.computeColor(this.animSpeed * n);
        this.computeGrid(n * this.animSpeed);
        this.getPatches();
        this.flattenArray(this.patches);
        Log.i("MeshPainter", " in draw");
        final RenderMaterial material = super.material;
        final MaterialEnums.UniformFloatType mat4 = MaterialEnums.UniformFloatType.MAT4;
        ((Material)material).setFloatArray("uPatchX[0]", mat4, this.uPatchX);
        ((Material)super.material).setFloatArray("uPatchY[0]", mat4, this.uPatchY);
        ((Material)super.material).setFloatArray("uPatchR[0]", mat4, this.uPatchR);
        ((Material)super.material).setFloatArray("uPatchG[0]", mat4, this.uPatchG);
        ((Material)super.material).setFloatArray("uPatchB[0]", mat4, this.uPatchB);
        ((Material)super.material).active();
        super.primitive.draw(1);
    }

    public void flattenArray(final Patch[][] array) {
        int i = 0;
        int n = 0;
        while (i < this.row - 1) {
            for (int j = 0; j < this.col - 1; ++j) {
                for (int k = 0; k < 4; ++k) {
                    for (int l = 0; l < 4; ++l) {
                        this.uPatchX[n] = array[i][j].x[k][l];
                        ++n;
                    }
                }
            }
            ++i;
        }
        int n2 = 0;
        int n3 = 0;
        while (n2 < this.row - 1) {
            for (int n4 = 0; n4 < this.col - 1; ++n4) {
                for (int n5 = 0; n5 < 4; ++n5) {
                    for (int n6 = 0; n6 < 4; ++n6) {
                        this.uPatchY[n3] = array[n2][n4].y[n5][n6];
                        ++n3;
                    }
                }
            }
            ++n2;
        }
        int n7 = 0;
        int n8 = 0;
        while (n7 < this.row - 1) {
            for (int n9 = 0; n9 < this.col - 1; ++n9) {
                for (int n10 = 0; n10 < 4; ++n10) {
                    for (int n11 = 0; n11 < 4; ++n11) {
                        this.uPatchR[n8] = array[n7][n9].r[n10][n11];
                        ++n8;
                    }
                }
            }
            ++n7;
        }
        int n12 = 0;
        int n13 = 0;
        while (n12 < this.row - 1) {
            for (int n14 = 0; n14 < this.col - 1; ++n14) {
                for (int n15 = 0; n15 < 4; ++n15) {
                    for (int n16 = 0; n16 < 4; ++n16) {
                        this.uPatchG[n13] = array[n12][n14].g[n15][n16];
                        ++n13;
                    }
                }
            }
            ++n12;
        }
        int n17 = 0;
        int n18 = 0;
        while (n17 < this.row - 1) {
            for (int n19 = 0; n19 < this.col - 1; ++n19) {
                for (int n20 = 0; n20 < 4; ++n20) {
                    for (int n21 = 0; n21 < 4; ++n21) {
                        this.uPatchB[n18] = array[n17][n19].b[n20][n21];
                        ++n18;
                    }
                }
            }
            ++n17;
        }
    }

    public void generateIndex() {
        final int row = this.row;
        final int col = this.col;
        final int meshCount = this.meshCount;
        this.index = new int[(row - 1) * (col - 1) * (meshCount - 1) * (meshCount - 1) * 6];
        int i = 0;
        int n = 0;
        while (i < this.row - 1) {
            int n2 = 0;
            while (true) {
                final int col2 = this.col;
                if (n2 >= col2 - 1) {
                    break;
                }
                for (int j = 0; j < this.meshCount - 1; ++j) {
                    int n3 = 0;
                    while (true) {
                        final int meshCount2 = this.meshCount;
                        if (n3 >= meshCount2 - 1) {
                            break;
                        }
                        final int n4 = ((col2 - 1) * i + n2) * meshCount2 * meshCount2 + j * meshCount2 + n3;
                        final int[] index = this.index;
                        index[n + 1] = (index[n] = n4) + meshCount2;
                        index[n + 3] = (index[n + 2] = n4 + 1);
                        index[n + 4] = n4 + meshCount2;
                        final int n5 = n + 6;
                        index[n + 5] = n4 + meshCount2 + 1;
                        ++n3;
                        n = n5;
                    }
                }
                ++n2;
            }
            ++i;
        }
    }

    protected int getFragId() {
        return R.raw.mesh_fragment_shader;
    }

    void getPatchAttribute(final int n, final int n2) {
        final GridPoint[][] gridPoints = this.gridPoints;
        final GridPoint[] array = gridPoints[n];
        final GridPoint meshPainter$GridPoint = array[n2];
        final GridPoint[] array2 = gridPoints[n + 1];
        final GridPoint meshPainter$GridPoint2 = array2[n2];
        final int n3 = n2 + 1;
        final GridPoint meshPainter$GridPoint3 = array[n3];
        final GridPoint meshPainter$GridPoint4 = array2[n3];
        final Patch meshPainter$Patch = this.patches[n][n2];
        meshPainter$Patch.x = new float[][] { { meshPainter$GridPoint.x, meshPainter$GridPoint2.x, 0.0f, 0.0f }, { meshPainter$GridPoint3.x, meshPainter$GridPoint4.x, 0.0f, 0.0f }, { meshPainter$GridPoint.dy, meshPainter$GridPoint2.dy, 0.0f, 0.0f }, { meshPainter$GridPoint3.dy, meshPainter$GridPoint4.dy, 0.0f, 0.0f } };
        meshPainter$Patch.y = new float[][] { { meshPainter$GridPoint.y, meshPainter$GridPoint2.y, meshPainter$GridPoint.dx, meshPainter$GridPoint2.dx }, { meshPainter$GridPoint3.y, meshPainter$GridPoint4.y, meshPainter$GridPoint3.dx, meshPainter$GridPoint4.dx }, { 0.0f, 0.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f, 0.0f } };
        meshPainter$Patch.r = new float[][] { { meshPainter$GridPoint.r, meshPainter$GridPoint2.r, 0.0f, 0.0f }, { meshPainter$GridPoint3.r, meshPainter$GridPoint4.r, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f, 0.0f } };
        meshPainter$Patch.g = new float[][] { { meshPainter$GridPoint.g, meshPainter$GridPoint2.g, 0.0f, 0.0f }, { meshPainter$GridPoint3.g, meshPainter$GridPoint4.g, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f, 0.0f } };
        meshPainter$Patch.b = new float[][] { { meshPainter$GridPoint.b, meshPainter$GridPoint2.b, 0.0f, 0.0f }, { meshPainter$GridPoint3.b, meshPainter$GridPoint4.b, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f, 0.0f } };
    }

    public void getPatches() {
        for (int i = 0; i < this.row - 1; ++i) {
            for (int j = 0; j < this.col - 1; ++j) {
                this.getPatchAttribute(i, j);
            }
        }
    }

    protected int getVertId() {
        return R.raw.mesh_vertex_shader;
    }

    public void initParam() {
        for (int i = 0; i < this.row; ++i) {
            for (int j = 0; j < this.col; ++j) {
                this.gridPoints[i][j] = new GridPoint();
                this.patches[i][j] = new Patch();
            }
        }
    }

    public class GridPoint {
        float b;
        float dx;
        float dy;
        float g;
        float r;
        float x;
        float y;

        public void set(final float x, final float y, final float r, final float g, final float b, final float dx, final float dy) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.g = g;
            this.b = b;
            this.dx = dx;
            this.dy = dy;
        }
    }

    public class Patch {
        float[][] b;
        float[][] g;
        float[][] r;
        float[][] x;
        float[][] y;
    }

}
