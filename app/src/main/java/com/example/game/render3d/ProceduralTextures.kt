package com.example.game.render3d

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.opengl.GLES20
import android.opengl.GLUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class TextureLibrary {
    val roadAsphalt = createRoadTexture()
    val intersectionAsphalt = createIntersectionTexture()
    val sidewalkConcrete = createSidewalkTexture()
    val facadeGlass = createGlassFacadeTexture()
    val facadeConcrete = createConcreteFacadeTexture()
    val facadeArtDeco = createArtDecoFacadeTexture()
    val storefront = createStorefrontTexture()
    val roofTar = createRoofTexture()
    val oceanWater = createOceanTexture()
    val beachSand = createSandTexture()
    val carPaint = createCarMetallicTexture()
    val tireWheel = createTireWheelTexture()
    val palmBark = createPalmBarkTexture()
    val palmFrond = createPalmFrondTexture()
    val humanFace = createHumanFaceTexture()
    val catFace = createCatFaceTexture()
    val catFur = createCatFurTexture()
    val fabricWeave = createFabricTexture()
    val cashBill = createCashTexture()
    val medkitCase = createMedkitTexture()
    val briefcaseLeather = createBriefcaseTexture()
    val lightPool = createLightPoolTexture()

    fun release() {
        val ids = intArrayOf(
            roadAsphalt,
            intersectionAsphalt,
            sidewalkConcrete,
            facadeGlass,
            facadeConcrete,
            facadeArtDeco,
            storefront,
            roofTar,
            oceanWater,
            beachSand,
            carPaint,
            tireWheel,
            palmBark,
            palmFrond,
            humanFace,
            catFace,
            catFur,
            fabricWeave,
            cashBill,
            medkitCase,
            briefcaseLeather,
            lightPool,
        )
        GLES20.glDeleteTextures(ids.size, ids, 0)
    }

    private fun uploadBitmap(bmp: Bitmap, repeat: Boolean = true): Int {
        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        val id = texIds[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        val wrap = if (repeat) GLES20.GL_REPEAT else GLES20.GL_CLAMP_TO_EDGE
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrap)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrap)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR_MIPMAP_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        bmp.recycle()
        return id
    }

    private fun addAsphaltNoise(bmp: Bitmap, baseR: Int, baseG: Int, baseB: Int, variance: Int, seed: Int) {
        val rng = Random(seed)
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        for (i in pixels.indices) {
            val n = rng.nextInt(-variance, variance + 1)
            val r = (baseR + n).coerceIn(0, 255)
            val g = (baseG + n).coerceIn(0, 255)
            val b = (baseB + n).coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun createRoadTexture(): Int {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        addAsphaltNoise(bmp, 38, 41, 48, 12, 101)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Tire wear tracks & darker wheel-path sheen
        paint.color = Color.argb(56, 14, 16, 22)
        canvas.drawRect(76f, 0f, 152f, size.toFloat(), paint)
        canvas.drawRect(360f, 0f, 436f, size.toFloat(), paint)

        // Subtle asphalt gutter shadow along curb edges
        paint.color = Color.argb(95, 16, 18, 24)
        canvas.drawRect(0f, 0f, 18f, size.toFloat(), paint)
        canvas.drawRect(size - 18f, 0f, size.toFloat(), size.toFloat(), paint)

        // Storm drain grates near edges
        paint.color = Color.rgb(24, 26, 32)
        canvas.drawRect(6f, 180f, 20f, 224f, paint)
        canvas.drawRect(size - 20f, 180f, size - 6f, 224f, paint)

        // Crisp white edge shoulder lines
        paint.color = Color.argb(225, 226, 232, 240)
        canvas.drawRect(22f, 0f, 30f, size.toFloat(), paint)
        canvas.drawRect(size - 30f, 0f, size - 22f, size.toFloat(), paint)

        // Double amber-yellow centerline with crisp dark gap
        paint.color = Color.argb(245, 242, 192, 54)
        canvas.drawRect(size / 2f - 13f, 0f, size / 2f - 4f, size.toFloat(), paint)
        canvas.drawRect(size / 2f + 4f, 0f, size / 2f + 13f, size.toFloat(), paint)

        // Dashed white lane dividers + reflective cat's-eye studs
        var y = 24f
        while (y < size) {
            paint.color = Color.argb(220, 234, 238, 246)
            canvas.drawRect(size * 0.25f - 4f, y, size * 0.25f + 4f, y + 72f, paint)
            canvas.drawRect(size * 0.75f - 4f, y, size * 0.75f + 4f, y + 72f, paint)
            // Reflective cyan-white road studs between dashes
            paint.color = Color.argb(215, 160, 238, 255)
            canvas.drawCircle(size * 0.25f, y - 16f, 3.2f, paint)
            canvas.drawCircle(size * 0.75f, y - 16f, 3.2f, paint)
            y += 128f
        }
        return uploadBitmap(bmp)
    }

    private fun createIntersectionTexture(): Int {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        addAsphaltNoise(bmp, 37, 40, 47, 10, 202)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Crisp stop bars before crosswalks
        paint.color = Color.argb(200, 228, 232, 240)
        canvas.drawRect(44f, 4f, size - 44f, 12f, paint)
        canvas.drawRect(44f, size - 12f, size - 44f, size - 4f, paint)
        canvas.drawRect(4f, 44f, 12f, size - 44f, paint)
        canvas.drawRect(size - 12f, 44f, size - 4f, size - 44f, paint)

        // High-contrast Zebra Crosswalk stripes on all 4 sides
        paint.color = Color.argb(235, 236, 240, 246)
        for (i in 0 until 8) {
            val pos = 44f + i * 56f
            canvas.drawRect(pos, 18f, pos + 28f, 72f, paint)
            canvas.drawRect(pos, size - 72f, pos + 28f, size - 18f, paint)
            canvas.drawRect(18f, pos, 72f, pos + 28f, paint)
            canvas.drawRect(size - 72f, pos, size - 18f, pos + 28f, paint)
        }

        // Subtle intersection box outline
        paint.color = Color.argb(95, 242, 192, 54)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRect(88f, 88f, size - 88f, size - 88f, paint)
        paint.style = Paint.Style.FILL

        // Detailed cast-iron utility manhole cover in center
        paint.color = Color.rgb(66, 70, 78)
        canvas.drawCircle(size / 2f, size / 2f, 22f, paint)
        paint.color = Color.rgb(28, 30, 36)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.5f
        canvas.drawCircle(size / 2f, size / 2f, 22f, paint)
        canvas.drawCircle(size / 2f, size / 2f, 13f, paint)
        paint.style = Paint.Style.FILL
        return uploadBitmap(bmp)
    }

    private fun createSidewalkTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        addAsphaltNoise(bmp, 114, 116, 124, 10, 303)
        val canvas = Canvas(bmp)
        val paint = Paint()

        // Concrete tile paving slabs with dark joint & sunlit bevel highlight
        var p = 0f
        while (p <= size) {
            paint.color = Color.argb(150, 48, 50, 58)
            paint.strokeWidth = 3f
            canvas.drawLine(p, 0f, p, size.toFloat(), paint)
            canvas.drawLine(0f, p, size.toFloat(), p, paint)

            paint.color = Color.argb(65, 210, 214, 224)
            paint.strokeWidth = 1.5f
            canvas.drawLine(p + 2f, 0f, p + 2f, size.toFloat(), paint)
            canvas.drawLine(0f, p + 2f, size.toFloat(), p + 2f, paint)
            p += 64f
        }
        return uploadBitmap(bmp)
    }

    private fun createGlassFacadeTexture(): Int {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(20, 30, 46))

        val rng = Random(404)
        val paint = Paint()
        val cols = 12
        val rows = 16
        val cw = size / cols.toFloat()
        val rh = size / rows.toFloat()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val left = c * cw + 3.5f
                val top = r * rh + 4.5f
                val right = (c + 1) * cw - 3.5f
                val bottom = (r + 1) * rh - 5.5f
                val litRoll = rng.nextFloat()
                if (litRoll > 0.48f) {
                    val warm = rng.nextFloat() > 0.34f
                    val topCol = if (warm) Color.rgb(248, 214, 148) else Color.rgb(168, 232, 255)
                    val botCol = if (warm) Color.rgb(174, 132, 74) else Color.rgb(88, 152, 198)
                    paint.shader = LinearGradient(left, top, left, bottom, topCol, botCol, Shader.TileMode.CLAMP)
                    canvas.drawRect(left, top, right, bottom, paint)
                    paint.shader = null
                    // Subtle interior ceiling/desk band for realism
                    paint.color = Color.argb(48, 30, 26, 22)
                    canvas.drawRect(left, (top + bottom) * 0.56f, right, bottom, paint)
                } else {
                    val dark = 30 + rng.nextInt(26)
                    paint.shader = LinearGradient(
                        left,
                        top,
                        left,
                        bottom,
                        Color.rgb(dark + 22, dark + 42, dark + 68),
                        Color.rgb(dark, dark + 14, dark + 30),
                        Shader.TileMode.CLAMP,
                    )
                    canvas.drawRect(left, top, right, bottom, paint)
                    paint.shader = null
                    // Diagonal sky reflection highlight
                    paint.color = Color.argb(38, 165, 215, 250)
                    canvas.drawLine(left + 2f, top + 2f, right - 4f, top + 9f, paint)
                }
            }
        }
        paint.shader = null
        // Crisp floor slab spandrels + vertical aluminum mullions
        paint.color = Color.rgb(14, 20, 30)
        for (r in 0..rows) {
            canvas.drawRect(0f, r * rh - 3f, size.toFloat(), r * rh + 3f, paint)
        }
        paint.color = Color.rgb(34, 44, 62)
        for (c in 0..cols) {
            canvas.drawRect(c * cw - 1.5f, 0f, c * cw + 1.5f, size.toFloat(), paint)
        }
        return uploadBitmap(bmp)
    }

    private fun createConcreteFacadeTexture(): Int {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        addAsphaltNoise(bmp, 84, 82, 92, 8, 505)
        val canvas = Canvas(bmp)
        val rng = Random(506)
        val paint = Paint()

        val cols = 8
        val rows = 12
        val cw = size / cols.toFloat()
        val rh = size / rows.toFloat()

        for (r in 0 until rows) {
            // Floor concrete ledge shadow & highlight
            paint.color = Color.argb(95, 26, 24, 34)
            canvas.drawRect(0f, r * rh, size.toFloat(), r * rh + 5f, paint)
            paint.color = Color.argb(55, 165, 162, 178)
            canvas.drawRect(0f, r * rh + 5f, size.toFloat(), r * rh + 7f, paint)

            for (c in 0 until cols) {
                val left = c * cw + 10f
                val top = r * rh + 10f
                val right = (c + 1) * cw - 10f
                val bottom = (r + 1) * rh - 7f
                // Recessed frame
                paint.color = Color.rgb(34, 36, 46)
                canvas.drawRect(left - 2.5f, top - 2.5f, right + 2.5f, bottom + 2.5f, paint)
                val lit = rng.nextFloat() > 0.40f
                paint.color = if (lit) {
                    if (rng.nextBoolean()) Color.rgb(245, 204, 130) else Color.rgb(206, 228, 250)
                } else {
                    Color.rgb(24, 28, 40)
                }
                canvas.drawRect(left, top, right, bottom, paint)
                // Window crossbar mullion
                paint.color = Color.rgb(36, 38, 48)
                canvas.drawRect((left + right) * 0.5f - 1.2f, top, (left + right) * 0.5f + 1.2f, bottom, paint)
            }
        }
        return uploadBitmap(bmp)
    }

    private fun createArtDecoFacadeTexture(): Int {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        addAsphaltNoise(bmp, 128, 118, 112, 7, 606)
        val canvas = Canvas(bmp)
        val rng = Random(607)
        val paint = Paint()

        val cols = 10
        val rows = 12
        val cw = size / cols.toFloat()
        val rh = size / rows.toFloat()

        // Art-Deco vertical stone pilasters with crisp edge highlight
        for (c in 0..cols) {
            paint.color = Color.argb(125, 168, 158, 148)
            canvas.drawRect(c * cw - 5f, 0f, c * cw + 5f, size.toFloat(), paint)
            paint.color = Color.argb(70, 58, 52, 54)
            canvas.drawRect(c * cw + 4f, 0f, c * cw + 6f, size.toFloat(), paint)
        }
        for (r in 0 until rows) {
            paint.color = Color.argb(65, 225, 208, 188)
            canvas.drawRect(0f, r * rh, size.toFloat(), r * rh + 3f, paint)
            for (c in 0 until cols) {
                val left = c * cw + 9f
                val top = r * rh + 8f
                val right = (c + 1) * cw - 9f
                val bottom = (r + 1) * rh - 6f
                paint.color = Color.rgb(38, 34, 46)
                canvas.drawRect(left - 2f, top - 2f, right + 2f, bottom + 2f, paint)
                val lit = rng.nextFloat() > 0.32f
                paint.color = if (lit) Color.rgb(248, 212, 142) else Color.rgb(32, 36, 52)
                canvas.drawRect(left, top, right, bottom, paint)
            }
        }
        return uploadBitmap(bmp)
    }

    private fun createStorefrontTexture(): Int {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        addAsphaltNoise(bmp, 56, 54, 62, 6, 650)
        val canvas = Canvas(bmp)
        val paint = Paint()

        // Stone columns, bright shop display windows, neon fascia strip & double doors
        val bays = 4
        val bw = size / bays.toFloat()
        for (b in 0 until bays) {
            val left = b * bw + 14f
            val right = (b + 1) * bw - 14f
            paint.color = Color.rgb(30, 32, 42)
            canvas.drawRect(left - 4f, 52f, right + 4f, size - 8f, paint)
            val isDoor = b == 1
            if (isDoor) {
                paint.shader = LinearGradient(left, 68f, left, size - 10f, Color.rgb(185, 224, 248), Color.rgb(74, 108, 142), Shader.TileMode.CLAMP)
                canvas.drawRect(left, 68f, right, size - 8f, paint)
                paint.shader = null
                // Door center divider + metallic push handles
                paint.color = Color.rgb(28, 32, 42)
                canvas.drawRect((left + right) * 0.5f - 2.5f, 68f, (left + right) * 0.5f + 2.5f, size - 8f, paint)
                paint.color = Color.rgb(220, 226, 236)
                canvas.drawRect((left + right) * 0.5f - 9f, size * 0.52f, (left + right) * 0.5f - 5f, size * 0.66f, paint)
                canvas.drawRect((left + right) * 0.5f + 5f, size * 0.52f, (left + right) * 0.5f + 9f, size * 0.66f, paint)
            } else {
                paint.shader = LinearGradient(left, 56f, left, size - 44f, Color.rgb(250, 210, 142), Color.rgb(154, 112, 68), Shader.TileMode.CLAMP)
                canvas.drawRect(left, 56f, right, size - 44f, paint)
                paint.shader = null
                // Display window sill
                paint.color = Color.rgb(88, 84, 92)
                canvas.drawRect(left - 4f, size - 44f, right + 4f, size - 34f, paint)
            }
        }
        // Upper dark sign board with glowing neon shop sign accents
        paint.color = Color.rgb(20, 22, 32)
        canvas.drawRect(0f, 0f, size.toFloat(), 44f, paint)
        paint.color = Color.rgb(0, 235, 255)
        canvas.drawRect(22f, 16f, size * 0.44f, 28f, paint)
        paint.color = Color.rgb(255, 56, 152)
        canvas.drawRect(size * 0.54f, 16f, size - 22f, 28f, paint)
        return uploadBitmap(bmp)
    }

    private fun createRoofTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        addAsphaltNoise(bmp, 52, 54, 62, 8, 707)
        val canvas = Canvas(bmp)
        val paint = Paint()
        paint.color = Color.argb(145, 92, 96, 108)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f
        canvas.drawRect(6f, 6f, size - 6f, size - 6f, paint)
        return uploadBitmap(bmp)
    }

    private fun createOceanTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        addAsphaltNoise(bmp, 16, 66, 106, 8, 808)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rng = Random(809)
        repeat(120) {
            val x = rng.nextFloat() * size
            val y = rng.nextFloat() * size
            val len = 14f + rng.nextFloat() * 28f
            paint.color = Color.argb(55, 145, 228, 255)
            paint.strokeWidth = 2.2f
            canvas.drawLine(x, y, x + len, y + 3f, paint)
        }
        return uploadBitmap(bmp)
    }

    private fun createSandTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        addAsphaltNoise(bmp, 192, 170, 136, 10, 909)
        return uploadBitmap(bmp)
    }

    private fun createCarMetallicTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        paint.shader = LinearGradient(
            0f,
            0f,
            size.toFloat(),
            size.toFloat(),
            Color.rgb(255, 255, 255),
            Color.rgb(214, 222, 236),
            Shader.TileMode.MIRROR,
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null
        // Crisp automotive door panel seams & trim reflection
        paint.color = Color.argb(58, 18, 22, 32)
        paint.strokeWidth = 2.5f
        canvas.drawLine(size * 0.46f, 0f, size * 0.46f, size.toFloat(), paint)
        canvas.drawLine(size * 0.78f, 0f, size * 0.78f, size.toFloat(), paint)
        paint.color = Color.argb(42, 255, 255, 255)
        canvas.drawLine(size * 0.46f + 3f, 0f, size * 0.46f + 3f, size.toFloat(), paint)
        return uploadBitmap(bmp)
    }

    private fun createTireWheelTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = size / 2f
        val cy = size / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Rubber tire base + subtle sidewall ring
        canvas.drawColor(Color.rgb(22, 24, 28))
        paint.color = Color.rgb(38, 41, 48)
        canvas.drawCircle(cx, cy, size * 0.47f, paint)
        paint.color = Color.rgb(22, 24, 28)
        canvas.drawCircle(cx, cy, size * 0.37f, paint)

        // Metallic alloy rim outer ring
        paint.color = Color.rgb(208, 216, 228)
        canvas.drawCircle(cx, cy, size * 0.31f, paint)

        // Dark brake rotor + red brake caliper hint
        paint.color = Color.rgb(44, 48, 56)
        canvas.drawCircle(cx, cy, size * 0.25f, paint)
        paint.color = Color.rgb(198, 42, 48)
        canvas.drawArc(
            cx - size * 0.22f,
            cy - size * 0.22f,
            cx + size * 0.22f,
            cy + size * 0.22f,
            -35f,
            60f,
            true,
            paint,
        )

        // 5-Spoke dual-split metallic alloy spokes
        paint.color = Color.rgb(228, 234, 244)
        paint.strokeWidth = 10f
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 5) {
            val a = (i * 2.0 * PI / 5.0).toFloat()
            canvas.drawLine(
                cx + cos(a) * 10f,
                cy + sin(a) * 10f,
                cx + cos(a) * (size * 0.29f),
                cy + sin(a) * (size * 0.29f),
                paint,
            )
        }
        // Center hubcap + lug nuts
        paint.color = Color.rgb(168, 176, 190)
        canvas.drawCircle(cx, cy, 14f, paint)
        return uploadBitmap(bmp, repeat = false)
    }

    private fun createPalmBarkTexture(): Int {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        addAsphaltNoise(bmp, 122, 92, 68, 10, 930)
        val canvas = Canvas(bmp)
        val paint = Paint()
        for (y in 8 until size step 14) {
            paint.color = Color.argb(105, 58, 42, 28)
            canvas.drawRect(0f, y.toFloat(), size.toFloat(), y + 4f, paint)
            paint.color = Color.argb(55, 175, 142, 108)
            canvas.drawRect(0f, y + 4f, size.toFloat(), y + 6f, paint)
        }
        return uploadBitmap(bmp)
    }

    private fun createPalmFrondTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.TRANSPARENT)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Central spine
        paint.color = Color.rgb(86, 162, 64)
        paint.strokeWidth = 9f
        canvas.drawLine(size / 2f, 0f, size / 2f, size.toFloat(), paint)

        // Realistic serrated palm leaflets along spine
        val leaflets = 18
        for (i in 0 until leaflets) {
            val y = 12f + (i / leaflets.toFloat()) * (size - 22f)
            val span = (1f - kotlin.math.abs(i / leaflets.toFloat() - 0.38f) * 1.15f).coerceIn(0.28f, 1f) * (size * 0.46f)
            paint.color = if (i % 2 == 0) Color.rgb(48, 138, 56) else Color.rgb(66, 158, 70)
            paint.strokeWidth = 9f
            canvas.drawLine(size / 2f, y, size / 2f - span, y + 18f, paint)
            canvas.drawLine(size / 2f, y, size / 2f + span, y + 18f, paint)
        }
        return uploadBitmap(bmp, repeat = false)
    }

    private fun createHumanFaceTexture(): Int {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(245, 240, 236))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val fx = size * 0.50f
        val eyeY = size * 0.44f
        paint.color = Color.rgb(68, 52, 46)
        paint.strokeWidth = 3.2f
        canvas.drawLine(fx - 14f, eyeY - 7f, fx - 5f, eyeY - 6f, paint)
        canvas.drawLine(fx + 5f, eyeY - 6f, fx + 14f, eyeY - 7f, paint)
        paint.color = Color.rgb(248, 248, 250)
        canvas.drawCircle(fx - 9f, eyeY, 3.6f, paint)
        canvas.drawCircle(fx + 9f, eyeY, 3.6f, paint)
        paint.color = Color.rgb(34, 30, 32)
        canvas.drawCircle(fx - 9f, eyeY, 2.1f, paint)
        canvas.drawCircle(fx + 9f, eyeY, 2.1f, paint)
        return uploadBitmap(bmp, repeat = false)
    }

    private fun createCatFaceTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        // Soft fur-textured neutral base multiplied by cat coat uTintColor
        addAsphaltNoise(bmp, 244, 240, 236, 11, 955)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val fx = size * 0.50f
        val eyeY = size * 0.44f

        // Subtle warm cheek & brow shading
        paint.shader = RadialGradient(
            fx,
            eyeY + 26f,
            46f,
            Color.argb(120, 255, 252, 246),
            Color.argb(0, 255, 252, 246),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(fx, eyeY + 26f, 46f, paint)
        paint.shader = null

        // Crisp Forehead tabby "M" markings
        paint.color = Color.argb(125, 68, 48, 36)
        paint.strokeWidth = 5.5f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(fx - 22f, eyeY - 34f, fx - 12f, eyeY - 22f, paint)
        canvas.drawLine(fx, eyeY - 36f, fx, eyeY - 18f, paint)
        canvas.drawLine(fx + 22f, eyeY - 34f, fx + 12f, eyeY - 22f, paint)
        // Cheek tabby side stripes
        canvas.drawLine(fx - 46f, eyeY + 8f, fx - 30f, eyeY + 12f, paint)
        canvas.drawLine(fx + 46f, eyeY + 8f, fx + 30f, eyeY + 12f, paint)

        // Cream muzzle cheek pads
        paint.color = Color.argb(215, 255, 250, 242)
        canvas.drawCircle(fx - 10f, eyeY + 24f, 13f, paint)
        canvas.drawCircle(fx + 10f, eyeY + 24f, 13f, paint)

        // Expressive feline eyes: dark eyeliner rim, bright emerald-gold iris, crisp vertical slit pupil & catchlight
        paint.color = Color.rgb(34, 28, 26)
        canvas.drawCircle(fx - 20f, eyeY, 10.5f, paint)
        canvas.drawCircle(fx + 20f, eyeY, 10.5f, paint)

        paint.color = Color.rgb(126, 226, 114)
        canvas.drawCircle(fx - 20f, eyeY, 9.0f, paint)
        canvas.drawCircle(fx + 20f, eyeY, 9.0f, paint)

        paint.color = Color.rgb(18, 20, 22)
        canvas.drawOval(fx - 23f, eyeY - 7.5f, fx - 17f, eyeY + 7.5f, paint)
        canvas.drawOval(fx + 17f, eyeY - 7.5f, fx + 23f, eyeY + 7.5f, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(fx - 23f, eyeY - 3.5f, 2.4f, paint)
        canvas.drawCircle(fx + 17f, eyeY - 3.5f, 2.4f, paint)

        // Crisp pink triangular cat nose + soft highlight
        val nosePath = android.graphics.Path().apply {
            moveTo(fx - 8.5f, eyeY + 15f)
            lineTo(fx + 8.5f, eyeY + 15f)
            lineTo(fx, eyeY + 24f)
            close()
        }
        paint.color = Color.rgb(242, 132, 150)
        canvas.drawPath(nosePath, paint)

        // Mouth "w" curve + crisp whiskers
        paint.color = Color.rgb(72, 56, 52)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.2f
        canvas.drawLine(fx, eyeY + 24f, fx, eyeY + 29f, paint)
        canvas.drawLine(fx, eyeY + 29f, fx - 9f, eyeY + 33f, paint)
        canvas.drawLine(fx, eyeY + 29f, fx + 9f, eyeY + 33f, paint)

        paint.strokeWidth = 2.6f
        paint.color = Color.argb(215, 252, 248, 244)
        for (row in -1..1) {
            val wy = eyeY + 22f + row * 5.2f
            canvas.drawLine(fx - 13f, wy, fx - 44f, wy + row * 4.5f, paint)
            canvas.drawLine(fx + 13f, wy, fx + 44f, wy + row * 4.5f, paint)
        }
        paint.style = Paint.Style.FILL

        return uploadBitmap(bmp, repeat = false)
    }

    private fun createCatFurTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        addAsphaltNoise(bmp, 240, 236, 230, 14, 958)
        val canvas = Canvas(bmp)
        val paint = Paint()
        val rng = Random(959)
        // Fine directional fur hairlines
        for (i in 0 until 450) {
            val x = rng.nextFloat() * size
            val y = rng.nextFloat() * size
            paint.color = if (i % 2 == 0) Color.argb(36, 255, 252, 248) else Color.argb(38, 86, 62, 46)
            paint.strokeWidth = 1.8f
            canvas.drawLine(x, y, x + 2f, y + 10f, paint)
        }
        // Soft tabby stripes
        paint.color = Color.argb(46, 78, 56, 44)
        for (y in 8 until size step 32) {
            canvas.drawRect(0f, y.toFloat(), size.toFloat(), y + 9f, paint)
        }
        return uploadBitmap(bmp)
    }

    private fun createFabricTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        addAsphaltNoise(bmp, 238, 236, 238, 11, 960)
        val canvas = Canvas(bmp)
        val paint = Paint()
        paint.color = Color.argb(24, 48, 44, 42)
        for (y in 0 until size step 8) {
            canvas.drawLine(0f, y.toFloat(), size.toFloat(), y.toFloat(), paint)
            canvas.drawLine(y.toFloat(), 0f, y.toFloat(), size.toFloat(), paint)
        }
        // Subtle stitched jacket seam line
        paint.color = Color.argb(55, 80, 72, 66)
        paint.strokeWidth = 3f
        canvas.drawLine(size * 0.50f, 0f, size * 0.50f, size.toFloat(), paint)
        return uploadBitmap(bmp)
    }

    private fun createCashTexture(): Int {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(52, 146, 76))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(174, 224, 178)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        canvas.drawRect(8f, 16f, size - 8f, size - 16f, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(225, 206, 146)
        canvas.drawRect(size * 0.40f, 0f, size * 0.60f, size.toFloat(), paint)
        paint.color = Color.rgb(34, 96, 50)
        canvas.drawCircle(size * 0.50f, size * 0.50f, 16f, paint)
        return uploadBitmap(bmp, repeat = false)
    }

    private fun createMedkitTexture(): Int {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(238, 240, 244))
        val paint = Paint()
        paint.color = Color.rgb(212, 34, 46)
        canvas.drawRect(size * 0.42f, 22f, size * 0.58f, size - 22f, paint)
        canvas.drawRect(22f, size * 0.42f, size - 22f, size * 0.58f, paint)
        paint.color = Color.rgb(150, 158, 170)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        canvas.drawRect(3f, 3f, size - 3f, size - 3f, paint)
        return uploadBitmap(bmp, repeat = false)
    }

    private fun createBriefcaseTexture(): Int {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        addAsphaltNoise(bmp, 68, 48, 38, 6, 975)
        val canvas = Canvas(bmp)
        val paint = Paint()
        paint.color = Color.rgb(218, 182, 74)
        canvas.drawRect(24f, 12f, 40f, 26f, paint)
        canvas.drawRect(size - 40f, 12f, size - 24f, 26f, paint)
        paint.color = Color.rgb(36, 26, 20)
        canvas.drawRect(0f, 18f, size.toFloat(), 24f, paint)
        return uploadBitmap(bmp, repeat = false)
    }

    private fun createLightPoolTexture(): Int {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(
            size / 2f,
            size / 2f,
            size / 2f,
            intArrayOf(
                Color.argb(115, 255, 220, 158),
                Color.argb(48, 255, 198, 128),
                Color.argb(0, 255, 190, 120),
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return uploadBitmap(bmp, repeat = false)
    }
}
