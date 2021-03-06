#version 300 es
uniform mat4 uMVPMatrix;
uniform mat4 uMVMatrix;
uniform mat4 uNormalMatrix;
uniform mat4 uShadowProjMatrix;
layout(location = 0) in vec4 aPosition;
layout(location = 1) in vec4 aColor;
layout(location = 2) in vec3 aNormal;
layout(location = 3) in vec2 aTexcoord;
// to pass on
out vec3 vPosition;
out vec4 vColor;
out vec3 vNormal;
out vec2 vTexcoord;
out vec4 vShadowCoord;
void main() {
	vPosition = vec3(uMVMatrix * aPosition);
	vColor = aColor;
	vNormal = vec3(uNormalMatrix * vec4(aNormal, 0.0));
	vTexcoord = aTexcoord;
	vShadowCoord = uShadowProjMatrix * aPosition;
	gl_Position = uMVPMatrix * aPosition;                     
}