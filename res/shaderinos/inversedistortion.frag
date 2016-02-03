#version 330 core

in vec2 UV;

uniform sampler2D texture;

out vec4 inverse;

vec4 InverseH(vec2 TexCoord);
vec4 InverseV(vec2 TexCoord);

void main(){

//coords in [-1,1]
  float nY = 2.0f*( UV.y - 0.5f);
  float nX = 2.0f*( UV.x - 0.5f);
  inverse = vec4(1,0,0,1);

	//use these to determine which quadrant we are in
	if(abs(nY)<abs(nX))
	{ //left or right segment
				float u = UV.x;
        		float v = UV.y;

        		u = abs(u-0.5f) * 2;
        		v = v * 2 - 1;
        	    float v0 = v/u;
        		v0 = (v0 + 1) / 2;

        		vec2 newCoords = vec2(UV.x,v0);
        		//horizontal info was stored in the Red component
        		inverse =  texture2D(texture, newCoords);
	}
	else
	{ //upper or lower segment
	  		float u = UV.y;
      		float v = UV.x;

      		u = abs(u-0.5f) * 2;
      		v = v * 2 - 1;
      		float v0 = v/u;
      		v0 = (v0 + 1) / 2;

      		vec2 newCoords = vec2(UV.y,v0);
      		//vertical info was stored in the Green component
      		inverse = texture2D(texture, newCoords);
	}
}

vec4 InverseH(vec2 TexCoord)
{
		float u = TexCoord.x;
		float v = TexCoord.y;

		u = abs(u-0.5f) * 2;
		v = v * 2 - 1;
	    float v0 = v/u;
		v0 = (v0 + 1) / 2;

		vec2 newCoords = vec2(TexCoord.x,v0);
		//horizontal info was stored in the Red component
		return texture2D(texture, newCoords);
}

vec4 InverseV(vec2 TexCoord)
{
		float u = TexCoord.y;
		float v = TexCoord.x;

		u = abs(u-0.5f) * 2;
		v = v * 2 - 1;
		float v0 = v/u;
		v0 = (v0 + 1) / 2;

		vec2 newCoords = vec2(TexCoord.y,v0);
		//vertical info was stored in the Green component
		return texture2D(texture, newCoords);
}