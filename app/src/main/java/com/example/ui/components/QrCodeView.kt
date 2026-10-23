package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.security.MessageDigest
import kotlin.math.abs

/**
 * Procedural authentic QR-code style 2D cryptographic matrix pattern
 * with standard 3 corner finder patterns.
 */
@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    matrixSize: Int = 25,
    pixelColor: Color = Color(0xFF00E676),
    backgroundColor: Color = Color(0xFF0B141A)
) {
    val grid = remember(data, matrixSize) {
        generateQrGrid(data, matrixSize)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().aspectRatio(1f)) {
            val cellSize = size.width / matrixSize
            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    if (grid[r][c]) {
                        drawRect(
                            color = pixelColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.94f, cellSize * 0.94f)
                        )
                    }
                }
            }
        }
    }
}

private fun generateQrGrid(data: String, size: Int): Array<BooleanArray> {
    val grid = Array(size) { BooleanArray(size) }
    val md = MessageDigest.getInstance("SHA-256")
    val hash = md.digest(data.toByteArray())

    // 1. Fill pseudo-random matrix based on hash
    for (r in 0 until size) {
        for (c in 0 until size) {
            val byteIndex = (r * size + c) % hash.size
            val bit = (abs(hash[byteIndex].toInt()) shr ((r + c) % 8)) and 1
            grid[r][c] = bit == 1
        }
    }

    // 2. Overlay corner finder patterns (7x7)
    applyFinderPattern(grid, 0, 0)
    applyFinderPattern(grid, 0, size - 7)
    applyFinderPattern(grid, size - 7, 0)

    // 3. Timing tracks
    for (i in 7 until size - 7) {
        grid[6][i] = (i % 2 == 0)
        grid[i][6] = (i % 2 == 0)
    }

    return grid
}

private fun applyFinderPattern(grid: Array<BooleanArray>, startR: Int, startC: Int) {
    for (r in 0..6) {
        for (c in 0..6) {
            val isBorder = r == 0 || r == 6 || c == 0 || c == 6
            val isInner = r in 2..4 && c in 2..4
            grid[startR + r][startC + c] = isBorder || isInner
        }
    }
}
