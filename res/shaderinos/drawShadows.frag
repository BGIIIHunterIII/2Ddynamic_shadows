in vec2 UV;

uniform sampler2D shadowTextureSampler;//tex0
uniform sampler2D shadowMapSampler;//tex1
uniform vec2 renderTargetSize;

out vec4 result;

float GetShadowDistanceH(in vec2 TexCoord);
float GetShadowDistanceV(in vec2 TexCoord);

void main(){
    // distance of this pixel from the center

    vec2 pos = (gl_FragCoord.xy / renderTargetSize.xy) - vec2(0.5);
    pos.x *= renderTargetSize.x / renderTargetSize.y;
    float distance = length(pos);
    //distance *= renderTargetSize.x;

    //apply a 2-pixel bias
    distance -=2;

    //distance stored in the shadow map
    float shadowMapDistance=1;

    //coords in [-1,1]
    float nY = 2.0f*( UV.y - 0.5f);
    float nX = 2.0f*( UV.x - 0.5f);

    //we use these to determine which quadrant we are in
    if(abs(nY)<abs(nX))
    {
     shadowMapDistance=GetShadowDistanceH(UV);
    }
    else
    {
    shadowMapDistance=GetShadowDistanceV(UV);
    }

    //if distance to this pixel is lower than distance from shadowMap,
    //then we are not in shadow
    float light = distance < shadowMapDistance ? 1:0;
    vec3 colorR = texture2D(shadowMapSampler, vec2(1,1)).rgb;
    float debug = shadowMapDistance<1?1:0;


    result = vec4( vec3(shadowMapDistance),1.0);
    //result = vec4(colorR,1);
    //result.b = length(UV - 0.5f);//for gaussian blur


}

float GetShadowDistanceH(in vec2 TexCoord)
{
 float u = TexCoord.x;
 float v = 1-TexCoord.y;

 u = abs(u-0.5f) * 2;
 v = v * 2 - 1;
 float v0 = v/u;
 v0 = (v0 + 1) / 2;

 vec2 newCoords = vec2(TexCoord.x,v0);
 //horizontal info was stored in the Red component
 return texture2D(shadowMapSampler, newCoords).r;
}

float GetShadowDistanceV(in vec2 TexCoord)
{
 float u = 1-TexCoord.y;
 float v = TexCoord.x;

 u = abs(u-0.5f) * 2;
 v = v * 2 - 1;
 float v0 = v/u;
 v0 = (v0 + 1) / 2;

 vec2 newCoords = vec2(TexCoord.y,v0);
 //vertical info was stored in the Green component
 return texture2D(shadowMapSampler, newCoords).g;
}