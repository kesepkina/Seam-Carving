package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Exception
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class Node(
    var from: Pair<Int, Int>? = null,
    var distance: Double = Double.MAX_VALUE,
    var energy: Double = 0.0,
    var processed: Boolean = false
)

enum class Args(val value: String) {
    IN("-in"),
    OUT("-out"),
    WIDTH("-width"),
    HEIGHT("-height")
}

enum class Direction {
    HORIZONTAL, VERTICAL
}

fun main(args: Array<String>) {
    var inputFileName: String? = null
    var outputFileName: String? = null
    var numOfVerticalSeamsToRemove: Int? = null
    var numOfHorizontalSeamsToRemove: Int? = null
    for (ind in 0..args.lastIndex step 2) {
        when (args[ind]) {
            Args.IN.value -> inputFileName = args[ind + 1]
            Args.OUT.value -> outputFileName = args[ind + 1]
            Args.WIDTH.value -> numOfVerticalSeamsToRemove = args[ind + 1].toInt()
            Args.HEIGHT.value -> numOfHorizontalSeamsToRemove = args[ind + 1].toInt()
            else -> throw Exception("Undefined argument ${args[ind]}")
        }
    }
    var image =
        ImageIO.read(inputFileName?.let { File(it) }) ?: throw Exception("Not defined image file $inputFileName")
    if (numOfVerticalSeamsToRemove != null) {
        repeat(numOfVerticalSeamsToRemove) {
            drawVertivalSeamToRemove(image, Color.RED)
            image = removePixelsOfColor(image, Color.RED, Direction.VERTICAL)
        }
    }
    if (numOfHorizontalSeamsToRemove != null) {
        repeat(numOfHorizontalSeamsToRemove) {
            drawHorizontalSeamToRemove(image, Color.RED)
            image = removePixelsOfColor(image, Color.RED, Direction.HORIZONTAL)
        }
    }
    val file = outputFileName?.let { File(it) }
    ImageIO.write(image, "png", file)
}

fun removePixelsOfColor(image: BufferedImage, color: Color?, seamDirection: Direction): BufferedImage {
    val newWidth = if (seamDirection == Direction.VERTICAL) image.width - 1 else image.width
    val newHeight = if (seamDirection == Direction.HORIZONTAL) image.height - 1 else image.height
    val resizedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
    if (seamDirection == Direction.HORIZONTAL) {
        for (i in 0 until image.width) {
            var shift = 0
            for (j in 0 until image.height) {
                val rgb = image.getRGB(i, j)
                if (rgb == (color?: Color.RED).rgb) {
                    shift += 1
                } else {
                    resizedImage.setRGB(i, j - shift, rgb)
                }
            }
        }
    } else {
        for (j in 0 until image.height) {
            var shift = 0
            for (i in 0 until image.width) {
                val rgb = image.getRGB(i, j)
                if (rgb == (color?: Color.RED).rgb) {
                    shift += 1
                } else {
                    resizedImage.setRGB(i - shift, j, rgb)
                }
            }
        }
    }
    return resizedImage
}

fun drawHorizontalSeamToRemove(image: BufferedImage, seamColor: Color) {
    val nodesMatrix = calcPixelsEnergyMatrix(image)
    calcShortestHorizontalPathWithDP(nodesMatrix)
    val minNode = nodesMatrix.last()[0]
    var minNodeJ = 0
    var minDist = minNode.distance
    for (j in nodesMatrix.last().indices) {
        nodesMatrix.last()[j].run {
            if (distance < minDist) {
                minDist = distance
                minNodeJ = j
            }
        }
    }
    image.setRGB(nodesMatrix.lastIndex, minNodeJ, seamColor.rgb)
    for (i in nodesMatrix.lastIndex downTo 1) {
        minDist = nodesMatrix[i - 1][minNodeJ].distance
        for (j in max(minNodeJ - 1, 0)..min(minNodeJ + 1, nodesMatrix[0].lastIndex)) {
            if (nodesMatrix[i - 1][j].distance < minDist) {
                minDist = nodesMatrix[i - 1][j].distance
                minNodeJ = j
            }
        }
        image.setRGB(i - 1, minNodeJ, seamColor.rgb)
    }
}

