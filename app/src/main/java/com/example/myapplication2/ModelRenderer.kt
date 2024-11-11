// ModelRenderer.kt
package com.example.myapplication2

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin

class ModelRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private var programId = 0
    private lateinit var model: Model
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private var angle = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.2f, 0.2f, 0.2f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)

        // Load the 3D model
        try {
            val objLoader = ObjLoader()
            val inputStream = context.assets.open("suzanne.obj")
            model = objLoader.loadObjModel(inputStream)
            inputStream.close()
        } catch (e: Exception) {
            Log.e("ModelRenderer", "Error loading model", e)
            throw RuntimeException("Error loading model", e)
        }

        // Create and link shaders
        try {
            val vertexShaderCode = loadShaderFromAssets("vertex_shader.glsl")
            val fragmentShaderCode = loadShaderFromAssets("fragment_shader.glsl")

            val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

            programId = GLES30.glCreateProgram()
            GLES30.glAttachShader(programId, vertexShader)
            GLES30.glAttachShader(programId, fragmentShader)
            GLES30.glLinkProgram(programId)

            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES30.GL_TRUE) {
                throw RuntimeException("Error linking program: " + GLES30.glGetProgramInfoLog(programId))
            }

            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)
        } catch (e: Exception) {
            Log.e("ModelRenderer", "Error creating program", e)
            throw RuntimeException("Error creating program", e)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        try {
            GLES30.glUseProgram(programId)

            val positionHandle = GLES30.glGetAttribLocation(programId, "vPosition")
            val mvpMatrixHandle = GLES30.glGetUniformLocation(programId, "uMVPMatrix")

            // Update rotation angle
            angle += 2f

            // Set up model matrix with rotation
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.rotateM(modelMatrix, 0, angle, 0.5f, 1f, 0.3f)

            // Calculate the projection and view transformation
            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

            // Apply the projection and view transformation
            GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

            // Enable vertex array
            GLES30.glEnableVertexAttribArray(positionHandle)

            // Set the vertex attributes
            GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, model.vertices)

            // Draw the model
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, model.numIndices, GLES30.GL_UNSIGNED_BYTE, model.indices)

            // Disable vertex array
            GLES30.glDisableVertexAttribArray(positionHandle)

        } catch (e: Exception) {
            Log.e("ModelRenderer", "Error in onDrawFrame", e)
        }
    }
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()

        // Set up projection matrix
//        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
        Matrix.frustumM(projectionMatrix, 0,
            -ratio * 2, // left
            ratio * 2,  // right
            -2f,        // bottom
            2f,         // top
            3f,         // near
            15f         // far (increased from 7f to 15f)
        )
        // Set up camera position
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, -8f,  // eye position (z changed from -4f to -8f)
            0f, 0f, 0f,   // center position (looking at origin)
            0f, 1f, 0f    // up vector
        )
//        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -4f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] != GLES30.GL_TRUE) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Error compiling shader: $log")
        }
        return shader
    }

    private fun loadShaderFromAssets(fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }
}