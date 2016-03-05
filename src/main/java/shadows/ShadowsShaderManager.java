package shadows;

/**
 * Created by P on 05.08.2015.
 */
public class ShadowsShaderManager {

    public final ShaderProgram distanceProgram;
    public final ShaderProgram distortionProgram;
    public final ShaderProgram reductionProgram;
    public final ShaderProgram drawProgram;
    public final ShaderProgram blurProgram;

    public ShadowsShaderManager() {
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
    }

}

