

in vec2 UV;

uniform sampler2D texture;
uniform vec2 resolution;

//TODO try to filter everything except red

void main()
{

	vec4 color = texture2D(texture, UV);
   //determine center
   // vec2 position = (gl_FragCoord.xy / resolution.xy) - vec2(0.5);
	vec2 position = (gl_FragCoord.xy / resolution.xy) - vec2(0.5);
	position.x *= resolution.x / resolution.y;
	float colorIntensity = length(position);


	gl_FragColor = vec4(color.rgb*colorIntensity,1);
}