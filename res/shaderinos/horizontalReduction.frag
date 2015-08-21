#version 330

in vec2 UV; //tex0

uniform sampler2D inputSampler;//tex1
uniform vec2 targetTextureDimensions;

out vec4 minValue;

void main(){

    vec2 cUV = UV;
    cUV.x*=0.5;

    vec2 color = texture2D(inputSampler, UV).rg;
    vec2 colorR = texture2D(inputSampler,UV + vec2(0.5,0)).rg;
    vec2 result = min(color,colorR);
    minValue = vec4(result,0,1);

}