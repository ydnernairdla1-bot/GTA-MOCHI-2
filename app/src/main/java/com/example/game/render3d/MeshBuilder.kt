package com.example.game.render3d

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class GpuMesh(
    val vboId: Int,
    val vertexCount: Int,
) {
    fun bindAndDraw(shader: LitShader) {
        if (vertexCount <= 0 || vboId == 0) return
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        val stride = FLOATS_PER_VERTEX * 4
        GLES20.glEnableVertexAttribArray(shader.aPosition)
        GLES20.glVertexAttribPointer(shader.aPosition, 3, GLES20.GL_FLOAT, false, stride, 0)

        GLES20.glEnableVertexAttribArray(shader.aNormal)
        GLES20.glVertexAttribPointer(shader.aNormal, 3, GLES20.GL_FLOAT, false, stride, 3 * 4)

        GLES20.glEnableVertexAttribArray(shader.aTexCoord)
        GLES20.glVertexAttribPointer(shader.aTexCoord, 2, GLES20.GL_FLOAT, false, stride, 6 * 4)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
    }

    fun release() {
        if (vboId != 0) {
            GLES20.glDeleteBuffers(1, intArrayOf(vboId), 0)
        }
    }

    companion object {
        const val FLOATS_PER_VERTEX = 8 // x, y, z, nx, ny, nz, u, v
    }
}

class MeshBuilder(initialCapacityFloats: Int = 4096) {
    private var data = FloatArray(initialCapacityFloats)
    private var count = 0

    private fun ensureCapacity(extra: Int) {
        if (count + extra <= data.size) return
        var newCap = data.size * 2
        while (newCap < count + extra) newCap *= 2
        data = data.copyOf(newCap)
    }

    fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, v: Float) {
        ensureCapacity(8)
        data[count++] = x
        data[count++] = y
        data[count++] = z
        data[count++] = nx
        data[count++] = ny
        data[count++] = nz
        data[count++] = u
        data[count++] = v
    }

    fun addQuad(
        x0: Float, y0: Float, z0: Float, u0: Float, v0: Float,
        x1: Float, y1: Float, z1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, z2: Float, u2: Float, v2: Float,
        x3: Float, y3: Float, z3: Float, u3: Float, v3: Float,
        nx: Float, ny: Float, nz: Float,
    ) {
        addVertex(x0, y0, z0, nx, ny, nz, u0, v0)
        addVertex(x1, y1, z1, nx, ny, nz, u1, v1)
        addVertex(x2, y2, z2, nx, ny, nz, u2, v2)

        addVertex(x0, y0, z0, nx, ny, nz, u0, v0)
        addVertex(x2, y2, z2, nx, ny, nz, u2, v2)
        addVertex(x3, y3, z3, nx, ny, nz, u3, v3)
    }

    fun addBox(
        cx: Float,
        cy: Float,
        cz: Float,
        sx: Float,
        sy: Float,
        sz: Float,
        uvScaleX: Float = 1f,
        uvScaleY: Float = 1f,
        includeBottom: Boolean = false,
        includeTop: Boolean = true,
    ) {
        val x0 = cx - sx / 2f
        val x1 = cx + sx / 2f
        val y0 = cy - sy / 2f
        val y1 = cy + sy / 2f
        val z0 = cz - sz / 2f
        val z1 = cz + sz / 2f

        // +Z Front
        addQuad(
            x0, y0, z1, 0f, uvScaleY,
            x1, y0, z1, uvScaleX, uvScaleY,
            x1, y1, z1, uvScaleX, 0f,
            x0, y1, z1, 0f, 0f,
            0f, 0f, 1f,
        )
        // -Z Back
        addQuad(
            x1, y0, z0, 0f, uvScaleY,
            x0, y0, z0, uvScaleX, uvScaleY,
            x0, y1, z0, uvScaleX, 0f,
            x1, y1, z0, 0f, 0f,
            0f, 0f, -1f,
        )
        // +X Right
        addQuad(
            x1, y0, z1, 0f, uvScaleY,
            x1, y0, z0, uvScaleX, uvScaleY,
            x1, y1, z0, uvScaleX, 0f,
            x1, y1, z1, 0f, 0f,
            1f, 0f, 0f,
        )
        // -X Left
        addQuad(
            x0, y0, z0, 0f, uvScaleY,
            x0, y0, z1, uvScaleX, uvScaleY,
            x0, y1, z1, uvScaleX, 0f,
            x0, y1, z0, 0f, 0f,
            -1f, 0f, 0f,
        )
        if (includeTop) {
            addQuad(
                x0, y1, z1, 0f, 1f,
                x1, y1, z1, 1f, 1f,
                x1, y1, z0, 1f, 0f,
                x0, y1, z0, 0f, 0f,
                0f, 1f, 0f,
            )
        }
        if (includeBottom) {
            addQuad(
                x0, y0, z0, 0f, 0f,
                x1, y0, z0, 1f, 0f,
                x1, y0, z1, 1f, 1f,
                x0, y0, z1, 0f, 1f,
                0f, -1f, 0f,
            )
        }
    }

    fun addSlopedBox(
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float,
        halfZBottom: Float,
        halfZTop: Float,
        frontSlopeX: Float,
        rearSlopeX: Float,
    ) {
        val tx0 = minX + rearSlopeX
        val tx1 = maxX - frontSlopeX
        addQuad(
            tx0, maxY, halfZTop, 0f, 1f,
            tx1, maxY, halfZTop, 1f, 1f,
            tx1, maxY, -halfZTop, 1f, 0f,
            tx0, maxY, -halfZTop, 0f, 0f,
            0f, 1f, 0f,
        )
        val fnx = (maxY - minY)
        val fny = frontSlopeX
        val fl = kotlin.math.hypot(fnx, fny).coerceAtLeast(0.001f)
        addQuad(
            maxX, minY, halfZBottom, 0f, 1f,
            maxX, minY, -halfZBottom, 1f, 1f,
            tx1, maxY, -halfZTop, 1f, 0f,
            tx1, maxY, halfZTop, 0f, 0f,
            fnx / fl, fny / fl, 0f,
        )
        val rnx = -(maxY - minY)
        val rny = rearSlopeX
        val rl = kotlin.math.hypot(rnx, rny).coerceAtLeast(0.001f)
        addQuad(
            minX, minY, -halfZBottom, 0f, 1f,
            minX, minY, halfZBottom, 1f, 1f,
            tx0, maxY, halfZTop, 1f, 0f,
            tx0, maxY, -halfZTop, 0f, 0f,
            rnx / rl, rny / rl, 0f,
        )
        addQuad(
            minX, minY, halfZBottom, 0f, 1f,
            maxX, minY, halfZBottom, 1f, 1f,
            tx1, maxY, halfZTop, 1f, 0f,
            tx0, maxY, halfZTop, 0f, 0f,
            0f, 0.25f, 0.96f,
        )
        addQuad(
            maxX, minY, -halfZBottom, 0f, 1f,
            minX, minY, -halfZBottom, 1f, 1f,
            tx0, maxY, -halfZTop, 1f, 0f,
            tx1, maxY, -halfZTop, 0f, 0f,
            0f, 0.25f, -0.96f,
        )
    }

    /**
     * Smooth tapered cylindrical limb extending downward from origin (y=0 down to y=-length).
     */
    fun addTaperedLimb(
        radiusTop: Float,
        radiusBottom: Float,
        length: Float,
        segments: Int = 10,
    ) {
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val nx = cos((a0 + a1) * 0.5f)
            val nz = sin((a0 + a1) * 0.5f)
            addQuad(
                c0 * radiusBottom, -length, s0 * radiusBottom, 0f, 1f,
                c1 * radiusBottom, -length, s1 * radiusBottom, 1f, 1f,
                c1 * radiusTop, 0f, s1 * radiusTop, 1f, 0f,
                c0 * radiusTop, 0f, s0 * radiusTop, 0f, 0f,
                nx, 0.05f, nz,
            )
            // Top & bottom caps
            addVertex(0f, 0f, 0f, 0f, 1f, 0f, 0.5f, 0.5f)
            addVertex(c1 * radiusTop, 0f, s1 * radiusTop, 0f, 1f, 0f, 0.5f, 0.5f)
            addVertex(c0 * radiusTop, 0f, s0 * radiusTop, 0f, 1f, 0f, 0.5f, 0.5f)

            addVertex(0f, -length, 0f, 0f, -1f, 0f, 0.5f, 0.5f)
            addVertex(c0 * radiusBottom, -length, s0 * radiusBottom, 0f, -1f, 0f, 0.5f, 0.5f)
            addVertex(c1 * radiusBottom, -length, s1 * radiusBottom, 0f, -1f, 0f, 0.5f, 0.5f)
        }
    }

    /**
     * Realistic wheel cylinder along Z axis with hubcap UVs mapped [0..1] onto alloy wheel texture
     * and tire tread mapped to outer edge [0..0.1] dark rubber area.
     */
    fun addWheelCylinder(radius: Float, width: Float, segments: Int = 16) {
        val hz = width / 2f
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val x0 = c0 * radius; val y0 = s0 * radius
            val x1 = c1 * radius; val y1 = s1 * radius
            val nx = cos((a0 + a1) * 0.5f)
            val ny = sin((a0 + a1) * 0.5f)
            // Tread maps to rubber corner (0.04, 0.04)
            addQuad(
                x0, y0, hz, 0.02f, 0.02f,
                x1, y1, hz, 0.08f, 0.02f,
                x1, y1, -hz, 0.08f, 0.08f,
                x0, y0, -hz, 0.02f, 0.08f,
                nx, ny, 0f,
            )
            // +Z Hubcap / Alloy rim
            addVertex(0f, 0f, hz, 0f, 0f, 1f, 0.5f, 0.5f)
            addVertex(x0, y0, hz, 0f, 0f, 1f, 0.5f + c0 * 0.48f, 0.5f + s0 * 0.48f)
            addVertex(x1, y1, hz, 0f, 0f, 1f, 0.5f + c1 * 0.48f, 0.5f + s1 * 0.48f)
            // -Z Hubcap / Alloy rim
            addVertex(0f, 0f, -hz, 0f, 0f, -1f, 0.5f, 0.5f)
            addVertex(x1, y1, -hz, 0f, 0f, -1f, 0.5f + c1 * 0.48f, 0.5f + s1 * 0.48f)
            addVertex(x0, y0, -hz, 0f, 0f, -1f, 0.5f + c0 * 0.48f, 0.5f + s0 * 0.48f)
        }
    }

    fun addVerticalCylinder(
        cx: Float,
        yBottom: Float,
        yTop: Float,
        cz: Float,
        rBottom: Float,
        rTop: Float,
        segments: Int = 10,
        uvScaleY: Float = 1f,
    ) {
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val nx = cos((a0 + a1) * 0.5f)
            val nz = sin((a0 + a1) * 0.5f)
            addQuad(
                cx + c0 * rBottom, yBottom, cz + s0 * rBottom, 0f, uvScaleY,
                cx + c1 * rBottom, yBottom, cz + s1 * rBottom, 1f, uvScaleY,
                cx + c1 * rTop, yTop, cz + s1 * rTop, 1f, 0f,
                cx + c0 * rTop, yTop, cz + s0 * rTop, 0f, 0f,
                nx, 0.1f, nz,
            )
        }
    }

    /**
     * Sphere with U = 0.5 facing +X (character forward direction!) so face textures align cleanly.
     */
    fun addSphere(radius: Float, radiusY: Float = radius, stacks: Int = 10, slices: Int = 14) {
        for (i in 0 until stacks) {
            val phi0 = (PI * i / stacks).toFloat()
            val phi1 = (PI * (i + 1) / stacks).toFloat()
            val v0 = i / stacks.toFloat()
            val v1 = (i + 1) / stacks.toFloat()
            for (j in 0 until slices) {
                // Shift theta by -PI so j = slices/2 (u=0.5) faces +X (theta=0)!
                val u0 = j / slices.toFloat()
                val u1 = (j + 1) / slices.toFloat()
                val theta0 = ((u0 - 0.5f) * 2.0 * PI).toFloat()
                val theta1 = ((u1 - 0.5f) * 2.0 * PI).toFloat()

                val x00 = sin(phi0) * cos(theta0); val y00 = cos(phi0); val z00 = sin(phi0) * sin(theta0)
                val x10 = sin(phi1) * cos(theta0); val y10 = cos(phi1); val z10 = sin(phi1) * sin(theta0)
                val x11 = sin(phi1) * cos(theta1); val y11 = cos(phi1); val z11 = sin(phi1) * sin(theta1)
                val x01 = sin(phi0) * cos(theta1); val y01 = cos(phi0); val z01 = sin(phi0) * sin(theta1)

                addVertex(x00 * radius, y00 * radiusY, z00 * radius, x00, y00, z00, u0, v0)
                addVertex(x10 * radius, y10 * radiusY, z10 * radius, x10, y10, z10, u0, v1)
                addVertex(x11 * radius, y11 * radiusY, z11 * radius, x11, y11, z11, u1, v1)

                addVertex(x00 * radius, y00 * radiusY, z00 * radius, x00, y00, z00, u0, v0)
                addVertex(x11 * radius, y11 * radiusY, z11 * radius, x11, y11, z11, u1, v1)
                addVertex(x01 * radius, y01 * radiusY, z01 * radius, x01, y01, z01, u1, v0)
            }
        }
    }

    fun build(): GpuMesh {
        val vertexCount = count / GpuMesh.FLOATS_PER_VERTEX
        if (vertexCount == 0) return GpuMesh(0, 0)
        val buffer: FloatBuffer = ByteBuffer.allocateDirect(count * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(data, 0, count)
        buffer.position(0)

        val vboIds = IntArray(1)
        GLES20.glGenBuffers(1, vboIds, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[0])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, count * 4, buffer, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        return GpuMesh(vboIds[0], vertexCount)
    }
}
