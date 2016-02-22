package tester;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

/**
 * Created by P on 05.08.2015.
 */
public class ShaderTester {

    int distanceProgram;
    int distortionProgram;
    int reductionProgram;
    int drawProgram;
    int blurProgram;

    int vaoStream;
    int streamVBO;
    int indicesBuffer;
    int uvBuffer;

    public ShaderTester() {
        init();
    }

    /**
     * loads a shader program from file
     *
     * @param vertPath vertex shader
     * @param fragPath fragment shader
     * @return a valid programID
     * <p>
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
                throw new IllegalArgumentException("couldn't load shaders, likely path is incorrect");
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
        distanceProgram = loadShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/calcDistances.frag");
        distortionProgram = loadShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/distortImage.frag");
        reductionProgram = loadShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/horizontalReduction.frag");
        drawProgram = loadShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/drawShadows.frag");
        blurProgram = loadShaderProgram("res/shaderinos/passthroughVBO.vert", "res/shaderinos/simpleGaussianBlur.frag");

        //******** init stream voa environment
        streamVBO = glGenBuffers();
        indicesBuffer = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, (ShortBuffer) BufferUtils.createShortBuffer(6).put(new short[]{
                0, 1, 2,
                2, 3, 0
        }).flip(), GL_STATIC_DRAW);

        uvBuffer = glGenBuffers();
        glBindBuffer(GL15.GL_ARRAY_BUFFER, uvBuffer);
        glBufferData(GL15.GL_ARRAY_BUFFER, (FloatBuffer) BufferUtils.createFloatBuffer(8).put(new float[]{
                0, 0,
                1, 0,
                1, 1,
                0, 1,

        }).flip(), GL_STATIC_DRAW);

        vaoStream = GL30.glGenVertexArrays();


        glBindVertexArray(vaoStream);
        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer);
        glBindVertexArray(0);


    }

    public void useProgram(int program) {
        ARBShaderObjects.glUseProgramObjectARB(program);
    }

    public void stopUsingProgram() {
        ARBShaderObjects.glUseProgramObjectARB(0);
    }


    /**
     * drawprogram and uniforms must be setup prior to calling this function!
     *
     * @param width  far rigth coordinate
     * @param height bottom y coordinate
     */
    public void updateVBOandDraw(int width, int height) {
        glBindBuffer(GL15.GL_ARRAY_BUFFER, streamVBO);
        glBufferData(GL15.GL_ARRAY_BUFFER, (FloatBuffer) BufferUtils.createFloatBuffer(12).put(new float[]{
                0, 0, 0.0f,
                width, 0, 0.0f,
                width, height, 0.0f,
                0, height, 0.0f})
                .flip(), GL15.GL_STREAM_DRAW);

        GL30.glBindVertexArray(vaoStream);

        glEnableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, streamVBO);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);
        GL30.glBindVertexArray(0);
    }

}

