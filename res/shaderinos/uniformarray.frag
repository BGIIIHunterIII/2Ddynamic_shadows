/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
#version 330 core

in vec2 UV;

uniform vec3 cols[4];
uniform int chosen;
uniform sampler2D texture;

out vec4 color;

void main(void) {
    vec4 chosenColor = vec4(cols[chosen],1.0);
    vec4 textureColor = texture2D(texture,UV);


    color = chosenColor * textureColor;
}