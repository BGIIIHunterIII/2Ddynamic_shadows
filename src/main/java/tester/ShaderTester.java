package tester;

import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;

/**
 * Created by P on 05.08.2015.
 */
public class ShaderTester {

    int distanceProgram;
    int distortionProgram;
    int reductionProgram;
    int drawProgram;
    int blurVProgram;
    int blurHProgram;

    int testProgram;

    int texture;
    int fbo;

    public ShaderTester() {
        init();
    }

    /**
     * loads a shader program from file
     *
     * @param vertPath vertex shader
     * @param fragPath fragment shader
     * @return a valid programID
     * <p/>
     * shaderprogram can be used like this
     * if(shaderLoadedCorrectly)
     * ARBShaderObjects.glUseProgramObjectARB(program);
     * shaders must be released after use
     * if(shaderLoadedCorrectly)
     * ARBShaderObjects.glUseProgramObjectARB(0);
     */
    public static int loadShaderProgram(String vertPath, String fragPath) {
        int vertShader = 0, fragShader = 0;
        int program;
        try {
            vertShader = createShader(vertPath, ARBVertexShader.GL_VERTEX_SHADER_ARB);
            fragShader = createShader(fragPath, ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        } finally {
            if (vertShader == 0 || fragShader == 0)
                throw new IllegalArgumentException("couldn't load shaders");
        }

        program = ARBShaderObjects.glCreateProgramObjectARB();

        if (program == 0) throw new UnsupportedOperationException("couln't create program");

        /*
        * if the vertex and fragment shaders setup sucessfully,
        * attach them to the shader program, link the sahder program
        * (into the GL context I suppose), and validate
        */
        ARBShaderObjects.glAttachObjectARB(program, vertShader);
        ARBShaderObjects.glAttachObjectARB(program, fragShader);

        ARBShaderObjects.glLinkProgramARB(program);
        if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
            System.err.println(ARBShaderObjects.glGetInfoLogARB(program, ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB)));
            throw new UnsupportedOperationException("couldn't link shaderprogram");
        }

        ARBShaderObjects.glValidateProgramARB(program);
        if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
            System.err.println(ARBShaderObjects.glGetInfoLogARB(program, ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB)));
            throw new RuntimeException("invalid program!");
        }
        return program;
    }

    private static int createShader(String filename, int shaderType) throws Exception {
        int shader = 0;
        try {
            shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);

            if (shader == 0)
                return 0;

            ARBShaderObjects.glShaderSourceARB(shader, readFileAsString(filename));
            ARBShaderObjects.glCompileShaderARB(shader);

            if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader " + filename + ": " + getLogInfo(shader));

            return shader;
        } catch (Exception exc) {
            ARBShaderObjects.glDeleteObjectARB(shader);
            throw exc;
        }
    }

    private static String readFileAsString(String filename) throws IOException {
        //TODO specify file encoding (this code will break on other platforms)
        return new String(Files.readAllBytes(Paths.get(filename)));

    }

    private static String getLogInfo(int obj) {
        return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
    }

    private void init() {
        testProgram = loadShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/testFragger.frag");
        distanceProgram = loadShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/calcDistances.frag");
        distortionProgram = loadShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/distortImage.frag");
        reductionProgram = loadShaderProgram("res/shaderinos/passthrough.vert", "res/shaderinos/horizontalReduction.frag");
        drawProgram = loadShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/drawShadows.frag");

    }

    public void calculateShadows() {
        //GL_Helper.renderToFBO(shadowCastersFBO,Config.WIDTH,Config.HEIGHT);
        //ARBShaderObjects.glUseProgramObjectARB(distanceProgram);

    }

    public void useProgram(int program) {
        ARBShaderObjects.glUseProgramObjectARB(program);
    }

    public void stopUsingProgram() {
        ARBShaderObjects.glUseProgramObjectARB(0);
    }

    public void genFBOwithRGBA32F(Integer fboID, Integer textureID, int width, int height){

        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width, height, 0, GL_RGBA, GL_FLOAT, (java.nio.ByteBuffer) null); // implicitly create new ByteBuffer

        IntBuffer buffer = ByteBuffer.allocateDirect(1 * 4).order(ByteOrder.nativeOrder()).asIntBuffer(); // allocate a 1 int byte buffer
        EXTFramebufferObject.glGenFramebuffersEXT(buffer); // generate
        fboID = buffer.get();

        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboID );
        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,
            GL11.GL_TEXTURE_2D, textureID, 0);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);




    }

}

