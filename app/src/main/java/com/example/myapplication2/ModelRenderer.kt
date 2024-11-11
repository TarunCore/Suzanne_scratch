// ModelRenderer.kt
import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ModelRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private var programId = 0
    private var vertexBuffer: FloatBuffer? = null
    private val TAG = "ModelRenderer"

    // Sample triangle vertices
    private val triangleCoords = floatArrayOf(
        0.0f, 0.622008459f, 0.0f,      // top
        -0.5f, -0.311004243f, 0.0f,    // bottom left
        0.5f, -0.311004243f, 0.0f      // bottom right
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Set background color to dark gray for better visibility
        GLES30.glClearColor(0.2f, 0.2f, 0.2f, 1.0f)

        // Initialize vertex buffer
        val bb = ByteBuffer.allocateDirect(triangleCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer().apply {
            put(triangleCoords)
            position(0)
        }

        try {
            // Load and compile shaders
            val vertexShaderCode = loadShaderFromAssets("vertex_shader.glsl")
            val fragmentShaderCode = loadShaderFromAssets("fragment_shader.glsl")

            Log.d(TAG, "Vertex Shader Code:\n$vertexShaderCode")
            Log.d(TAG, "Fragment Shader Code:\n$fragmentShaderCode")

            val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

            // Create shader program
            programId = GLES30.glCreateProgram()
            GLES30.glAttachShader(programId, vertexShader)
            GLES30.glAttachShader(programId, fragmentShader)
            GLES30.glLinkProgram(programId)

            // Check linking status
            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES30.GL_TRUE) {
                val log = GLES30.glGetProgramInfoLog(programId)
                Log.e(TAG, "Error linking program: $log")
                throw RuntimeException("Error linking program: $log")
            }

            // Delete shaders as they're linked into the program now and no longer needed
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)

            Log.d(TAG, "Program created successfully with ID: $programId")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating program", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "Surface changed: width=$width, height=$height")
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            // Clear the background
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            // Use shader program
            GLES30.glUseProgram(programId)
            checkGlError("glUseProgram")

            // Get position attribute location
            val positionHandle = GLES30.glGetAttribLocation(programId, "vPosition")
            Log.d(TAG, "Position handle: $positionHandle")

            if (positionHandle == -1) {
                throw RuntimeException("vPosition attribute not found")
            }

            // Enable vertex array
            GLES30.glEnableVertexAttribArray(positionHandle)
            checkGlError("glEnableVertexAttribArray")

            // Prepare the triangle coordinate data
            GLES30.glVertexAttribPointer(
                positionHandle, 3,
                GLES30.GL_FLOAT, false,
                0, vertexBuffer
            )
            checkGlError("glVertexAttribPointer")

            // Draw the triangle
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
            checkGlError("glDrawArrays")

            // Disable vertex array
            GLES30.glDisableVertexAttribArray(positionHandle)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDrawFrame", e)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        // Check compilation status
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] != GLES30.GL_TRUE) {
            val log = GLES30.glGetShaderInfoLog(shader)
            Log.e(TAG, "Error compiling shader: $log")
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Error compiling shader: $log")
        }

        return shader
    }

    private fun loadShaderFromAssets(fileName: String): String {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading shader from assets: $fileName", e)
            throw RuntimeException("Error loading shader: $fileName", e)
        }
    }

    private fun checkGlError(operation: String) {
        val error = GLES30.glGetError()
        if (error != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "$operation: glError $error")
            throw RuntimeException("$operation: glError $error")
        }
    }
}