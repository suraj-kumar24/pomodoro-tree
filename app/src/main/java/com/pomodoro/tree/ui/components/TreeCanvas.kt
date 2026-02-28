package com.pomodoro.tree.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.lerp
import com.pomodoro.tree.ui.theme.LeafGreen
import com.pomodoro.tree.ui.theme.LeafOrange
import com.pomodoro.tree.ui.theme.LeafYellow
import com.pomodoro.tree.ui.theme.TrunkBrown
import com.pomodoro.tree.ui.theme.WiltedGray
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a tree at the given growth progress (0.0 to 1.0).
 *
 * Growth stages:
 *  0.0 - 0.10: Seed in soil
 *  0.10 - 0.30: Small sprout with first leaves
 *  0.30 - 0.60: Young sapling
 *  0.60 - 0.90: Growing tree with branches
 *  0.90 - 1.00: Full tree with canopy
 *
 * @param progress Timer progress 0.0 to 1.0
 * @param wiltProgress Overtime wilting 0.0 (fresh) to 1.0 (fully wilted)
 * @param isWithered True for cancelled sessions (dead gray tree)
 * @param modifier Composable modifier
 */
@Composable
fun TreeCanvas(
    progress: Float,
    wiltProgress: Float = 0f,
    isWithered: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "treeGrowth"
    )

    val animatedWilt by animateFloatAsState(
        targetValue = wiltProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1500),
        label = "treeWilt"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val cx = size.width / 2f
        val groundY = size.height * 0.85f

        if (isWithered) {
            drawWitheredTree(cx, groundY)
        } else {
            // Draw ground line
            drawGroundLine(cx, groundY)

            when {
                animatedProgress < 0.10f -> drawSeed(cx, groundY, animatedProgress / 0.10f)
                animatedProgress < 0.30f -> drawSprout(cx, groundY, (animatedProgress - 0.10f) / 0.20f, animatedWilt)
                animatedProgress < 0.60f -> drawSapling(cx, groundY, (animatedProgress - 0.30f) / 0.30f, animatedWilt)
                animatedProgress < 0.90f -> drawGrowingTree(cx, groundY, (animatedProgress - 0.60f) / 0.30f, animatedWilt)
                else -> drawFullTree(cx, groundY, (animatedProgress - 0.90f) / 0.10f, animatedWilt)
            }
        }
    }
}

/**
 * Miniature tree for forest grid and widget. No animation.
 */
@Composable
fun MiniTreeCanvas(
    isCompleted: Boolean,
    wiltProgress: Float = 0f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val cx = size.width / 2f
        val groundY = size.height * 0.88f

        if (!isCompleted) {
            drawWitheredTree(cx, groundY)
        } else {
            drawFullTree(cx, groundY, stageProgress = 1f, wiltProgress = wiltProgress)
        }
    }
}

// --- Drawing helpers ---

