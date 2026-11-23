package com.example.domain.engine

import androidx.compose.runtime.Immutable
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Immutable
data class PhysicsCardBody(
    val photoId: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val width: Float,
    val height: Float,
    val angleDeg: Float,
    val angularVel: Float,
    val zOrder: Int
)

class Physics2DEngine {
    private var bodies = mutableListOf<PhysicsCardBody>()
    private var containerW = 0f
    private var containerH = 0f
    private var draggedId: Long? = null

    fun syncBodies(
        photoIdsAndRatios: List<Pair<Long, Float>>,
        widthPx: Float,
        heightPx: Float,
        cardBaseWidthPx: Float
    ): List<PhysicsCardBody> {
        if (widthPx <= 10f || heightPx <= 10f) return bodies
        containerW = widthPx
        containerH = heightPx

        val existingMap = bodies.associateBy { it.photoId }
        val rng = Random(99)
        val updated = mutableListOf<PhysicsCardBody>()

        photoIdsAndRatios.forEachIndexed { idx, (id, ratio) ->
            val existing = existingMap[id]
            if (existing != null) {
                updated.add(existing)
            } else {
                val cw = cardBaseWidthPx
                val ch = (cw / ratio.coerceIn(0.65f, 1.45f)).coerceIn(cw * 0.7f, cw * 1.45f)
                val cols = 3
                val col = idx % cols
                val row = idx / cols
                val startX = (widthPx * (0.22f + col * 0.28f) + rng.nextFloat() * 40f - 20f)
                    .coerceIn(cw * 0.55f, widthPx - cw * 0.55f)
                val startY = (heightPx * 0.20f + row * (ch * 0.68f))
                    .coerceIn(ch * 0.55f, heightPx * 0.72f)
                updated.add(
                    PhysicsCardBody(
                        photoId = id,
                        x = startX,
                        y = startY,
                        vx = (rng.nextFloat() - 0.5f) * 260f,
                        vy = rng.nextFloat() * 120f,
                        width = cw,
                        height = ch,
                        angleDeg = (rng.nextFloat() - 0.5f) * 22f,
                        angularVel = (rng.nextFloat() - 0.5f) * 30f,
                        zOrder = idx
                    )
                )
            }
        }
        bodies = updated
        return bodies.toList()
    }

    fun step(
        dtSec: Float,
        gravityX: Float,
        gravityY: Float,
        zeroGravity: Boolean
    ): List<PhysicsCardBody> {
        if (containerW <= 10f || containerH <= 10f || bodies.isEmpty()) return bodies

        val dt = dtSec.coerceIn(0.001f, 0.033f)
        val damping = if (zeroGravity) 0.994f else 0.985f
        val angularDamping = 0.96f
        val restitution = 0.74f
        val topWall = 24f
        val bottomWall = max(topWall + 200f, containerH - 24f)

        // 1. Integrate forces & velocities
        for (i in bodies.indices) {
            var b = bodies[i]
            if (b.photoId == draggedId) continue

            val gx = if (zeroGravity) gravityX * 0.15f else gravityX
            val gy = if (zeroGravity) gravityY * 0.08f else gravityY

            var vx = (b.vx + gx * dt) * damping
            var vy = (b.vy + gy * dt) * damping
            var av = b.angularVel * angularDamping

            var nx = b.x + vx * dt
            var ny = b.y + vy * dt
            var angle = (b.angleDeg + av * dt)

            val halfW = b.width * 0.5f
            val halfH = b.height * 0.5f

            if (nx - halfW < 12f) {
                nx = 12f + halfW
                vx = -vx * restitution
                av += vy * 0.05f
            } else if (nx + halfW > containerW - 12f) {
                nx = containerW - 12f - halfW
                vx = -vx * restitution
                av -= vy * 0.05f
            }

            if (ny - halfH < topWall) {
                ny = topWall + halfH
                vy = -vy * restitution
                av += vx * 0.04f
            } else if (ny + halfH > bottomWall) {
                ny = bottomWall - halfH
                vy = -vy * restitution
                vx *= 0.92f
                av *= 0.85f
                if (kotlin.math.abs(vy) < 28f && !zeroGravity) vy = 0f
            }

            bodies[i] = b.copy(
                x = nx,
                y = ny,
                vx = vx,
                vy = vy,
                angleDeg = angle.coerceIn(-45f, 45f),
                angularVel = av
            )
        }

        // 2. Pairwise elastic collision resolution (bounding circle approximation for smooth 60fps card bounce)
        for (i in 0 until bodies.size) {
            for (j in i + 1 until bodies.size) {
                val a = bodies[i]
                val b = bodies[j]
                val ra = min(a.width, a.height) * 0.44f
                val rb = min(b.width, b.height) * 0.44f
                val minDist = ra + rb

                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = hypot(dx, dy)
                if (dist > 0.001f && dist < minDist) {
                    val nx = dx / dist
                    val ny = dy / dist
                    val overlap = minDist - dist

                    val aPinned = a.photoId == draggedId
                    val bPinned = b.photoId == draggedId

                    val axShift = if (aPinned) 0f else if (bPinned) -overlap else -overlap * 0.5f
                    val bxShift = if (bPinned) 0f else if (aPinned) overlap else overlap * 0.5f

                    val rvx = b.vx - a.vx
                    val rvy = b.vy - a.vy
                    val velAlongNormal = rvx * nx + rvy * ny

                    var avx = a.vx
                    var avy = a.vy
                    var bvx = b.vx
                    var bvy = b.vy
                    var aSpin = a.angularVel
                    var bSpin = b.angularVel

                    if (velAlongNormal < 0f) {
                        val impulse = -(1f + restitution) * velAlongNormal * 0.5f
                        if (!aPinned) {
                            avx -= impulse * nx
                            avy -= impulse * ny
                            aSpin += (nx * rvy - ny * rvx) * 0.04f
                        }
                        if (!bPinned) {
                            bvx += impulse * nx
                            bvy += impulse * ny
                            bSpin -= (nx * rvy - ny * rvx) * 0.04f
                        }
                    }

                    bodies[i] = a.copy(
                        x = a.x + nx * axShift,
                        y = a.y + ny * axShift,
                        vx = avx,
                        vy = avy,
                        angularVel = aSpin
                    )
                    bodies[j] = b.copy(
                        x = b.x + nx * bxShift,
                        y = b.y + ny * bxShift,
                        vx = bvx,
                        vy = bvy,
                        angularVel = bSpin
                    )
                }
            }
        }

        return bodies.toList()
    }

