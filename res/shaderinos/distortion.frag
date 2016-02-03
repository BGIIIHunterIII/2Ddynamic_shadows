#version 330 core

in vec2 UV;
uniform sampler2D inputSampler;

out vec4 distortedVertex;

// TODO rotation

void main(){
	//translate u and v from [0,1] into [-1 , 1] domain
	float u0 = UV.x * 2.0f - 1.0f;
	float v0 = UV.y * 2.0f - 1.0f;

	//then, as u0 approaches 0 (the center), v should also approach 0
	v0 = v0 * abs(u0);
	//convert back from [-1,1] domain to [0,1] domain
	v0 = (v0 + 1.0f) / 2.0f;

	//we now have the coordinates for reading from the initial image
	vec2 horizontalV = vec2(UV.x,1-v0);
	vec2 verticalV = vec2(v0,1-UV.x);

	//read for both horizontal and vertical direction and store them in separate channels
	float horizontal = texture2D(inputSampler, horizontalV).r;
	float vertical = texture2D(inputSampler, verticalV).r;
	distortedVertex  = vec4(horizontal,vertical ,0,1);
}