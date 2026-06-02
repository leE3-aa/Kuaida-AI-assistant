package com.kuaida.core.common.util

import kotlin.math.sqrt

fun cosineSimilarity(left: List<Float>, right: List<Float>): Double {
    if (left.isEmpty() || right.isEmpty() || left.size != right.size) return 0.0
    var dot = 0.0
    var leftNorm = 0.0
    var rightNorm = 0.0
    left.indices.forEach { index ->
        dot += left[index] * right[index]
        leftNorm += left[index] * left[index]
        rightNorm += right[index] * right[index]
    }
    if (leftNorm == 0.0 || rightNorm == 0.0) return 0.0
    return dot / (sqrt(leftNorm) * sqrt(rightNorm))
}
