package com.inc38craft.sphereview

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView.Renderer
import android.opengl.Matrix
import com.inc38craft.sphereview.model.Sphere
import timber.log.Timber
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

class SphereViewRenderer : Renderer {

    private val sphere = Sphere(SPHERE_RADIUS, 180, 90)

    // GL関連
    private var program = 0
    private var positionAttributeLocation = 0
    private var texCoordAttributeLocation = 0
    private var modelViewProjectionMatrixUniformLocation = 0
    private var surfaceTextureMatrixUniformLocation = 0
    private var surfaceTextureCoordOriginUniformLocation = 0
    private var surfaceTextureCoordFlipUniformLocation = 0
    private var textureId = 0
    private var verticesBufferObjectId = 0
    private var indicesBufferObjectId = 0

    private val projectionMatrix = FloatArray(4 * 4)
    private val modelMatrix = FloatArray(4 * 4)
    private val cameraMatrix = FloatArray(4 * 4)
    private val viewMatrix = FloatArray(4 * 4)
    private var fovAngleDegree = 90.0f
    private val modelViewProjectionMatrix = FloatArray(4 * 4)
    private val surfaceTextureMatrix = FloatArray(4 * 4)
    private val surfaceTextureCoordOrigin = FloatArray(2)
    private val surfaceTextureCoordFlip = FloatArray(2)
    private var surfaceTextureCreatedCb: ((surfaceTexture: SurfaceTexture) -> Unit)? = null

    private var surfaceTexture: SurfaceTexture? = null
    var surfaceHeight = 0
    var surfaceWidth = 0
    var isVerticalFlip = false
    var isHorizontalFlip = false

    init {
        Matrix.setIdentityM(projectionMatrix, 0)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setIdentityM(cameraMatrix, 0)
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.setIdentityM(surfaceTextureMatrix, 0 )
    }

    fun setOnSurfaceTextureCreated(callback: ((surfaceTexture: SurfaceTexture) -> Unit)?) {
        surfaceTextureCreatedCb = callback
    }

    fun setFovAngle(degree: Float) {
        fovAngleDegree = degree.coerceIn(10.0f, 170.0f)
        if (surfaceHeight != 0 && surfaceWidth != 0) {
            val ratio = surfaceHeight.toFloat() / surfaceWidth.toFloat()
            val near = sin(Math.toRadians((90 - fovAngleDegree / 2).toDouble())).toFloat() * SPHERE_RADIUS
            val theta = atan(ratio)
            val r = cos(asin(near)) * SPHERE_RADIUS
            val top = r * sin(theta)
            val right = r * cos(theta)
            Matrix.frustumM(projectionMatrix, 0, -right, right, -top, top, near, SPHERE_RADIUS * 2)
        }
    }

    fun resetCameraAngle() {
        Matrix.setLookAtM(cameraMatrix, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0f, 1.0f, 0f, 1.0f, 0.0f)
    }

