#version 300 es
precision mediump float;
uniform vec3 uLightPos;
uniform sampler2D uShadowTexture;
uniform sampler2D s_baseMap;
uniform float uxPixelOffset;
uniform float uyPixelOffset;
uniform float useSkinTextures;
in vec3 vPosition;
in vec4 vColor;
in vec3 vNormal;
in vec2 vTexcoord;
in vec4 vShadowCoord;
out vec4 outColor;
void main()                    		
{
	vec3 lightVec = uLightPos - vPosition;
	lightVec = normalize(lightVec);
	float diffuseComponent = max(0.0,dot(lightVec, vNormal));
	float ambientComponent = 0.3;
   	float shadow = 1.0;
	if (vShadowCoord.w > 0.0) {
		for (float y = -1.5; y <= 1.5; y = y + 1.0) {
        	for (float x = -1.5; x <= 1.5; x = x + 1.0) {
        	    vec2 offSet = vec2(x,y);
        	    vec4 shadowMapPosition = vShadowCoord / vShadowCoord.w;
                float distanceFromLight = texture(uShadowTexture, (shadowMapPosition +
                	  vec4(offSet.x * uxPixelOffset, offSet.y * uyPixelOffset, 0.05, 0.0)).st ).z;
                float bias;
                vec3 n = normalize(vNormal);
                vec3 l = normalize(uLightPos);
                float cosTheta = clamp(dot( n,l ), 0.0, 1.0);
                bias = 0.0001*tan(acos(cosTheta));
                bias = clamp(bias, 0.0, 0.01);
                float lookUpRet =  float(distanceFromLight > shadowMapPosition.z - bias);
                shadow += lookUpRet;
        	}
        }
        shadow /= 16.0;
        shadow += 0.2;
		shadow = (shadow * 0.8) + 0.2;
	}
	bool hasSkin = (useSkinTextures == 1.0);
	if(hasSkin){
	    vec4 tcolors = texture(s_baseMap,vTexcoord);
	    outColor = tcolors*diffuseComponent + tcolors*ambientComponent* shadow;
	}else{
        outColor = vColor *diffuseComponent + vColor *ambientComponent* shadow;
    }
}                                                                     	
