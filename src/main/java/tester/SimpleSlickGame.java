package tester;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.*;
import org.newdawn.slick.opengl.SlickCallable;
import org.newdawn.slick.opengl.pbuffer.FBOGraphics;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import static org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glBindFramebufferEXT;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class SimpleSlickGame extends BasicGame {
    final static int w = 512;
    final static int h = 512;

    final ArrayList<FrameBuffer> reductionCalcFBO = new ArrayList<>();


    int colorTextureID;
    int framebufferID;
    int depthRenderBufferID;
    int angle = 0;
    ShaderTester shaderTester;
    Image gandalf;
    Image cat4;
    Image empty;
    Image shadowCasters;
    FBOGraphics shadowCastersFBO;
    Image testfboTexture;
    FBOGraphics testFBO;
    int targetTextureDimensionLocation_ReductionProgram;
    int textureDimensionsLocation_DistanceProgram;
    int renderTargetSizeLoaction_DrawProgram;
    int shadowMapSamplerLoaction_DrawProgram;
    int inputSamplerLoaction_ReductionProgram;
    int mvpLocation;
    int mvpDistanceProgram;
    int mvpDistortionProgam;
    int mvpReductionProgam;
    int mvpDrawProgam;
    int textureLocation;
    int resolutionLocation;
    int inputSamplerLocation_DistortionProgram;
    int shadowCastersTextureLocation_DistanceProgam;
    int vao;
    int indicesBuffer;
    int vertexBuffer;
    int uvBuffer;
    Matrix4f mvp;
    FloatBuffer mat4Buffer = BufferUtils.createFloatBuffer(16);
    int secondTextureLocation;
    int myFBOId;
    int textureID;

    FrameBuffer distanceFBO;
    FrameBuffer distortionFBO;
    FrameBuffer[] reductionFBO;
    FrameBuffer shadowsFBO;

    public SimpleSlickGame(String gamename) {
        super(gamename);
    }

    public static void main(String[] args) {
        try {
            AppGameContainer appgc;
            appgc = new AppGameContainer(new SimpleSlickGame("Simple Slick Game"));
            appgc.setDisplayMode(w, h, false);
            appgc.start();
        } catch (SlickException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Sets the given matrix to an orthographic 2D projection matrix, and returns it. If the given
     * matrix is null, a new one will be created and returned.
     *
     * @param m    the matrix to re-use, or null to create a new matrix
     * @param near near clipping plane
     * @param far  far clipping plane
     * @return the given matrix, or a newly created matrix if none was specified
     */
    public static Matrix4f toOrtho2D(Matrix4f m, float x, float y, float width, float height, float near, float far) {
        return toOrtho(m, x, x + width, y, y + height, near, far);
    }

    /**
     * Sets the given matrix to an orthographic projection matrix, and returns it. If the given matrix
     * is null, a new one will be created and returned.
     *
     * @param m    the matrix to re-use, or null to create a new matrix
     * @param near near clipping plane
     * @param far  far clipping plane
     * @return the given matrix, or a newly created matrix if none was specified
     */
    public static Matrix4f toOrtho(Matrix4f m, float left, float right, float bottom, float top,
                                   float near, float far) {
        if (m == null)
            m = new Matrix4f();
        float x_orth = 2.0f / (right - left);
        float y_orth = 2.0f / (top - bottom);
        float z_orth = -2.0f / (far - near);

        float tx = -(right + left) / (right - left);
        float ty = -(top + bottom) / (top - bottom);
        float tz = -(far + near) / (far - near);

        m.m00 = x_orth;
        m.m10 = 0;
        m.m20 = 0;
        m.m30 = 0;
        m.m01 = 0;
        m.m11 = y_orth;
        m.m21 = 0;
        m.m31 = 0;
        m.m02 = 0;
        m.m12 = 0;
        m.m22 = z_orth;
        m.m32 = 0;
        m.m03 = tx;
        m.m13 = ty;
        m.m23 = tz;
        m.m33 = 1;
        return m;
    }

    @Override
    public void init(GameContainer gc) throws SlickException {
        gandalf = new Image("res/sprites/entities/testEntity1.png");
        cat4 = new Image("res/sprites/entities/cat4.png");
        empty = new Image(w, h);
        shaderTester = new ShaderTester();
        GL11.glEnable(GL11.GL_TEXTURE);

        textureLocation = GL20.glGetUniformLocation(shaderTester.testProgram, "texture");
        secondTextureLocation = GL20.glGetUniformLocation(shaderTester.testProgram, "texture1");
        resolutionLocation = GL20.glGetUniformLocation(shaderTester.testProgram, "resolution");
        mvpLocation = GL20.glGetUniformLocation(shaderTester.testProgram, "mvp");

        targetTextureDimensionLocation_ReductionProgram = GL20.glGetUniformLocation(shaderTester.reductionProgram, "normalizedSourceDimensions");
        textureDimensionsLocation_DistanceProgram = GL20.glGetUniformLocation(shaderTester.distanceProgram, "textureDimension");
        renderTargetSizeLoaction_DrawProgram = GL20.glGetUniformLocation(shaderTester.drawProgram, "renderTargetSize");
        shadowMapSamplerLoaction_DrawProgram = GL20.glGetUniformLocation(shaderTester.drawProgram, "shadowMapSampler");
        inputSamplerLoaction_ReductionProgram = GL20.glGetUniformLocation(shaderTester.reductionProgram, "inputSampler");
        mvpDistanceProgram = GL20.glGetUniformLocation(shaderTester.distanceProgram, "mvp");
        mvpDistortionProgam = GL20.glGetUniformLocation(shaderTester.distortionProgram, "mvp");
        mvpReductionProgam = GL20.glGetUniformLocation(shaderTester.reductionProgram, "mvp");
        mvpDrawProgam = GL20.glGetUniformLocation(shaderTester.drawProgram, "mvp");
        inputSamplerLocation_DistortionProgram = GL20.glGetUniformLocation(shaderTester.distortionProgram, "inputSampler");
        shadowCastersTextureLocation_DistanceProgam = GL20.glGetUniformLocation(shaderTester.distanceProgram, "shadowCastersTexture");


        shadowCasters = new Image(w,h);
        shadowCastersFBO = new FBOGraphics(shadowCasters);

        distanceFBO = new FrameBuffer(FrameBuffer.type.FLOAT, false, w, h);
        distortionFBO = new FrameBuffer(FrameBuffer.type.FLOAT, false, w, h);
        shadowsFBO = new FrameBuffer(FrameBuffer.type.FLOAT, false, w, h);


        int nReductions = (int) (Math.log(w) / Math.log(2) + 1e-12);
        for (int i = 1; i < nReductions; i++) {
            reductionCalcFBO.add(new FrameBuffer(FrameBuffer.type.FLOAT, false, (int) Math.pow(2, i), h));
        }


        vertexBuffer = glGenBuffers();
        glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
        glBufferData(GL15.GL_ARRAY_BUFFER, (FloatBuffer) BufferUtils.createFloatBuffer(12).put(new float[]{
                0, 0, 0.0f,
                w, 0, 0.0f,
                w, h, 0.0f,
                0, h, 0.0f})
                .flip(), GL15.GL_STATIC_DRAW);


        //indices for a quad made up of two triangles
        indicesBuffer = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, (ShortBuffer) BufferUtils.createShortBuffer(6).put(new short[]{
                0, 1, 2,
                2, 3, 0
        }).flip(), GL_STATIC_DRAW); //flip() optimizes the buffer for read operations

        uvBuffer = glGenBuffers();
        glBindBuffer(GL15.GL_ARRAY_BUFFER, uvBuffer);
        glBufferData(GL15.GL_ARRAY_BUFFER, (FloatBuffer) BufferUtils.createFloatBuffer(8).put(new float[]{
                0.0f, 0.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,

        }).flip(), GL_STATIC_DRAW);

        vao = glGenVertexArrays();

        glViewport(0, 0, w, h);
        Matrix4f model = new Matrix4f();
        Matrix4f view = new Matrix4f();
        Matrix4f projection = toOrtho2D(null, 0, 0, w, h, 1, -1);
        projection.transpose();
        model.scale(new Vector3f(1, -1, 1));
        projection.translate(new Vector3f(0, h, 0));


        mvp = Matrix4f.mul(projection, view, null);
        Matrix4f.mul(mvp, model, mvp);

        glBindVertexArray(vao);
        glEnableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer);
        glBindVertexArray(0);

        mvp.store(mat4Buffer);
        mat4Buffer.flip();//prepare for read

        testfboTexture = new Image(w, h);
        testFBO = new FBOGraphics(testfboTexture);

    }

    @Override
    public void update(GameContainer gc, int delta) throws SlickException {
        if (Keyboard.isKeyDown(Keyboard.KEY_J)) {
            System.out.println("x: " + Mouse.getX() + " |y: " + Mouse.getY());
        }
    }

    @Override
    public void render(GameContainer gc, Graphics g) throws SlickException {
        g.clear();
        //make sure opengl calls don't disturb slick - probably unecessary
        SlickCallable.enterSafeBlock();

        //distance step
        distanceFBO.setAsActiveFBO();
//        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);

        glClear(GL_COLOR_BUFFER_BIT);
        shaderTester.useProgram(shaderTester.distanceProgram);
        GL11.glBindTexture(GL_TEXTURE_2D,cat4.getTexture().getTextureID());
        GL20.glUniformMatrix4(mvpDistanceProgram, false, mat4Buffer);
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);
        GL30.glBindVertexArray(0);
        GL11.glBindTexture(GL_TEXTURE_2D,0);
        shaderTester.stopUsingProgram();


        //************ distortion step
        distortionFBO.setAsActiveFBO();
        //glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
        glClear(GL_COLOR_BUFFER_BIT);
        shaderTester.useProgram(shaderTester.distortionProgram);

        GL11.glBindTexture(GL_TEXTURE_2D,distanceFBO.getTextureHandle());
        GL20.glUniformMatrix4(mvpDistortionProgam, false, mat4Buffer);

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);
        GL30.glBindVertexArray(0);
        GL11.glBindTexture(GL_TEXTURE_2D,0);


        shaderTester.stopUsingProgram();

        //************* draw shadows
        shadowsFBO.setAsActiveFBO();
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT,0);

        glClear(GL_COLOR_BUFFER_BIT);

        shaderTester.useProgram(shaderTester.drawProgram);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, distortionFBO.getTextureHandle());
        GL20.glUniform1i(shadowMapSamplerLoaction_DrawProgram, 0);

        GL20.glUniformMatrix4(mvpDrawProgam, false, mat4Buffer);
        GL20.glUniform2f(renderTargetSizeLoaction_DrawProgram, w, h);

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);
        GL30.glBindVertexArray(0);

        shaderTester.stopUsingProgram();

        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);

        SlickCallable.leaveSafeBlock();

        //switch back to the defaul lwjgl rendering context and draw the shadowcaster image
        Graphics.setCurrent(g);
        g.drawImage(cat4,0,0);
    }
}