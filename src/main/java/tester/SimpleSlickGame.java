package tester;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Matrix4f;
import org.newdawn.slick.*;
import org.newdawn.slick.opengl.pbuffer.FBOGraphics;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import static org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glBindFramebufferEXT;
import static org.lwjgl.opengl.GL11.*;

public class SimpleSlickGame extends BasicGame {
    final static int w = 512;
    final static int h = 512;

    ShadowsShaderManager shadowsShaderManager;
    Image cat4;
    Image shadowCasters;
    FBOGraphics shadowCastersFBO;
    FBOGraphics shadow;
    Image shadowTexture;
    int indicesBuffer;
    int vertexBuffer;
    int uvBuffer;
    FloatBuffer mvpMatrixBuffer = BufferUtils.createFloatBuffer(16);
    QuadVAO vao;
    UpdatableQuadVAO streamDrawVAO;

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
        shadowsShaderManager = new ShadowsShaderManager();


        shadowCasters = new Image(w, h);
        shadowCastersFBO = new FBOGraphics(shadowCasters);
        shadowTexture = new Image(w, h);
        shadow = new FBOGraphics(shadowTexture);

        distanceFBO = new FrameBuffer(FrameBuffer.type.FLOAT, false, w, h, GL_LINEAR);
        distortionFBO = new FrameBuffer(FrameBuffer.type.FLOAT, false, w, h, GL_NEAREST);
        shadowsFBO = new FrameBuffer(FrameBuffer.type.FLOAT, false, w, h, GL_LINEAR);
        blurFBO = new FrameBuffer(FrameBuffer.type.FLOAT, false, w, h, GL_LINEAR);

        final int nReductions = (int) (Math.log(w) / Math.log(2) + 1e-12);
        for (int i = 1; i < nReductions; i++) {
            reductionCalcFBO.add(new FrameBuffer(FrameBuffer.type.FLOAT, false, (int) Math.pow(2, i), h, GL_NEAREST));
        }

        vao = new QuadVAO(w, h);
        streamDrawVAO = new UpdatableQuadVAO(w, h);

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

        renderShadows(shadowCasters, shadow);

        //switch back to the defaul lwjgl rendering context and draw the shadowcaster image
        Graphics.setCurrent(g);
        g.drawImage(shadowTexture, 0, 0);
        g.drawImage(cat4, Mouse.getX() - cat4.getWidth() / 2, Mouse.getY() - cat4.getHeight() / 2);

        //check for errors
        int glError = glGetError();
        if (glError != 0) System.err.println("gl error: " + glError);
    }

    /**
     * all textures are implicitly sent to the default texture unit (GL13.GL_TEXTURE0);
     *
     * @param shadowCasters an Image where opaque pixels throw shadows
     * @param target        output is stored in the texture of target FBO
     */
    private void renderShadows(Image shadowCasters, FBOGraphics target) {

        //************* distance step
        //select framebuffer
        distanceFBO.setAsActiveFBO();
        glClear(GL_COLOR_BUFFER_BIT);

        //activate shader, while in use it will affect all drawing operations
        shadowsShaderManager.distanceProgram.useProgram();
        shadowsShaderManager.distanceProgram.sendUniform2f(
                "textureDimension", shadowCasters.getWidth(), shadowCasters.getHeight());
        shadowsShaderManager.distanceProgram.sendUniformMatrix4("mvp", mvpMatrixBuffer);
        shadowCasters.bind();
        vao.drawQuad();

        //************ distortion step
        distortionFBO.setAsActiveFBO();
        glClear(GL_COLOR_BUFFER_BIT);
        shadowsShaderManager.distortionProgram.useProgram();
        shadowsShaderManager.distortionProgram.sendUniformMatrix4("mvp", mvpMatrixBuffer);

        distanceFBO.bindTexture();
        vao.drawQuad();

        //*************** reductionCalcFBO
        shadowsShaderManager.reductionProgram.useProgram();

        reductionCalcFBO.get(reductionCalcFBO.size() - 1).setAsActiveFBO();
        glClear(GL_COLOR_BUFFER_BIT);

        shadowsShaderManager.reductionProgram.sendUniform2f(
                "sourceDimensions", 1.0f / distortionFBO.getWidth(), 1.0f / distortionFBO.getHeight());
        shadowsShaderManager.reductionProgram.sendUniformMatrix4("mvp", mvpMatrixBuffer);
        distortionFBO.bindTexture();
        streamDrawVAO.update(w / 2, h);
        QuadVAO.drawWithCurrentlyBoundVAO();

        for (int i = 1; i < reductionCalcFBO.size(); i++) {
            int n = reductionCalcFBO.size() - 1 - i;
            reductionCalcFBO.get(n).setAsActiveFBO();
            glClear(GL_COLOR_BUFFER_BIT);
            shadowsShaderManager.reductionProgram.sendUniform2f(
                    "sourceDimensions", 1.0f / reductionCalcFBO.get(n + 1).getWidth(), 1.0f / reductionCalcFBO.get(n + 1).getHeight());
            shadowsShaderManager.reductionProgram.sendUniformMatrix4("mvp", mvpMatrixBuffer);
            reductionCalcFBO.get(n + 1).bindTexture();

            streamDrawVAO.update(reductionCalcFBO.get(n).getWidth(), reductionCalcFBO.get(n).getHeight());
            QuadVAO.drawWithCurrentlyBoundVAO();

        }

        //************* draw shadows
        shadowsFBO.setAsActiveFBO();
        glClear(GL_COLOR_BUFFER_BIT);
        shadowsShaderManager.drawProgram.useProgram();

        reductionCalcFBO.get(0).bindTexture();

        shadowsShaderManager.drawProgram.sendUniformMatrix4("mvp", mvpMatrixBuffer);
        shadowsShaderManager.drawProgram.sendUniform2f("renderTargetSize", w, h);
        vao.drawQuad();

        //********************************
        // blur filter vertical
        blurFBO.setAsActiveFBO();
        shadowsShaderManager.blurProgram.useProgram();

        glClear(GL_COLOR_BUFFER_BIT);
        shadowsFBO.bindTexture();
        //vertical blur
        shadowsShaderManager.blurProgram.sendUniform2f("dir", 0, 1);
        shadowsShaderManager.blurProgram.sendUniformMatrix4("mvp", mvpMatrixBuffer);
        vao.drawQuad();

        //blur horizontal to target FBO
        Graphics.setCurrent(target);
        //horizontal blur
        shadowsShaderManager.blurProgram.sendUniform2f("dir", 1, 0);
        shadowsFBO.bindTexture();
        vao.drawQuad();

        ShaderProgram.disablePrograms();
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
    }
}