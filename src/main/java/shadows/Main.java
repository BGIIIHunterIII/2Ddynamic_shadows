package shadows;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Matrix4f;
import org.newdawn.slick.*;
import org.newdawn.slick.opengl.pbuffer.FBOGraphics;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

public class Main extends BasicGame {
    final static int w = 512;
    final static int h = 512;

    ShadowsShaderManager shadowsShaderManager;
    Image cat4;
    //holds shadow-casting image
    FBOGraphics shadowCastersFBO;
    Image shadowCasters;
    //holds shadows
    FBOGraphics shadow;
    Image shadowTexture;
    FloatBuffer mvpMatrixBuffer = BufferUtils.createFloatBuffer(16);

    public Main(String gamename) {
        super(gamename);
    }

    public static void main(String[] args) {
        try {
            AppGameContainer appgc;
            appgc = new AppGameContainer(new Main("Simple Slick Game"));
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
        shadowCasters = new Image(w, h);
        shadowCastersFBO = new FBOGraphics(shadowCasters);
        shadowTexture = new Image(w, h);
        shadow = new FBOGraphics(shadowTexture);

        Matrix4f model = new Matrix4f();
        Matrix4f view = new Matrix4f();
        Matrix4f projection = toOrtho2D(null, 0, 0, w, h, 1, -1);
        // MVP = M * V * P;
        Matrix4f mvp = Matrix4f.mul(model, Matrix4f.mul(view, projection, null), null);
        mvp.store(mvpMatrixBuffer);
        mvpMatrixBuffer.flip();//prepare for read

        glDisable(GL_DEPTH_TEST);
        glClearColor(0, 0, 0, 1);

        shadowsShaderManager = new ShadowsShaderManager(mvpMatrixBuffer, w, h);

        int glError = glGetError();
        if (glError != 0) System.err.println("gl error during initalization: " + glError);
    }

    @Override
    public void update(GameContainer container, int delta) throws SlickException {

    }

    @Override
    public void render(GameContainer gc, Graphics g) throws SlickException {
        g.clear();
        //************ draw shadowmap
        Graphics.setCurrent(shadowCastersFBO);
        shadowCastersFBO.clear();

        //fill shadowcasters texture
        //draw image centered around the mouse
        shadowCastersFBO.drawImage(cat4, Mouse.getX() - cat4.getWidth() / 2, Mouse.getY() - cat4.getHeight() / 2);

        shadowsShaderManager.renderShadows(shadowCasters, shadow);

        Graphics.setCurrent(g);
        g.drawImage(shadowTexture, 0, 0);
        g.drawImage(cat4, Mouse.getX() - cat4.getWidth() / 2, Mouse.getY() - cat4.getHeight() / 2);

        //check for errors
        int glError = glGetError();
        if (glError != 0) System.err.println("gl error: " + glError);
    }
}