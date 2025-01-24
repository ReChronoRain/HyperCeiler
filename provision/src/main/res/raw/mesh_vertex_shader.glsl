#version 300 es

uniform mat4 uPatchX[6];
uniform mat4 uPatchY[6];
uniform mat4 uPatchR[6];
uniform mat4 uPatchG[6];
uniform mat4 uPatchB[6];

out vec3 vColor;

int meshCount = 20;
int meshCount2 = 400;
float f_meshCount_minus_one = float(19);
int row = 4;
int col = 3;
float f_row_minus_one = float(3);
float f_col_minus_one = float(2);
vec3 calcPosition;
vec3 calcColor;

mat4 HM = mat4(
    2.0, -2.0, 1.0, 1.0,
    -3.0, 3.0, -2.0, -1.0,
    0.0, 0.0, 1.0, 0.0,
    1.0, 0.0, 0.0, 0.0);

mat4 HM_T = mat4(
    2.0, -3.0, 0.0, 1.0,
    -2.0, 3.0, 0.0, 0.0,
    1.0, -2.0, 1.0, 0.0,
    1.0, -1.0, 0.0, 0.0);

float getPatchPoint(mat4 patchAttri, vec4 HM_mul_uVec, vec4 vVec) {
    return dot(HM_T * patchAttri * HM_mul_uVec, vVec);
}

void main() {
    int vertexIndex = gl_VertexID;
    int patchRow = vertexIndex / (meshCount2 * 2);
    int patchCol = int((vertexIndex - meshCount2 * 2 * patchRow) >= meshCount2);
    int patchIndex = patchRow * 2 + patchCol;
    int vertexIndexInPatch = vertexIndex - meshCount2 * patchIndex;
    int m = vertexIndexInPatch / meshCount;
    int n = vertexIndexInPatch % meshCount;
    float u = float(m) / f_meshCount_minus_one;
    float v = float(n) / f_meshCount_minus_one;
    vec4 uVec = vec4(u * u * u, u * u, u, 1.0);
    vec4 vVec = vec4(v * v * v, v * v, v, 1.0);
    vec4 HM_mul_uVec = HM * uVec;
    float x = getPatchPoint(uPatchX[patchIndex], HM_mul_uVec, vVec);
    float y = getPatchPoint(uPatchY[patchIndex], HM_mul_uVec, vVec);
    float r = getPatchPoint(uPatchR[patchIndex], HM_mul_uVec, vVec);
    float g = getPatchPoint(uPatchG[patchIndex], HM_mul_uVec, vVec);
    float b = getPatchPoint(uPatchB[patchIndex], HM_mul_uVec, vVec);
    float t = (float(patchCol) * f_meshCount_minus_one + float(m)) / (f_col_minus_one * f_meshCount_minus_one);
    float s = (float(patchRow) * f_meshCount_minus_one + float(n)) / (f_row_minus_one * f_meshCount_minus_one);
    float z = 1.0f - ((s - 0.5f) * (s - 0.5f) + (t - 0.5f) * (t - 0.5f));
    x = mix(-1.0f, 1.0f, x);
    y = mix(-1.0f, 1.0f, y);
    z = mix(-1.0f, 1.0f, z);

    vColor = vec3(r, g, b);
    gl_Position = vec4(x, y, z, 1.0);
}