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



void main(){

    vec2 UV = gl_TexCoord[0].st;
    vec4 color = texture2D(shadowCastersTexture,UV);

    vec2 position = (gl_FragCoord.xy / textureDimension.xy) - vec2(0.5);
    position.x *= textureDimension.x / textureDimension.y;
    float dist = color.a > 0.3f?length(position):1.0f;

    //dist *= textureDimension.x;
    gl_FragColor = vec4(dist,0,0,1);
}