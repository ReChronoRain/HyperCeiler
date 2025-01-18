#version 300 es
precision highp float;

in vec3 vColor;
out vec4 oFragColor;

void main() {
    oFragColor.a = 1.;
    oFragColor.rgb = vColor;
}