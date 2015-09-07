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

    vec2 centerToPixel = UV - vec2(0.5);
    float dist = color.a > 0.3f?length(centerToPixel):1.0f;

    dist *= textureDimension.x; //comment out for debugging

    color = vec4(dist,0,0,1);
}