    fun rotateCameraAngle(deltaYawDegree: Float, deltaPitchDegree: Float) {
        val rotationMatrix = FloatArray(4 * 4)

        // YawはY軸回転
        Matrix.setIdentityM(rotationMatrix, 0)
        Matrix.rotateM(rotationMatrix, 0, deltaYawDegree, 0.0f, 1.0f, 0.0f)
        Matrix.multiplyMM(cameraMatrix, 0, cameraMatrix, 0, rotationMatrix, 0)

        // PitchはCamera視点のX軸回転
        Matrix.setIdentityM(rotationMatrix, 0)
        Matrix.rotateM(rotationMatrix, 0, deltaPitchDegree, cameraMatrix[0], 0.0f, -cameraMatrix[2])
        Matrix.multiplyMM(cameraMatrix, 0, cameraMatrix, 0, rotationMatrix, 0)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        program = createProgram()
        if (0 == program) {
            return
        }

        // AttributeLocation
        positionAttributeLocation = GLES20.glGetAttribLocation(program, "attributePosition")
        checkGlError("glGetAttribLocation attributePosition")
        texCoordAttributeLocation = GLES20.glGetAttribLocation(program, "attributeTexCoord")
        checkGlError("glGetAttribLocation attributeTexCoord")

        // UniformLocation
        modelViewProjectionMatrixUniformLocation =
            GLES20.glGetUniformLocation(program, "uniformModelViewProjectionMatrix")
        checkGlError("glGetUniformLocation uniformModelViewProjectionMatrix")
        surfaceTextureMatrixUniformLocation =
            GLES20.glGetUniformLocation(program, "uniformSurfaceTextureMatrix")
        checkGlError("glGetUniformLocation uniformSurfaceTextureMatrix")
        surfaceTextureCoordOriginUniformLocation =
            GLES20.glGetUniformLocation(program, "uniformSurfaceTextureCoordOrigin")
        checkGlError("glGetUniformLocation uniformSurfaceTextureCoordOrigin")
        surfaceTextureCoordFlipUniformLocation =
            GLES20.glGetUniformLocation(program, "uniformSurfaceTextureCoordFlip")
        checkGlError("glGetUniformLocation uniformSurfaceTextureCoordFlip")

        // Texture
        val texID = IntArray(1)
        GLES20.glGenTextures(1, texID, 0)
        textureId = texID[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST_MIPMAP_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_REPEAT
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_REPEAT
        )
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

        // VBOの作成とデータのグラフィックスメモリへのコピー
        val vboID = IntArray(1)
        GLES20.glGenBuffers(1, vboID, 0)
        verticesBufferObjectId = vboID[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBufferObjectId)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            sphere.getInterleaveVertexBufferSize(),
            sphere.getInterleaveVerticesBuffer(),
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        val vbiID = IntArray(1)
        GLES20.glGenBuffers(1, vbiID, 0)
        indicesBufferObjectId = vbiID[0]
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBufferObjectId)
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER,
            sphere.getIndicesBufferSize(),
            sphere.getIndicesBuffer(),
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

        // SurfaceTextureとSurfaceの作成
        surfaceTexture = SurfaceTexture(textureId).apply {
            surfaceTextureCreatedCb?.invoke(this)
        }
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        gl.glViewport(0, 0, width, height)
        surfaceHeight = height
        surfaceWidth = width
        surfaceTexture?.setDefaultBufferSize( width, height )

        // SphereはZ軸が緯度になるように作られているので、X軸中心に90度回転してワールド座標系に合わせる
        Matrix.setRotateM(modelMatrix, 0, 90.0f, 1.0f, 0.0f, 0.0f)

