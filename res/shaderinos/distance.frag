#version 330 core

in vec2 UV;
in vec2 pos;

uniform sampler2D texture;

out vec4 color;

void main(){
    vec4 texColor = texture2D(texture,UV);
    float dist = texColor.a == 0?1:length(pos);
    vec4 distanceRes = vec4(vec3(dist),1);
    color = texColor;
}