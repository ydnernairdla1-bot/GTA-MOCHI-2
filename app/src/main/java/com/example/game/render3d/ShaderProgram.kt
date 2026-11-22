package com.example.game.render3d

import android.opengl.GLES20
import android.util.Log

object GlUtils {
    private const val TAG = "NeonViceGL"

    fun createProgram(vertexSrc: String, fragmentSrc: String): Int {
        val vShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vShader)
        GLES20.glAttachShader(program, fShader)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            Log.e(TAG, "Program link failed: $log")
            GLES20.glDeleteProgram(program)
            return 0
        }
        GLES20.glDeleteShader(vShader)
        GLES20.glDeleteShader(fShader)
        return program
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            Log.e(TAG, "Shader compile failed: $log")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}

class LitShader {
    val program = GlUtils.createProgram(VERTEX_SRC, FRAGMENT_SRC)
    val aPosition = GLES20.glGetAttribLocation(program, "aPosition")
    val aNormal = GLES20.glGetAttribLocation(program, "aNormal")
    val aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord")

    val uMVP = GLES20.glGetUniformLocation(program, "uMVP")
    val uModel = GLES20.glGetUniformLocation(program, "uModel")
    val uTintColor = GLES20.glGetUniformLocation(program, "uTintColor")
    val uUseTexture = GLES20.glGetUniformLocation(program, "uUseTexture")
    val uTexture = GLES20.glGetUniformLocation(program, "uTexture")
    val uSpecularStrength = GLES20.glGetUniformLocation(program, "uSpecularStrength")
    val uShininess = GLES20.glGetUniformLocation(program, "uShininess")
    val uEmissive = GLES20.glGetUniformLocation(program, "uEmissive")
    val uCameraPos = GLES20.glGetUniformLocation(program, "uCameraPos")
    val uSunDir = GLES20.glGetUniformLocation(program, "uSunDir")
    val uSunColor = GLES20.glGetUniformLocation(program, "uSunColor")
    val uSkyAmbient = GLES20.glGetUniformLocation(program, "uSkyAmbient")
    val uGroundAmbient = GLES20.glGetUniformLocation(program, "uGroundAmbient")
    val uFogColor = GLES20.glGetUniformLocation(program, "uFogColor")
    val uFogDensity = GLES20.glGetUniformLocation(program, "uFogDensity")
    val uPointPos = GLES20.glGetUniformLocation(program, "uPointPos")
    val uPointColor = GLES20.glGetUniformLocation(program, "uPointColor")