        setFovAngle(fovAngleDegree)
        resetCameraAngle()
    }

    override fun onDrawFrame(gl: GL10) {
        surfaceTexture?.let { texture ->
            texture.updateTexImage()
            texture.getTransformMatrix(surfaceTextureMatrix)
            surfaceTextureCoordOrigin[0] = if(isHorizontalFlip) 1.0f else 0.0f
            surfaceTextureCoordOrigin[1] = if(isVerticalFlip) 1.0f else 0.0f

            surfaceTextureCoordFlip[0] = if(isHorizontalFlip) -1.0f else 1.0f
            surfaceTextureCoordFlip[1] = if(isVerticalFlip) -1.0f else 1.0f
        }

        Matrix.multiplyMM(viewMatrix, 0, cameraMatrix, 0, modelMatrix, 0)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // バッファークリア
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        checkGlError("glClear")

        // 使用するシェーダープログラムの指定
        GLES20.glUseProgram(program)
        checkGlError("glUseProgram")

        // テクスチャの有効化
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        // シェーダープログラムへデータの転送
        GLES20.glUniformMatrix4fv(modelViewProjectionMatrixUniformLocation, 1, false, modelViewProjectionMatrix, 0)
        checkGlError("glUniformMatrix4fv MVPMatrix")
        GLES20.glUniformMatrix4fv(surfaceTextureMatrixUniformLocation, 1, false, surfaceTextureMatrix, 0 )
        checkGlError("glUniformMatrix4fv STMatrix")
        GLES20.glUniform2fv(surfaceTextureCoordOriginUniformLocation, 1, surfaceTextureCoordOrigin, 0)
        checkGlError("glUniform2fv surfaceTextureCoordOrigin")
        GLES20.glUniform2fv(surfaceTextureCoordFlipUniformLocation, 1, surfaceTextureCoordFlip, 0)
        checkGlError("glUniform2fv surfaceTextureCoordFlip")

        // VBOのバインド
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBufferObjectId)

        // シェーダープログラムへ頂点座標値データの転送
        GLES20.glVertexAttribPointer(
            positionAttributeLocation,
            3,
            GLES20.GL_FLOAT,
            false,
            5 * java.lang.Float.SIZE / 8,
            0
        )
        checkGlError("glVertexAttribPointer Position")
        GLES20.glEnableVertexAttribArray(positionAttributeLocation)
        checkGlError("glEnableVertexAttribArray Position")

        // シェーダープログラムへテクスチャ座標値データの転送
        GLES20.glVertexAttribPointer(
            texCoordAttributeLocation,
            2,
            GLES20.GL_FLOAT,
            false,
            5 * java.lang.Float.SIZE / 8,
            3 * java.lang.Float.SIZE / 8
        )
        checkGlError("glVertexAttribPointer TexCoord")
        GLES20.glEnableVertexAttribArray(texCoordAttributeLocation)
        checkGlError("glEnableVertexAttribArray TexCoord")

        // 描画
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBufferObjectId)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, sphere.getIndexCount(), GLES20.GL_UNSIGNED_INT, 0)
        checkGlError("glDrawElements")

        // VBOのバインドの解除
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

        GLES20.glFlush()
        checkGlError("glFlush")
        GLES20.glFinish()
    }

    private fun createProgram(): Int {
        // Vertexシェーダーの読み込み
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        if (0 == vertexShader) {
            return 0
        }
        // Fragmentシェーダーの読み込み
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        if (0 == fragmentShader) {
            return 0
        }

        // シェーダープログラムの作成
        val program = GLES20.glCreateProgram()
        if (0 == program) {
            return 0
        }

        // シェーダープログラムにシェーダーを割り付け
        GLES20.glAttachShader(program, vertexShader)
        checkGlError("glAttachShader")
        GLES20.glAttachShader(program, fragmentShader)
        checkGlError("glAttachShader")

        // シェーダープログラムのリンク
        GLES20.glLinkProgram(program)
        checkGlError("glLinkProgram")

        // リンク結果の確認
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (GLES20.GL_FALSE == linkStatus[0]) {
            Timber.e("Could not link program. ${GLES20.glGetProgramInfoLog(program)}")
            GLES20.glDeleteProgram(program)
            return 0
        }
        return program
    }

    // GLESシェーダーの読み込み
    private fun loadShader(shaderType: Int, shaderCode: String): Int {
        // シェーダーの作成
        val shader = GLES20.glCreateShader(shaderType)
        if (0 == shader) {
            Timber.e("Could not create shader. Shader Type = $shaderType")
            return 0
        }

        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        // コンパイル結果の確認
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (GLES20.GL_FALSE == compileStatus[0]) {
            Timber.e("Could not compile shader. Shader Type = $shaderType  ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    // GLESエラーチェック
    private fun checkGlError(functionName: String) {
        var error: Int
        while (true) {
            error = GLES20.glGetError()
            if (GLES20.GL_NO_ERROR == error) {
                break
            }
            Timber.e("checkGlError: $functionName error code = $error")
        }
    }

    companion object {
        private const val VERTEX_SHADER =
            "uniform mat4 uniformModelViewProjectionMatrix;\n" +
                    "uniform mat4 uniformSurfaceTextureMatrix;\n" +
                    "attribute vec4 attributePosition;\n" +
                    "attribute vec4 attributeTexCoord;\n" +
                    "varying vec2 varyingTexCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uniformModelViewProjectionMatrix * attributePosition;\n" +
                    "  varyingTexCoord = (uniformSurfaceTextureMatrix * attributeTexCoord).xy;\n" +
                    "}\n"

        private const val FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 varyingTexCoord;\n" +
                    "uniform vec2 uniformSurfaceTextureCoordOrigin;\n" +
                    "uniform vec2 uniformSurfaceTextureCoordFlip;\n" +
                    "uniform samplerExternalOES uniformTexture;\n" +
                    "void main() {\n" +
                    "  vec2 coord = uniformSurfaceTextureCoordOrigin + (varyingTexCoord.st * uniformSurfaceTextureCoordFlip);" +
                    "  gl_FragColor = texture2D(uniformTexture, coord);\n" +
                    "}\n"

        private const val SPHERE_RADIUS = 1.0f
    }
}
