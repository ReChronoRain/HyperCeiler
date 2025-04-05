#version 300 es
uniform mat4 uMatrix;

layout (location = 1) in vec3 aPosition;
layout (location = 0) in vec2 aUv;

out vec2 vUv;

void main() {
    vUv = aUv;
    gl_Position = uMatrix * vec4(aPosition, 1.0);
}