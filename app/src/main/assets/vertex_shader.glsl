#version 300 es
uniform mat4 uMVPMatrix;
in vec3 vPosition;

void main() {
    gl_Position = uMVPMatrix * vec4(vPosition, 1.0);
}