fun drawVertivalSeamToRemove(image: BufferedImage, seamColor: Color) {
    val nodesMatrix = calcPixelsEnergyMatrix(image)
    calcShortestVerticalPathWithDP(nodesMatrix)
    val minNode = nodesMatrix[0].last()
    var minNodeI = 0
    var minDist = minNode.distance
    for (i in nodesMatrix.indices) {
        nodesMatrix[i].last().run {
            if (distance < minDist) {
                minDist = distance
                minNodeI = i
            }
        }
    }
    image.setRGB(minNodeI, nodesMatrix[0].lastIndex, seamColor.rgb)
    for (j in nodesMatrix[0].lastIndex downTo 1) {
        minDist = nodesMatrix[minNodeI][j - 1].distance
        for (i in max(minNodeI - 1, 0)..min(minNodeI + 1, nodesMatrix.lastIndex)) {
            if (nodesMatrix[i][j - 1].distance < minDist) {
                minDist = nodesMatrix[i][j - 1].distance
                minNodeI = i
            }
        }
        image.setRGB(minNodeI, j - 1, seamColor.rgb)
    }
}

fun calcShortestHorizontalPathWithDP(matrix: Array<Array<Node>>) {
    val iFirstCol = 0
    for (j in 0..matrix[0].lastIndex) {
        matrix[iFirstCol][j].apply {
            distance = energy
        }
    }
    for (i in 1..matrix.lastIndex) {
        for (j in 0..matrix[0].lastIndex) {
            matrix[i][j].apply {
                var minDist = Double.MAX_VALUE
                for (jLeft in max(j - 1, 0)..min(j + 1, matrix[0].lastIndex)) {
                    if (matrix[i - 1][jLeft].distance < minDist) {
                        minDist = matrix[i - 1][jLeft].distance
                        from = Pair(i - 1, jLeft)
                    }
                }
                distance = energy + minDist
            }
        }
    }
}

fun calcShortestVerticalPathWithDP(matrix: Array<Array<Node>>) {
    val jFirstRow = 0
    for (i in 0..matrix.lastIndex) {
        matrix[i][jFirstRow].apply {
            distance = energy
        }
    }
    for (j in 1..matrix[0].lastIndex) {
        for (i in 0..matrix.lastIndex) {
            matrix[i][j].apply {
                var minDist = Double.MAX_VALUE
                for (iUp in max(i - 1, 0)..min(i + 1, matrix.lastIndex)) {
                    if (matrix[iUp][j - 1].distance < minDist) {
                        minDist = matrix[iUp][j - 1].distance
                        from = Pair(iUp, j - 1)
                    }
                }
                distance = energy + minDist
            }
        }
    }
}

fun calcPixelsEnergyMatrix(image: BufferedImage): Array<Array<Node>> {
    val energyMatrix = Array(image.width) { Array(image.height) { Node() } }
    for (i in 0 until image.width) {
        val (iLeft, iRight) = when (i) {
            0 -> Pair(0, 2)
            image.width - 1 -> Pair(i - 2, i)
            else -> Pair(i - 1, i + 1)
        }
        for (j in 0 until image.height) {
            val (jUp, jDown) = when (j) {
                0 -> Pair(0, 2)
                image.height - 1 -> Pair(j - 2, j)
                else -> Pair(j - 1, j + 1)
            }
            val initialColorRight = Color(image.getRGB(iRight, j))
            val initialColorLeft = Color(image.getRGB(iLeft, j))
            val initialColorUp = Color(image.getRGB(i, jUp))
            val initialColorDown = Color(image.getRGB(i, jDown))
            val xGrad = calcGradient(initialColorLeft, initialColorRight)
            val yGrad = calcGradient(initialColorDown, initialColorUp)
            energyMatrix[i][j].energy = sqrt((xGrad + yGrad))
        }
    }
    return energyMatrix
}

fun calcGradient(color1: Color, color2: Color): Double {
    val r = (color1.red - color2.red).toDouble()
    val g = (color1.green - color2.green).toDouble()
    val b = (color1.blue - color2.blue).toDouble()
    return r * r + g * g + b * b
}
