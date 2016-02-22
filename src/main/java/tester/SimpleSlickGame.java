package tester;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Matrix4f;
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

    ShaderTester shaderTester;
    Image cat4;
    Image shadowCasters;
    FBOGraphics shadowCastersFBO;
    int targetTextureDimensionLocation_ReductionProgram;
    int textureDimensionsLocation_DistanceProgram;
    int renderTargetSizeLocation_DrawProgram;
    int mvpDistanceProgram;
    int mvpDistortionProgam;
    int mvpReductionProgam;
    int mvpDrawProgam;
    int mvpBlurProgram;
    int directionLocation_BlurProgram;
    int indicesBuffer;
    int vertexBuffer;
    int uvBuffer;
    FloatBuffer mvpMatrixBuffer = BufferUtils.createFloatBuffer(16);
    QuadVAO vao;
    UpdateableQuadVAO streamDrawVAO;

    FrameBuffer distanceFBO;
    FrameBuffer distortionFBO;
    final ArrayList<FrameBuffer> reductionCalcFBO = new ArrayList<>();
    FrameBuffer shadowsFBO;
    FrameBuffer blurFBO;

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
        float z_orth = -2.0f / (far - near); //not really needed

        float tx = -(right + left) / (right - left);
        float ty = -(top + bottom) / (top - bottom);
        float tz = -(far + near) / (far - near);

        m.m00 = x_orth;
        m.m11 = y_orth;
        m.m22 = z_orth;
        m.m30 = tx;
        m.m31 = ty;
        m.m32 = tz;
        m.m33 = 1;
        return m;
    }

    @Override
    public void init(GameContainer gc) throws SlickException {
        cat4 = new Image("res/sprites/entities/cat4.png");

        shaderTester = new ShaderTester();


        targetTextureDimensionLocation_ReductionProgram = GL20.glGetUniformLocation(shaderTester.reductionProgram, "sourceDimensions");
        textureDimensionsLocation_DistanceProgram = GL20.glGetUniformLocation(shaderTester.distanceProgram, "textureDimension");
        renderTargetSizeLocation_DrawProgram = GL20.glGetUniformLocation(shaderTester.drawProgram, "renderTargetSize");
        mvpDistanceProgram = GL20.glGetUniformLocation(shaderTester.distanceProgram, "mvp");
        mvpDistortionProgam = GL20.glGetUniformLocation(shaderTester.distortionProgram, "mvp");
        mvpReductionProgam = GL20.glGetUniformLocation(shaderTester.reductionProgram, "mvp");
        mvpDrawProgam = GL20.glGetUniformLocation(shaderTester.drawProgram, "mvp");
        mvpBlurProgram = GL20.glGetUniformLocation(shaderTester.blurProgram, "mvp");
        directionLocation_BlurProgram = GL20.glGetUniformLocation(shaderTester.blurProgram, "dir");


        shadowCasters = new Image(w, h);
        shadowCastersFBO = new FBOGraphics(shadowCasters);

        distanceFBO = new FrameBuffer(FrameBuffer.type.FLOAT, false, w, h, GL_LINEAR);
        distortionFBO = new FrameBuffer(FrameBuffer.type.FLOAT, false, w, h, GL_NEAREST);
        shadowsFBO = new FrameBuffer(FrameBuffer.type.FLOAT, false, w, h, GL_LINEAR);
        blurFBO = new FrameBuffer(FrameBuffer.type.FLOAT, false, w, h, GL_LINEAR);

        final int nReductions = (int) (Math.log(w) / Math.log(2) + 1e-12);
        for (int i = 1; i < nReductions; i++) {
            reductionCalcFBO.add(new FrameBuffer(FrameBuffer.type.FLOAT, false, (int) Math.pow(2, i), h, GL_NEAREST));
        }

        vao = new QuadVAO(w,h);
        streamDrawVAO = new UpdateableQuadVAO(w,h);


        glViewport(0, 0, w, h); //not neeeded but why? TODO
        Matrix4f model = new Matrix4f();
        Matrix4f view = new Matrix4f();
        Matrix4f projection = toOrtho2D(null, 0, 0, w, h, 1, -1);
        // MVP = M * V * P;
        Matrix4f mvp = Matrix4f.mul(model, Matrix4f.mul(view, projection, null), null);
        mvp.store(mvpMatrixBuffer);
        mvpMatrixBuffer.flip();//prepare for read



        glDisable(GL_DEPTH_TEST);
        glClearColor(0, 0, 0, 1);

        int glError = glGetError();
        if (glError != 0) System.err.println("gl error during initalization: " + glError);
    }

    @Override
    public void update(GameContainer gc, int delta) throws SlickException {

    }

    @Override
    public void render(GameContainer gc, Graphics g) throws SlickException {
        g.clear();
        //************ draw shadowmap
        Graphics.setCurrent(shadowCastersFBO);
        shadowCastersFBO.clear();

        //draw centered around the mouse
        shadowCastersFBO.drawImage(cat4, Mouse.getX() - cat4.getWidth() / 2, Mouse.getY() - cat4.getHeight() / 2);
        //shadowCastersFBO.drawImage(cat4, 0, 0);


        //make sure opengl calls don't disturb slick - probably unecessary
        SlickCallable.enterSafeBlock();

        //************* distance step
        //select framebuffer
        distanceFBO.setAsActiveFBO();
        glClear(GL_COLOR_BUFFER_BIT);

        //activate shader, while in use it will affect all drawing operations
        shaderTester.useProgram(shaderTester.distanceProgram);

        //send uniforms to the shader
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowCasters.getTexture().getTextureID());

        GL20.glUniform2f(textureDimensionsLocation_DistanceProgram, shadowCasters.getWidth(), shadowCasters.getHeight());
        GL20.glUniformMatrix4(mvpDistanceProgram, false, mvpMatrixBuffer);
        vao.drawQuad();

        shaderTester.stopUsingProgram();

        //************ distortion step
        distortionFBO.setAsActiveFBO();
        glClear(GL_COLOR_BUFFER_BIT);
        shaderTester.useProgram(shaderTester.distortionProgram);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, distanceFBO.getTextureHandle());
        GL20.glUniformMatrix4(mvpDistortionProgam, false, mvpMatrixBuffer);
        vao.drawQuad();

        shaderTester.stopUsingProgram();

        //*************** reductionCalcFBO
        shaderTester.useProgram(shaderTester.reductionProgram);

        reductionCalcFBO.get(reductionCalcFBO.size() - 1).setAsActiveFBO();
        glClear(GL_COLOR_BUFFER_BIT);

        GL20.glUniform2f(targetTextureDimensionLocation_ReductionProgram, 1.0f / distortionFBO.getWidth(), 1.0f / distortionFBO.getHeight());
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, distortionFBO.getTextureHandle());
        GL20.glUniformMatrix4(mvpReductionProgam, false, mvpMatrixBuffer);
        streamDrawVAO.update(w/2,h);
        QuadVAO.drawWithCurrentlyBoundVAO();

        for (int i = 1; i < reductionCalcFBO.size(); i++) {
            int n = reductionCalcFBO.size() - 1 - i;
            reductionCalcFBO.get(n).setAsActiveFBO();
            glClear(GL_COLOR_BUFFER_BIT);
            GL20.glUniform2f(targetTextureDimensionLocation_ReductionProgram, 1.0f / reductionCalcFBO.get(n + 1).getWidth(), 1.0f / reductionCalcFBO.get(n + 1).getHeight());
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, reductionCalcFBO.get(n + 1).getTextureHandle());
            GL20.glUniformMatrix4(mvpReductionProgam, false, mvpMatrixBuffer);

            streamDrawVAO.update(reductionCalcFBO.get(n).getWidth(), reductionCalcFBO.get(n).getHeight());
            QuadVAO.drawWithCurrentlyBoundVAO();

        }
        shaderTester.stopUsingProgram();


        //************* draw shadows
        shadowsFBO.setAsActiveFBO();
        glClear(GL_COLOR_BUFFER_BIT);
        shaderTester.useProgram(shaderTester.drawProgram);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, reductionCalcFBO.get(0).getTextureHandle());

        GL20.glUniformMatrix4(mvpDrawProgam, false, mvpMatrixBuffer);
        GL20.glUniform2f(renderTargetSizeLocation_DrawProgram, w, h);
        vao.drawQuad();

        shaderTester.stopUsingProgram();

        //********************************
        // blur filter vertical
        blurFBO.setAsActiveFBO();

        glClear(GL_COLOR_BUFFER_BIT);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowsFBO.getTextureHandle());
        shaderTester.useProgram(shaderTester.blurProgram);
        GL20.glUniform2f(directionLocation_BlurProgram, 0, 1f);
        GL20.glUniformMatrix4(mvpBlurProgram, false, mvpMatrixBuffer);

        vao.drawQuad();

        //blur horizontal to screen
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
        GL20.glUniform2f(directionLocation_BlurProgram, 1f, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowsFBO.getTextureHandle());
        vao.drawQuad();

        shaderTester.stopUsingProgram();
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
        SlickCallable.leaveSafeBlock();

        //switch back to the defaul lwjgl rendering context and draw the shadowcaster image
        Graphics.setCurrent(g);

        g.drawImage(cat4, Mouse.getX() - cat4.getWidth() / 2, Mouse.getY() - cat4.getHeight() / 2);

        int glError = glGetError();
        if (glError != 0) System.err.println("gl error: " + glError);
    }
}