    fun startDrag(photoId: Long) {
        draggedId = photoId
        val maxZ = (bodies.maxOfOrNull { it.zOrder } ?: 0) + 1
        val idx = bodies.indexOfFirst { it.photoId == photoId }
        if (idx >= 0) {
            bodies[idx] = bodies[idx].copy(zOrder = maxZ, vx = 0f, vy = 0f, angularVel = 0f)
        }
    }

    fun dragBy(photoId: Long, dx: Float, dy: Float) {
        val idx = bodies.indexOfFirst { it.photoId == photoId }
        if (idx >= 0) {
            val b = bodies[idx]
            val nx = (b.x + dx).coerceIn(b.width * 0.35f, max(b.width, containerW - b.width * 0.35f))
            val ny = (b.y + dy).coerceIn(b.height * 0.35f, max(b.height, containerH - b.height * 0.35f))
            bodies[idx] = b.copy(
                x = nx,
                y = ny,
                vx = (dx * 32f).coerceIn(-2200f, 2200f),
                vy = (dy * 32f).coerceIn(-2200f, 2200f),
                angleDeg = (b.angleDeg + dx * 0.08f).coerceIn(-35f, 35f)
            )
        }
    }

    fun endDrag(photoId: Long) {
        if (draggedId == photoId) {
            draggedId = null
        }
    }

    fun impulseBurst() {
        val rng = Random(System.currentTimeMillis())
        for (i in bodies.indices) {
            val b = bodies[i]
            bodies[i] = b.copy(
                vx = (rng.nextFloat() - 0.5f) * 1800f,
                vy = -850f - rng.nextFloat() * 1100f,
                angularVel = (rng.nextFloat() - 0.5f) * 180f
            )
        }
    }

    fun tidyStack() {
        if (containerW <= 10f || containerH <= 10f) return
        val cols = 2
        val total = bodies.size
        for (i in bodies.indices) {
            val b = bodies[i]
            val col = i % cols
            val row = i / cols
            val rows = max(1, (total + 1) / cols)
            val tx = containerW * (0.29f + col * 0.42f)
            val ty = containerH * (0.24f + (row.toFloat() / max(1, rows - 1)) * 0.48f)
            bodies[i] = b.copy(
                x = tx,
                y = ty,
                vx = 0f,
                vy = 0f,
                angleDeg = (if (i % 2 == 0) -1 else 1) * (3f + (i % 4) * 2f),
                angularVel = 0f
            )
        }
    }
}
