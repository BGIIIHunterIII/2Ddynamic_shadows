#version 330 core

in vec2 position;
out vec2 UV;

void main(void) {
	gl_Position = vec4(position, 0.0, 1.0);
	vec2 tmp = (position * 0.5 + vec2(0.5, 0.5));
	UV = vec2(tmp.s,1-tmp.t);
}