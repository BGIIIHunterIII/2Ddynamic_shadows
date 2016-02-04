#version 330

in vec2 UV;


uniform sampler2D mapSampler;//tex1
uniform vec2 renderTargetSize;

out vec4 result;

vec4 GetShadowDistanceH(vec2 TexCoord, float displacementV);
vec4 GetShadowDistanceV(vec2 TexCoord, float displacementV);


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

  vec4 inverse;

	//use these to determine which quadrant we are in
	if(abs(nY)<abs(nX))
	{ //left or right segment
		inverse = GetShadowDistanceH(UV,0);
	}
	else
	{ //upper or lower segment
	  inverse = GetShadowDistanceV(UV,0);
	}
	result = inverse;
}


vec4 GetShadowDistanceH(vec2 TexCoord, float displacementV)
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
		return texture2D(mapSampler, newCoords).rgba;
}

vec4 GetShadowDistanceV(vec2 TexCoord, float displacementV)
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
		return texture2D(mapSampler, newCoords).rgba;
}