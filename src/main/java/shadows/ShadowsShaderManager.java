package shadows;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.opengl.pbuffer.FBOGraphics;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import static org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glBindFramebufferEXT;
import static org.lwjgl.opengl.GL11.*;

/**
 * Created by P on 05.08.2015.
 */
public class ShadowsShaderManager {

    private final ShaderProgram distanceProgram;
    private final ShaderProgram distortionProgram;
    private final ShaderProgram reductionProgram;
    private final ShaderProgram drawProgram;
    private final ShaderProgram blurProgram;

    FrameBuffer distanceFBO;
    FrameBuffer distortionFBO;
    final ArrayList<FrameBuffer> reductionCalcFBO = new ArrayList<>();
    FrameBuffer shadowsFBO;
    FrameBuffer blurFBO;
    QuadVAO vao;
    UpdatableQuadVAO streamDrawVAO;
    final FloatBuffer mvpMatrixBuffer;
    private final int w;
    private final int h;

    public ShadowsShaderManager(FloatBuffer mvpMatrixBuffer, int w, int h) {
        this.mvpMatrixBuffer = mvpMatrixBuffer;
        this.h = h;
        this.w = w;

        distanceProgram = new ShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/calcDistances.frag");
        distortionProgram = new ShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/distortImage.frag");
        reductionProgram = new ShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/horizontalReduction.frag");
        drawProgram = new ShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/drawShadows.frag");
        blurProgram = new ShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/simpleGaussianBlur.frag");


        init();
    }

    private void init() {
        reductionProgram.enableUniform("mvp", "sourceDimensions");
        distanceProgram.enableUniform("mvp", "textureDimension");
        drawProgram.enableUniform("mvp", "renderTargetSize");
        distortionProgram.enableUniform("mvp");
        blurProgram.enableUniform("mvp", "dir");

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
    }

    /**
     * all textures are implicitly sent to the default texture unit (GL13.GL_TEXTURE0);
     *
     * @param shadowCasters an Image where opaque pixels throw shadows
     * @param target        output is stored in the texture of target FBO
     */
    public void renderShadows(Image shadowCasters, FBOGraphics target) {

        //************* distance step
        //select framebuffer
        distanceFBO.setAsActiveFBO();
        glClear(GL_COLOR_BUFFER_BIT);

        //activate shader, while in use it will affect all drawing operations
        this.distanceProgram.useProgram();
        this.distanceProgram.sendUniform2f(
                "textureDimension", shadowCasters.getWidth(), shadowCasters.getHeight());
        this.distanceProgram.sendUniformMatrix4("mvp", mvpMatrixBuffer);
        shadowCasters.bind();
        vao.drawQuad();

        //************ distortion step
        distortionFBO.setAsActiveFBO();
        glClear(GL_COLOR_BUFFER_BIT);
        this.distortionProgram.useProgram();
        this.distortionProgram.sendUniformMatrix4("mvp", mvpMatrixBuffer);

        distanceFBO.bindTexture();
        vao.drawQuad();

        //*************** reductionCalcFBO
        this.reductionProgram.useProgram();

        reductionCalcFBO.get(reductionCalcFBO.size() - 1).setAsActiveFBO();
        glClear(GL_COLOR_BUFFER_BIT);

        this.reductionProgram.sendUniform2f(
                "sourceDimensions", 1.0f / distortionFBO.getWidth(), 1.0f / distortionFBO.getHeight());
        this.reductionProgram.sendUniformMatrix4("mvp", mvpMatrixBuffer);
        distortionFBO.bindTexture();
        streamDrawVAO.update(w / 2, h);
        QuadVAO.drawWithCurrentlyBoundVAO();

        for (int i = 1; i < reductionCalcFBO.size(); i++) {
            int n = reductionCalcFBO.size() - 1 - i;
            reductionCalcFBO.get(n).setAsActiveFBO();
            glClear(GL_COLOR_BUFFER_BIT);
            this.reductionProgram.sendUniform2f(
                    "sourceDimensions", 1.0f / reductionCalcFBO.get(n + 1).getWidth(), 1.0f / reductionCalcFBO.get(n + 1).getHeight());
            this.reductionProgram.sendUniformMatrix4("mvp", mvpMatrixBuffer);
            reductionCalcFBO.get(n + 1).bindTexture();

            streamDrawVAO.update(reductionCalcFBO.get(n).getWidth(), reductionCalcFBO.get(n).getHeight());
            QuadVAO.drawWithCurrentlyBoundVAO();

        }

        //************* draw shadows
        shadowsFBO.setAsActiveFBO();
        glClear(GL_COLOR_BUFFER_BIT);
        this.drawProgram.useProgram();

        reductionCalcFBO.get(0).bindTexture();

        this.drawProgram.sendUniformMatrix4("mvp", mvpMatrixBuffer);
        this.drawProgram.sendUniform2f("renderTargetSize", w, h);
        vao.drawQuad();

        //********************************
        // blur filter vertical
        blurFBO.setAsActiveFBO();
        this.blurProgram.useProgram();

        glClear(GL_COLOR_BUFFER_BIT);
        shadowsFBO.bindTexture();
        //vertical blur
        this.blurProgram.sendUniform2f("dir", 0, 1);
        this.blurProgram.sendUniformMatrix4("mvp", mvpMatrixBuffer);
        vao.drawQuad();

        //blur horizontal to target FBO
        Graphics.setCurrent(target);
        //horizontal blur
        this.blurProgram.sendUniform2f("dir", 1, 0);
        shadowsFBO.bindTexture();
        vao.drawQuad();

        ShaderProgram.disablePrograms();
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
    }
}

