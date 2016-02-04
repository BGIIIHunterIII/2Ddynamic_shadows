#version 330
/*
* calculates and outputs the distance from the center of shadowCastersTexture for each
* non transparent pixel
* saves distance in the red channel of output
*
*/

// texture coordinates
in vec2 UV;

uniform sampler2D shadowCastersTexture;
uniform vec2 textureDimension;

out vec4 color;

void main(){

    color = texture2D(shadowCastersTexture,UV);

    float dist = color.a > 0.3f?1:0.0f;

    color = vec4(dist,0,0,1);
}