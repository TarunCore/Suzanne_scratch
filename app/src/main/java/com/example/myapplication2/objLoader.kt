package com.example.myapplication2

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.ArrayList

data class Model(
    val vertices: FloatBuffer,
    val normals: FloatBuffer?,
    val textureCoords: FloatBuffer?,
    val indices: ByteBuffer,
    val numVertices: Int,
    val numIndices: Int
)

class ObjLoader {
    fun loadObjModel(inputStream: InputStream): Model {
        val vertices = ArrayList<Float>()
        val textures = ArrayList<Float>()
        val normals = ArrayList<Float>()
        val indices = ArrayList<Int>()

        val vertexIndices = ArrayList<Int>()
        val textureIndices = ArrayList<Int>()
        val normalIndices = ArrayList<Int>()

        val tempVertices = ArrayList<Float>()
        val tempTextures = ArrayList<Float>()
        val tempNormals = ArrayList<Float>()

        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val tokens = line!!.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
                if (tokens.isEmpty()) continue

                when (tokens[0]) {
                    "v" -> { // Vertex
                        tempVertices.add(tokens[1].toFloat())
                        tempVertices.add(tokens[2].toFloat())
                        tempVertices.add(tokens[3].toFloat())
                    }
                    "vt" -> { // Texture coordinate
                        tempTextures.add(tokens[1].toFloat())
                        tempTextures.add(tokens[2].toFloat())
                    }
                    "vn" -> { // Normal
                        tempNormals.add(tokens[1].toFloat())
                        tempNormals.add(tokens[2].toFloat())
                        tempNormals.add(tokens[3].toFloat())
                    }
                    "f" -> { // Face
                        for (i in 1..3) {
                            val vertexData = tokens[i].split("/")
                            vertexIndices.add(vertexData[0].toInt() - 1)
                            if (vertexData.size > 1 && vertexData[1].isNotEmpty()) {
                                textureIndices.add(vertexData[1].toInt() - 1)
                            }
                            if (vertexData.size > 2) {
                                normalIndices.add(vertexData[2].toInt() - 1)
                            }
                        }
                    }
                }
            }
        }

        // Process vertices
        for (i in vertexIndices.indices) {
            val vertexIndex = vertexIndices[i]
            vertices.add(tempVertices[vertexIndex * 3])
            vertices.add(tempVertices[vertexIndex * 3 + 1])
            vertices.add(tempVertices[vertexIndex * 3 + 2])

            if (textureIndices.isNotEmpty()) {
                val textureIndex = textureIndices[i]
                textures.add(tempTextures[textureIndex * 2])
                textures.add(tempTextures[textureIndex * 2 + 1])
            }

            if (normalIndices.isNotEmpty()) {
                val normalIndex = normalIndices[i]
                normals.add(tempNormals[normalIndex * 3])
                normals.add(tempNormals[normalIndex * 3 + 1])
                normals.add(tempNormals[normalIndex * 3 + 2])
            }

            indices.add(i)
        }

        // Create buffers
        val vertexBuffer = createFloatBuffer(vertices)
        val normalBuffer = if (normals.isNotEmpty()) createFloatBuffer(normals) else null
        val textureBuffer = if (textures.isNotEmpty()) createFloatBuffer(textures) else null
        val indexBuffer = createByteBuffer(indices)

        return Model(
            vertices = vertexBuffer,
            normals = normalBuffer,
            textureCoords = textureBuffer,
            indices = indexBuffer,
            numVertices = vertices.size / 3,
            numIndices = indices.size
        )
    }

    private fun createFloatBuffer(data: List<Float>): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        data.forEach { buffer.put(it) }
        buffer.position(0)
        return buffer
    }

    private fun createByteBuffer(data: List<Int>): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(data.size)
            .order(ByteOrder.nativeOrder())
        data.forEach { buffer.put(it.toByte()) }
        buffer.position(0)
        return buffer
    }
}