    companion object {
        private const val VERTEX_SRC = """
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec2 aTexCoord;

            uniform mat4 uMVP;
            uniform mat4 uModel;

            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;
            varying vec2 vTexCoord;

            void main() {
                vec4 worldPos = uModel * vec4(aPosition, 1.0);
                vWorldPos = worldPos.xyz;
                vWorldNormal = normalize((uModel * vec4(aNormal, 0.0)).xyz);
                vTexCoord = aTexCoord;
                gl_Position = uMVP * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SRC = """
            precision mediump float;

            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;
            varying vec2 vTexCoord;

            uniform vec4 uTintColor;
            uniform float uUseTexture;
            uniform sampler2D uTexture;
            uniform float uSpecularStrength;
            uniform float uShininess;
            uniform vec3 uEmissive;
            uniform vec3 uCameraPos;
            uniform vec3 uSunDir;
            uniform vec3 uSunColor;
            uniform vec3 uSkyAmbient;
            uniform vec3 uGroundAmbient;
            uniform vec3 uFogColor;
            uniform float uFogDensity;
            uniform vec3 uPointPos[4];
            uniform vec3 uPointColor[4];

            void main() {
                vec4 texel = texture2D(uTexture, vTexCoord);
                vec4 baseColor = mix(uTintColor, texel * uTintColor, uUseTexture);
                if (baseColor.a < 0.08) discard;

                vec3 N = normalize(vWorldNormal);
                vec3 V = normalize(uCameraPos - vWorldPos);
                vec3 L = normalize(uSunDir);

                // Hemisphere ambient lighting (warm street bounce vs crisp twilight sky)
                float hemi = N.y * 0.5 + 0.5;
                vec3 ambient = mix(uGroundAmbient, uSkyAmbient, hemi);

                // Key directional light (sunset/twilight key) + crisp Blinn-Phong specular
                float NdotL = max(dot(N, L), 0.0);
                vec3 H = normalize(L + V);
                float spec = pow(max(dot(N, H), 0.0), uShininess) * uSpecularStrength * (0.35 + 0.65 * NdotL);
                vec3 direct = uSunColor * NdotL;

                // Secondary cool neon rim fill from opposite horizon for crisp 3D silhouette edge
                vec3 rimDir = normalize(vec3(-L.x, 0.42, -L.z));
                float rimDiff = max(dot(N, rimDir), 0.0) * 0.34;
                vec3 rimLight = vec3(0.28, 0.46, 0.72) * rimDiff;

                // Subtle vertical height contact AO near ground (y < 3.5) for grounded object clarity
                float groundContact = smoothstep(0.0, 4.0, vWorldPos.y);
                float contactShade = mix(0.78, 1.0, groundContact);

                // Dynamic real-time point lights (headlights, police sirens, explosions)
                vec3 pointAccum = vec3(0.0);
                for (int i = 0; i < 4; i++) {
                    vec3 pVec = uPointPos[i] - vWorldPos;
                    float dist = length(pVec);
                    float atten = 1.0 / (1.0 + 0.0040 * dist + 0.00014 * dist * dist);
                    vec3 pDir = pVec / max(dist, 0.001);
                    float pDiff = max(dot(N, pDir), 0.16);
                    pointAccum += uPointColor[i] * pDiff * atten;
                }

                // Crisp Fresnel reflection boost on cars/glass/wet roads
                float fresnel = pow(1.0 - max(dot(N, V), 0.0), 2.8) * uSpecularStrength * 0.42;

                vec3 lit = baseColor.rgb * (ambient + direct + rimLight + pointAccum) * contactShade
                    + vec3(spec + fresnel)
                    + uEmissive;

                // Atmospheric distance fog tuned to keep mid-range architecture sharp
                float camDist = length(uCameraPos - vWorldPos);
                float fogFactor = 1.0 - clamp(exp(-camDist * camDist * uFogDensity * uFogDensity), 0.0, 1.0);
                vec3 finalRgb = mix(lit, uFogColor, fogFactor * 0.88);

                gl_FragColor = vec4(finalRgb, baseColor.a);
            }
        """
    }
}

class SkyShader {
    val program = GlUtils.createProgram(VERTEX_SRC, FRAGMENT_SRC)
    val aPosition = GLES20.glGetAttribLocation(program, "aPosition")
    val uMVP = GLES20.glGetUniformLocation(program, "uMVP")
    val uTime = GLES20.glGetUniformLocation(program, "uTime")

    companion object {
        private const val VERTEX_SRC = """
            attribute vec3 aPosition;
            uniform mat4 uMVP;
            varying vec3 vDir;
            void main() {
                vDir = normalize(aPosition);
                gl_Position = uMVP * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SRC = """
            precision mediump float;
            varying vec3 vDir;
            uniform float uTime;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
            }

            void main() {
                vec3 d = normalize(vDir);
                float h = clamp(d.y, 0.0, 1.0);
                vec3 horizonColor = vec3(0.32, 0.24, 0.42);
                vec3 midSkyColor = vec3(0.11, 0.19, 0.35);
                vec3 zenithColor = vec3(0.04, 0.07, 0.17);

                vec3 sky = mix(horizonColor, midSkyColor, smoothstep(0.0, 0.26, h));
                sky = mix(sky, zenithColor, smoothstep(0.22, 0.78, h));

                // Warm coastal sunset/dusk glow toward the ocean (-X)
                float sunAzimuth = max(0.0, dot(normalize(vec3(d.x, 0.0, d.z)), normalize(vec3(-0.85, 0.0, -0.45))));
                float horizonGlow = exp(-h * 6.2) * pow(sunAzimuth, 2.0);
                sky += vec3(0.68, 0.30, 0.26) * horizonGlow;

                // Subtle twilight stars in upper sky
                if (h > 0.18) {
                    vec2 starCell = floor(vec2(atan(d.z, d.x) * 32.0, d.y * 42.0));
                    float starVal = hash(starCell);
                    if (starVal > 0.988) {
                        float twinkle = 0.55 + 0.45 * sin(uTime * 2.5 + starVal * 60.0);
                        sky += vec3(0.78, 0.86, 1.0) * twinkle * smoothstep(0.18, 0.50, h);
                    }
                }

                // Crisp moon disc & halo in upper twilight sky
                vec3 moonDir = normalize(vec3(0.55, 0.52, -0.65));
                float moonDot = dot(d, moonDir);
                float moonDisc = smoothstep(0.9986, 0.9993, moonDot);
                float moonHalo = pow(max(moonDot, 0.0), 42.0) * 0.34;
                sky += vec3(0.90, 0.94, 1.0) * (moonDisc + moonHalo);

                gl_FragColor = vec4(sky, 1.0);
            }
        """
    }
}
