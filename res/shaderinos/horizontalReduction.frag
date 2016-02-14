#version 330

in vec2 UV; //tex0

uniform sampler2D inputSampler;
uniform vec2 normalizedSourceDimensions;

out vec4 minValue;

void main(){

    vec2 lUV = vec2(UV.s*0.5f,UV.t);

    vec2 color = texture(inputSampler, UV).rg;
    //vec2 colorR = texture(inputSampler,lUV + vec2(0.5f,0)).rg;
    vec2 colorR = texture(inputSampler,UV + vec2(normalizedSourceDimensions.x/2,0)).rg;


    vec2 result = min(color,colorR);
    minValue = vec4(result,0,1);
}