package com.javohir.physicsbubble

const val KINEMATIC_LENS_SHADER = """
uniform shader composable;
uniform float2 touchCenter;
uniform float radius;
uniform float progress; 
uniform float2 deformation; 
uniform float popProgress;
uniform float sysTime;
float hash(float2 p) {
    return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
}
float smoothNoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
               mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
}
half4 main(float2 fragCoord) {
    float THICKNESS_BASE = 300.0;
    float THICKNESS_GRAVITY = 120.0;
    float THICKNESS_SWIRL = 100.0;
    float THICKNESS_DETAIL = 40.0;
    float COLOR_INTENSITY = 2.0;
    float EDGE_FADE_END = 0.20;
    float ENV_REFLECTION_STRENGTH = 0.4;
    float ENV_BLUR_RADIUS = 50.0;
    half4 rawBackground = composable.eval(fragCoord);
    if (popProgress >= 1.0) return rawBackground;
    float2 rawUv = fragCoord - touchCenter;
    
    float speed = length(deformation); 
    float2 moveDir = speed > 0.001 ? deformation / speed : float2(0.0, 1.0); 
    
    float parallelDist = dot(rawUv, moveDir);
    float2 perpVector = rawUv - moveDir * parallelDist;
    
    float stretch = 1.0 + speed; 
    float squash = 1.0 / sqrt(stretch); 
    
    float2 uv = (moveDir * (parallelDist / stretch)) + (perpVector / squash);
    float dist = length(uv);
    float activeRadius = radius * (1.0 + popProgress * 1.5);
    if (dist >= activeRadius) {
        return rawBackground;
    }
    float2 nUv = uv / activeRadius;
    float distSq = dot(nUv, nUv);
    float z = sqrt(max(0.0, 1.0 - distSq));
    float3 normal = normalize(float3(nUv, z));
    float3 viewDir = float3(0.0, 0.0, 1.0); 
    float NdotV = max(0.0, dot(normal, viewDir));
    float magnification = 0.45;
    float lensDeform = (1.0 - z) * magnification * (1.0 - popProgress);
    
    float2 refUvR = fragCoord - (nUv * activeRadius * (lensDeform * 0.88));
    float2 refUvG = fragCoord - (nUv * activeRadius * (lensDeform * 1.00));
    float2 refUvB = fragCoord - (nUv * activeRadius * (lensDeform * 1.12));
    half3 bgColor = half3(
        composable.eval(refUvR).r,
        composable.eval(refUvG).g,
        composable.eval(refUvB).b
    );
    float3 reflectionDir = reflect(-viewDir, normal);
    float3 lightDir1 = normalize(float3(0.6, 0.7, 0.8));
    float3 lightDir2 = normalize(float3(-0.5, -0.4, 0.6));
    
    float lightAlign1 = max(0.0, dot(reflectionDir, lightDir1));
    float lightAlign2 = max(0.0, dot(reflectionDir, lightDir2));
    float n_film = 1.33;
    float n_air  = 1.0;
    float R0 = pow((n_film - n_air) / (n_film + n_air), 2.0);
    float fresnel = R0 + (1.0 - R0) * pow(1.0 - NdotV, 5.0);
    float sinThetaI = sqrt(max(0.0, 1.0 - NdotV * NdotV));
    float sinThetaT = sinThetaI / n_film;
    float cosThetaT = sqrt(max(0.0, 1.0 - sinThetaT * sinThetaT));
    float swirl = smoothNoise(nUv * 3.0 + sysTime * 0.12);
    float thicknessNoise = smoothNoise(nUv * 5.0 - sysTime * 0.08);
    float baseThickness = THICKNESS_BASE + nUv.y * THICKNESS_GRAVITY;
    float thickness = baseThickness + swirl * THICKNESS_SWIRL + thicknessNoise * THICKNESS_DETAIL;
    thickness = clamp(thickness, 80.0, 900.0);
    float opd = 2.0 * n_film * thickness * cosThetaT;
    float lambda_R = 650.0;
    float lambda_G = 532.0;
    float lambda_B = 450.0;
    float TWO_PI = 6.2831853;
    float oscR = 0.5 + 0.5 * cos(TWO_PI * opd / lambda_R);
    float oscG = 0.5 + 0.5 * cos(TWO_PI * opd / lambda_G);
    float oscB = 0.5 + 0.5 * cos(TWO_PI * opd / lambda_B);
    half3 interferenceColor = half3(oscR, oscG, oscB);
    float interferenceStrength = smoothstep(0.0, EDGE_FADE_END, NdotV);
    half3 filmReflection = interferenceColor * fresnel * COLOR_INTENSITY;
    half3 whiteReflection = half3(fresnel);
    half3 thinFilmColor = mix(whiteReflection, filmReflection, interferenceStrength);
    float spec1 = pow(lightAlign1, 250.0) * 2.5;
    float spec2 = pow(lightAlign2, 60.0) * 0.5;
    half3 highlights = half3(spec1 + spec2);
    float2 reflectOffset = normal.xy * ENV_BLUR_RADIUS;
    float2 envCenter = fragCoord + reflectOffset;
    float blurStep = ENV_BLUR_RADIUS * 0.4;
    half3 envSample = composable.eval(envCenter).rgb * 0.4
        + composable.eval(envCenter + float2(blurStep, 0.0)).rgb * 0.15
        + composable.eval(envCenter - float2(blurStep, 0.0)).rgb * 0.15
        + composable.eval(envCenter + float2(0.0, blurStep)).rgb * 0.15
        + composable.eval(envCenter - float2(0.0, blurStep)).rgb * 0.15;
    half3 envReflection = envSample * fresnel * ENV_REFLECTION_STRENGTH;
    float rimShadow = smoothstep(0.92, 1.0, sqrt(distSq));
    bgColor *= (1.0 - rimShadow * 0.25);
    half3 finalColor = bgColor * (1.0 - half3(fresnel)) + thinFilmColor + envReflection + highlights;
    float fadeOut = 1.0 - pow(popProgress, 0.5);
    return half4(mix(rawBackground.rgb, finalColor, fadeOut), rawBackground.a);
}
"""
