#version 330

in vec2 UV; //tex0

uniform sampler2D inputSampler;
uniform vec2 normalizedSourceDimensions;

out vec4 minValue;

void main(){
    vec2 color = texture2D(inputSampler, UV).rg;
    // TODO we actually don't need normalized source dimensions the width in pixels
    // this should be changed where the uniform is passed, instead of 1/normilizedSourcedimensions
    vec2 colorR = texture2D(inputSampler,UV + vec2(1f/normalizedSourceDimensions.x,0)).rg;

    vec2 result = min(color,colorR);
    minValue = vec4(result,0,1);
}