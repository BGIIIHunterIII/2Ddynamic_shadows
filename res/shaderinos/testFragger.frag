#version 330

in vec2 UV;

uniform sampler2D texture;
uniform sampler2D texture1;
uniform vec2 resolution;

//TODO try to filter everything except red
out vec4 color;

void main()
{

	color = texture2D(texture, UV);
   //determine center
    vec2 p = UV - vec2(0.5);
	p.x *= resolution.x / resolution.y;
	float colorIntensity = length(p);


	color = vec4(color.rgb*colorIntensity,1);

}