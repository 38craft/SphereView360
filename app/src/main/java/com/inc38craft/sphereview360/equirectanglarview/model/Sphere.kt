package com.inc38craft.sphereview360.equirectanglarview.model

import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.cos
import kotlin.math.sin

class Sphere(private val radius: Float, private val sectorCount: Int, private val stackCount: Int) {

    private val vertices = mutableListOf<Float>()
    private val texCoordinates = mutableListOf<Float>()
    private val indices = mutableListOf<Int>()
    private val interleaveVertices = mutableListOf<Float>()

    fun getIndexCount() = indices.size

    fun getIndicesBufferSize(): Int = indices.size *  java.lang.Float.SIZE / 8

    fun getIndicesBuffer(): IntBuffer = newIntBuffer(indices.toIntArray())

    fun getInterleaveVertexBufferSize(): Int = interleaveVertices.size * java.lang.Float.SIZE / 8

    fun getInterleaveVerticesBuffer(): FloatBuffer = newFloatBuffer(interleaveVertices.toFloatArray())

    init {
        buildVertices()
    }

    private fun addVertex(x: Float, y: Float, z: Float) {
        vertices.add(x)
        vertices.add(y)
        vertices.add(z)
    }

    private fun addTexCoordinate(s: Float, t:  Float) {
        texCoordinates.add(s) // 左右反転
        texCoordinates.add(t)
    }

    private fun addIndices(i1: Int, i2: Int, i3: Int) {
        indices.add(i1)
        indices.add(i2)
        indices.add(i3)
    }

    private fun buildVertices() {
        vertices.clear()
        texCoordinates.clear()
        indices.clear()
        val sphereVertices = createSphereVertices()
        var index = 0

        for (stack in 0 until stackCount) {
            var vi1 = stack * (sectorCount + 1)
            var vi2 = (stack + 1) * (sectorCount + 1)

            for (sector in 0 until sectorCount) {
                // get 4 vertices per sector
                //  v1--v3
                //  |    |
                //  v2--v4
                val v1 = sphereVertices[vi1]
                val v2 = sphereVertices[vi2]
                val v3 = sphereVertices[vi1 + 1]
                val v4 = sphereVertices[vi2 + 1]

                when {
                    // top stack
                    (stack == 0) -> {
                        // put triangle
                        addVertex(v1.x, v1.y, v1.z)
                        addVertex(v2.x, v2.y, v2.z)
                        addVertex(v4.x, v4.y, v4.z)

                        addTexCoordinate(v1.u, v1.v)
                        addTexCoordinate(v2.u, v2.v)
                        addTexCoordinate(v4.u, v4.v)

                        addIndices(index, index+1, index+2)
                        index += 3
                    }
                    // bottom stack
                    (stack == stackCount - 1) -> {
                        // put triangle
                        addVertex(v1.x, v1.y, v1.z)
                        addVertex(v2.x, v2.y, v2.z)
                        addVertex(v3.x, v3.y, v3.z)

                        addTexCoordinate(v1.u, v1.v)
                        addTexCoordinate(v2.u, v2.v)
                        addTexCoordinate(v3.u, v3.v)

                        addIndices(index, index+1, index+2)
                        index += 3
                    }
                    else -> {
                        // put quad vertices
                        addVertex(v1.x, v1.y, v1.z)
                        addVertex(v2.x, v2.y, v2.z)
                        addVertex(v3.x, v3.y, v3.z)
                        addVertex(v4.x, v4.y, v4.z)

                        addTexCoordinate(v1.u, v1.v)
                        addTexCoordinate(v2.u, v2.v)
                        addTexCoordinate(v3.u, v3.v)
                        addTexCoordinate(v4.u, v4.v)

                        addIndices(index, index+1, index+2)
                        addIndices(index+2, index+1, index+3)
                        index += 4
                    }
                }
                vi1++
                vi2++
            }
        }
        buildInterleaveVertices()
    }

    private fun buildInterleaveVertices() {
        interleaveVertices.clear()
        val vertexArray = vertices.toFloatArray()
        val texCoordinateArray = texCoordinates.toFloatArray()

        var texCoordinateIndex = 0

        for (vertexIndex in vertices.indices step 3) {
            val x = vertexArray[vertexIndex]
            val y = vertexArray[vertexIndex + 1]
            val z = vertexArray[vertexIndex + 2]
            val s = texCoordinateArray[texCoordinateIndex]
            val t = texCoordinateArray[texCoordinateIndex + 1]
            interleaveVertices.add(x)
            interleaveVertices.add(y)
            interleaveVertices.add(z)
            interleaveVertices.add(s)
            interleaveVertices.add(t)
            texCoordinateIndex += 2
        }
    }

    private fun createSphereVertices(): Array<Vertex> {
        val modelVertices = mutableListOf<Vertex>()
        val sectorStep = 2 * Math.PI / sectorCount
        val stackStep = Math.PI / stackCount

        for (stack in 0 .. stackCount) {
            val stackAngle = Math.PI / 2 - stack * stackStep
            val xy = radius * cos(stackAngle)
            val z = radius * sin(stackAngle)

            for (sector in 0 .. sectorCount) {
                val sectorAngle = sector * sectorStep
                modelVertices.add(
                    Vertex(
                        x = (xy * cos(sectorAngle)).toFloat(),
                        y = (xy * sin(sectorAngle)).toFloat(),
                        z = z.toFloat(),
                        u = sector.toFloat() / sectorCount,
                        v = stack.toFloat() / stackCount
                    )
                )
            }
        }
        return modelVertices.toTypedArray()
    }
}
