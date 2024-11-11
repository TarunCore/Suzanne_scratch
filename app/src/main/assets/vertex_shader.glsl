#version 300 es
layout(location = 0) in vec3 vPosition;
layout(location = 1) in vec4 vColor;
uniform mat4 uMVPMatrix;
out vec4 fragColor;

void main() {
    gl_Position = uMVPMatrix * vec4(vPosition, 1.0);
    fragColor = vColor;
}