#version 330

in vec2 UV;
uniform sampler2D inputSampler;

out vec4 distortedVertex;

// TODO rotation

void main(){

	//translate u and v from [0,1] into [-1 , 1] domain
	float u0 = UV.x * 2.0 - 1.0;
	float v0 = UV.y * 2.0 - 1.0;

	//then, as u0 approaches 0 (the center), v should also approach 0
	v0 = v0 * abs(u0);
	//convert back from [-1,1] domain to [0,1] domain
	v0 = (v0 + 1.0) / 2.0;
	//we now have the coordinates for reading from the initial image
	vec2 newCoords = vec2(UV.x, v0);

	//read for both horizontal and vertical direction and store them in separate channels
	float horizontal = texture2D(inputSampler, newCoords).r;
	float vertical = texture2D(inputSampler, newCoords.yx).r;
	distortedVertex  = vec4(horizontal,vertical ,0,1);
	//debug - check if texture has correct float values (distance * texturedimension.x) almost always > 1
	//distortedVertex = vec4(vec3(texture2D(inputSampler, UV).r>1.1f?1:0),1);
}