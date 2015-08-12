#version 130

// Output data ; will be interpolated for each fragment.
varying vec2 UV;

void main(){

	  gl_TexCoord[0] = gl_MultiTexCoord0;
      gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
      UV = gl_TexCoord[0].st;
}
