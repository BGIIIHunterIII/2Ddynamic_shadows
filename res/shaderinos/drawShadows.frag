#version 330

in vec2 UV;


uniform sampler2D mapSampler;//tex1
uniform vec2 renderTargetSize;

out vec4 result;

float GetShadowDistanceH(vec2 TexCoord, float displacementV);
float GetShadowDistanceV(vec2 TexCoord, float displacementV);


void main(){
	// distance of this pixel from the center
  vec2 centerToPixel = UV - vec2(0.5);
  float distance = length(centerToPixel);


  distance *=512;
	//apply a 2-pixel bias
	distance -=2.0f;

	//distance stored in the shadow map
	float shadowMapDistance;

	//coords in [-1,1]
	float nY = 2.0f*( UV.y - 0.5f);
  float nX = 2.0f*( UV.x - 0.5f);

	//use these to determine which quadrant we are in
	if(abs(nY)<abs(nX))
	{ //left or right segment
		shadowMapDistance = GetShadowDistanceH(UV,0);
	}
	else
	{ //upper or lower segment
	  shadowMapDistance = GetShadowDistanceV(UV,0);
	}

	//if distance to this pixel is lower than distance from shadowMap,
	//then we are not in shadow
	float light = distance < shadowMapDistance? 1:0;

	float debug = distance == shadowMapDistance/1?1:0;

	result = vec4(vec3(light),1);
	//result = vec4(texture2D(mapSampler,UV).rgb,1);

//	  result.b = length(TexCoord - 0.5f);
}


float GetShadowDistanceH(vec2 TexCoord, float displacementV)
{
		float u = TexCoord.x;
		float v = TexCoord.y;

		u = abs(u-0.5f) * 2;
		v = v * 2 - 1;
	  float v0 = v/u;
		v0+=displacementV;
		v0 = (v0 + 1) / 2;

		vec2 newCoords = vec2(TexCoord.x,v0);
		//horizontal info was stored in the Red component
		return texture2D(mapSampler, newCoords).r;
}

float GetShadowDistanceV(vec2 TexCoord, float displacementV)
{
		float u = TexCoord.y;
		float v = TexCoord.x;

		u = abs(u-0.5f) * 2;
		v = v * 2 - 1;
		float v0 = v/u;
		v0+=displacementV;
		v0 = (v0 + 1) / 2;

		vec2 newCoords = vec2(TexCoord.y,v0);
		//vertical info was stored in the Green component
		return texture2D(mapSampler, newCoords).g;
}