private fun DrawScope.drawGroundLine(cx: Float, groundY: Float) {
    val groundWidth = size.width * 0.4f
    drawLine(
        color = TrunkBrown.copy(alpha = 0.3f),
        start = Offset(cx - groundWidth / 2, groundY),
        end = Offset(cx + groundWidth / 2, groundY),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawSeed(cx: Float, groundY: Float, stageProgress: Float) {
    // Small oval seed sitting on the ground
    val seedSize = size.width * 0.04f * (0.5f + stageProgress * 0.5f)
    val seedY = groundY - seedSize

    drawOval(
        color = TrunkBrown,
        topLeft = Offset(cx - seedSize, seedY - seedSize * 0.6f),
        size = androidx.compose.ui.geometry.Size(seedSize * 2, seedSize * 1.2f)
    )

    // Tiny green sprout tip emerging as progress increases
    if (stageProgress > 0.5f) {
        val sproutHeight = seedSize * (stageProgress - 0.5f) * 2f
        drawLine(
            color = LeafGreen,
            start = Offset(cx, seedY - seedSize * 0.3f),
            end = Offset(cx, seedY - seedSize * 0.3f - sproutHeight),
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawSprout(cx: Float, groundY: Float, stageProgress: Float, wiltProgress: Float) {
    val leafColor = lerpLeafColor(wiltProgress)

    // Stem grows upward
    val stemHeight = size.height * 0.08f + size.height * 0.07f * stageProgress
    val stemTop = groundY - stemHeight

    drawLine(
        color = TrunkBrown,
        start = Offset(cx, groundY),
        end = Offset(cx, stemTop),
        strokeWidth = 3f
    )

    // Two small leaves
    val leafSize = size.width * 0.03f + size.width * 0.02f * stageProgress
    drawLeaf(cx - leafSize * 0.5f, stemTop + stemHeight * 0.3f, leafSize, -30f, leafColor)
    drawLeaf(cx + leafSize * 0.5f, stemTop + stemHeight * 0.3f, leafSize, 30f, leafColor)

    // Top bud / tiny leaf
    drawCircle(
        color = leafColor,
        radius = leafSize * 0.5f,
        center = Offset(cx, stemTop)
    )
}

private fun DrawScope.drawSapling(cx: Float, groundY: Float, stageProgress: Float, wiltProgress: Float) {
    val leafColor = lerpLeafColor(wiltProgress)

    // Trunk — thicker and taller
    val trunkHeight = size.height * 0.18f + size.height * 0.08f * stageProgress
    val trunkTop = groundY - trunkHeight
    val trunkWidth = size.width * 0.015f + size.width * 0.005f * stageProgress

    drawLine(
        color = TrunkBrown,
        start = Offset(cx, groundY),
        end = Offset(cx, trunkTop),
        strokeWidth = trunkWidth * 2
    )

    // Small branches
    val branchY1 = trunkTop + trunkHeight * 0.4f
    val branchLen = size.width * 0.06f * (0.5f + stageProgress * 0.5f)

    drawLine(color = TrunkBrown, start = Offset(cx, branchY1), end = Offset(cx - branchLen, branchY1 - branchLen * 0.6f), strokeWidth = 2f)
    drawLine(color = TrunkBrown, start = Offset(cx, branchY1), end = Offset(cx + branchLen, branchY1 - branchLen * 0.5f), strokeWidth = 2f)

    // Leaf clusters at branch tips + top
    val clusterRadius = size.width * 0.04f + size.width * 0.02f * stageProgress
    drawCircle(color = leafColor, radius = clusterRadius, center = Offset(cx - branchLen, branchY1 - branchLen * 0.6f))
    drawCircle(color = leafColor, radius = clusterRadius, center = Offset(cx + branchLen, branchY1 - branchLen * 0.5f))
    drawCircle(color = leafColor, radius = clusterRadius * 1.2f, center = Offset(cx, trunkTop))
}

private fun DrawScope.drawGrowingTree(cx: Float, groundY: Float, stageProgress: Float, wiltProgress: Float) {
    val leafColor = lerpLeafColor(wiltProgress)

    // Thicker trunk
    val trunkHeight = size.height * 0.28f + size.height * 0.06f * stageProgress
    val trunkTop = groundY - trunkHeight
    val trunkWidth = size.width * 0.025f + size.width * 0.008f * stageProgress

    // Trunk as a tapered path
    val trunkPath = Path().apply {
        moveTo(cx - trunkWidth, groundY)
        lineTo(cx + trunkWidth, groundY)
        lineTo(cx + trunkWidth * 0.6f, trunkTop)
        lineTo(cx - trunkWidth * 0.6f, trunkTop)
        close()
    }
    drawPath(trunkPath, TrunkBrown, style = Fill)

    // Multiple branches
    val branches = listOf(
        Triple(0.55f, -35f, 0.7f),
        Triple(0.45f, 25f, 0.8f),
        Triple(0.30f, -20f, 0.6f),
        Triple(0.25f, 30f, 0.65f)
    )

    for ((heightFrac, angle, lenFrac) in branches) {
        val by = trunkTop + trunkHeight * heightFrac
        val branchLen = size.width * 0.08f * lenFrac * (0.6f + stageProgress * 0.4f)
        val rad = Math.toRadians(angle.toDouble())
        val bx = cx + branchLen * cos(rad).toFloat()
        val bey = by - branchLen * sin(rad).toFloat()

        drawLine(color = TrunkBrown, start = Offset(cx, by), end = Offset(bx, bey), strokeWidth = 3f)

        // Leaf cluster at branch end
        val clusterSize = size.width * 0.05f + size.width * 0.02f * stageProgress
        drawCircle(color = leafColor, radius = clusterSize, center = Offset(bx, bey))
    }

    // Canopy forming at top
    val canopyRadius = size.width * 0.10f + size.width * 0.06f * stageProgress
    drawCircle(color = leafColor, radius = canopyRadius, center = Offset(cx, trunkTop + canopyRadius * 0.2f))
}

private fun DrawScope.drawFullTree(cx: Float, groundY: Float, stageProgress: Float, wiltProgress: Float) {
    val leafColor = lerpLeafColor(wiltProgress)

    // Full thick trunk
    val trunkHeight = size.height * 0.35f
    val trunkTop = groundY - trunkHeight
    val trunkWidth = size.width * 0.035f

    val trunkPath = Path().apply {
        moveTo(cx - trunkWidth, groundY)
        lineTo(cx + trunkWidth, groundY)
        lineTo(cx + trunkWidth * 0.5f, trunkTop + trunkHeight * 0.15f)
        lineTo(cx - trunkWidth * 0.5f, trunkTop + trunkHeight * 0.15f)
        close()
    }
    drawPath(trunkPath, TrunkBrown, style = Fill)

    // Branches (visible under canopy slightly)
    val branches = listOf(
        Triple(0.45f, -40f, 0.9f),
        Triple(0.40f, 35f, 0.85f),
        Triple(0.30f, -25f, 0.7f),
        Triple(0.25f, 28f, 0.75f),
        Triple(0.20f, -15f, 0.5f)
    )

    for ((heightFrac, angle, lenFrac) in branches) {
        val by = trunkTop + trunkHeight * heightFrac
        val branchLen = size.width * 0.10f * lenFrac
        val rad = Math.toRadians(angle.toDouble())
        val bx = cx + branchLen * cos(rad).toFloat()
        val bey = by - branchLen * sin(rad).toFloat()

        drawLine(color = TrunkBrown.copy(alpha = 0.7f), start = Offset(cx, by), end = Offset(bx, bey), strokeWidth = 3f)
    }

    // Full canopy — overlapping circles for organic shape
    val canopyBase = trunkTop + trunkHeight * 0.15f
    val r = size.width * 0.14f + size.width * 0.02f * stageProgress

    drawCircle(color = leafColor, radius = r, center = Offset(cx, canopyBase))
    drawCircle(color = leafColor, radius = r * 0.85f, center = Offset(cx - r * 0.5f, canopyBase + r * 0.15f))
    drawCircle(color = leafColor, radius = r * 0.85f, center = Offset(cx + r * 0.5f, canopyBase + r * 0.15f))
    drawCircle(color = leafColor, radius = r * 0.75f, center = Offset(cx - r * 0.25f, canopyBase - r * 0.5f))
    drawCircle(color = leafColor, radius = r * 0.75f, center = Offset(cx + r * 0.25f, canopyBase - r * 0.5f))
    drawCircle(color = leafColor.copy(alpha = 0.8f), radius = r * 0.6f, center = Offset(cx, canopyBase - r * 0.7f))

    // Falling leaves during wilting
    if (wiltProgress > 0.3f) {
        val fallenCount = ((wiltProgress - 0.3f) / 0.7f * 5).toInt().coerceAtMost(5)
        val leafPositions = listOf(
            Offset(cx - r * 0.8f, groundY - size.height * 0.05f),
            Offset(cx + r * 0.6f, groundY - size.height * 0.03f),
            Offset(cx - r * 0.3f, groundY - size.height * 0.02f),
            Offset(cx + r * 0.9f, groundY - size.height * 0.06f),
            Offset(cx - r * 1.0f, groundY - size.height * 0.01f),
        )
        for (i in 0 until fallenCount) {
            val pos = leafPositions[i]
            drawCircle(
                color = LeafYellow.copy(alpha = 0.6f),
                radius = size.width * 0.012f,
                center = pos
            )
        }
    }
}

private fun DrawScope.drawWitheredTree(cx: Float, groundY: Float) {
    // Dead stump + drooping bare branches
    val trunkHeight = size.height * 0.22f
    val trunkTop = groundY - trunkHeight
    val trunkWidth = size.width * 0.03f

    val trunkPath = Path().apply {
        moveTo(cx - trunkWidth, groundY)
        lineTo(cx + trunkWidth, groundY)
        lineTo(cx + trunkWidth * 0.7f, trunkTop)
        lineTo(cx - trunkWidth * 0.7f, trunkTop)
        close()
    }
    drawPath(trunkPath, WiltedGray, style = Fill)

    // Bare drooping branches
    val branches = listOf(
        Triple(0.35f, -50f, 0.6f),
        Triple(0.30f, 45f, 0.5f),
        Triple(0.50f, -30f, 0.4f),
    )

    for ((heightFrac, angle, lenFrac) in branches) {
        val by = trunkTop + trunkHeight * heightFrac
        val branchLen = size.width * 0.08f * lenFrac
        val rad = Math.toRadians(angle.toDouble())
        val bx = cx + branchLen * cos(rad).toFloat()
        // Branches droop downward
        val bey = by + branchLen * 0.3f

        drawLine(color = WiltedGray.copy(alpha = 0.7f), start = Offset(cx, by), end = Offset(bx, bey), strokeWidth = 2f)
    }
}

private fun DrawScope.drawLeaf(x: Float, y: Float, size: Float, angleDeg: Float, color: Color) {
    val rad = Math.toRadians(angleDeg.toDouble())
    val tipX = x + size * cos(rad).toFloat()
    val tipY = y - size * sin(rad).toFloat()

    val path = Path().apply {
        moveTo(x, y)
        quadraticBezierTo(
            x + size * 0.3f * cos(rad + Math.PI / 3).toFloat(),
            y - size * 0.3f * sin(rad + Math.PI / 3).toFloat(),
            tipX, tipY
        )
        quadraticBezierTo(
            x + size * 0.3f * cos(rad - Math.PI / 3).toFloat(),
            y - size * 0.3f * sin(rad - Math.PI / 3).toFloat(),
            x, y
        )
        close()
    }
    drawPath(path, color, style = Fill)
}

private fun lerpLeafColor(wiltProgress: Float): Color {
    return when {
        wiltProgress <= 0f -> LeafGreen
        wiltProgress < 0.5f -> lerp(LeafGreen, LeafYellow, wiltProgress * 2f)
        else -> lerp(LeafYellow, LeafOrange, (wiltProgress - 0.5f) * 2f)
    }
}
