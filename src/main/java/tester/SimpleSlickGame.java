package tester;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.*;
import org.newdawn.slick.opengl.ImageData;
import org.newdawn.slick.opengl.SlickCallable;
import org.newdawn.slick.opengl.pbuffer.FBOGraphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import static org.lwjgl.opengl.EXTFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class SimpleSlickGame extends BasicGame {
  final static int w = 1024;
  final static int h = 1024;
  int colorTextureID;
  int framebufferID;
  int depthRenderBufferID;
  int angle = 0;
  ShaderTester shaderTester;
  Image gandalf;
  Image empty;
  Image shadowCasters;
  FBOGraphics shadowCastersFBO;

  Integer distanceCalcTexture;
  Integer distanceCalcFBO;
  Integer distortionCalcTexture;
  Integer distortionCalcFBO;
  ArrayList<Integer> reductionCalcFBO = new ArrayList<>();
  ArrayList<Integer> reductionCalcTexture = new ArrayList<>();
  Integer shadowsTexture;
  Integer shadowsFBO;

  int targetTextureDimensionLocation_ReductionProgram;
  int textureDimensionsLocation_DistanceProgram;
  int renderTargetSizeLoaction_DrawProgram;
  int shadowMapSamplerLoaction_DrawProgram;
  int targetTextureLocation_ReductionProgram;
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
    empty = new Image(w, h);
    shaderTester = new ShaderTester();
    GL11.glEnable(GL11.GL_TEXTURE);

    textureLocation = GL20.glGetUniformLocation(shaderTester.testProgram, "texture");
    secondTextureLocation = GL20.glGetUniformLocation(shaderTester.testProgram, "texture1");
    resolutionLocation = GL20.glGetUniformLocation(shaderTester.testProgram, "resolution");
    mvpLocation = GL20.glGetUniformLocation(shaderTester.testProgram, "mvp");

    targetTextureDimensionLocation_ReductionProgram = GL20.glGetUniformLocation(shaderTester.reductionProgram, "targetTextureDimensions");
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
    targetTextureLocation_ReductionProgram = GL20.glGetUniformLocation(shaderTester.reductionProgram, "targetTexture");


    shadowCasters = new Image(w, h);
    shadowCastersFBO = new FBOGraphics(shadowCasters);

    shaderTester.genFBOwithRGBA32F(distanceCalcFBO,distanceCalcTexture,w,h);
    shaderTester.genFBOwithRGBA32F(distortionCalcFBO,distortionCalcTexture,w,h);
    shaderTester.genFBOwithRGBA32F(shadowsFBO,shadowsTexture,w,h);


    int nReductions = (int) (Math.log(w) / Math.log(2) + 1e-12);
    for (int i = 1; i < nReductions; i++) {
      reductionCalcTexture.add(new Integer(0));
      reductionCalcFBO.add(new Integer(0));

      shaderTester.genFBOwithRGBA32F(reductionCalcFBO.get(reductionCalcFBO.size() - 1), reductionCalcTexture.get(reductionCalcTexture.size() - 1), (int) Math.pow(2, i), h);
    }


    vertexBuffer = glGenBuffers();
    glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
    glBufferData(GL15.GL_ARRAY_BUFFER, (FloatBuffer) BufferUtils.createFloatBuffer(12).put(new float[]{
        0, 0, 0.0f,
        w, 0, 0.0f,
        w, h, 0.0f,
        0, h, 0.0f})
        .flip(), GL15.GL_STATIC_DRAW);

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

    vao = glGenVertexArrays();


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



  }

  @Override
  public void update(GameContainer gc, int delta) throws SlickException {
    angle += 0.15f * delta;
  }

  @Override
  public void render(GameContainer gc, Graphics g) throws SlickException {
    //************ draw shadowmap
    Graphics.setCurrent(shadowCastersFBO);
    shadowCastersFBO.drawImage(gandalf, 100, 100);
    shadowCastersFBO.drawImage(gandalf, w - gandalf.getWidth(), 0);
    shadowCastersFBO.drawImage(gandalf, 20, h - gandalf.getHeight());
    shadowCastersFBO.drawImage(gandalf, 200 + w / 2, 100 + h / 2);

    //************* distance step
//    Graphics.setCurrent(distanceCalcFBO);
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, distanceCalcFBO);
    SlickCallable.enterSafeBlock();
    shaderTester.useProgram(shaderTester.distanceProgram);

    GL13.glActiveTexture(GL13.GL_TEXTURE0);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowCasters.getTexture().getTextureID());
    GL20.glUniform1i(shadowCastersTextureLocation_DistanceProgam, 0);

    GL20.glUniform2f(textureDimensionsLocation_DistanceProgram, shadowCasters.getWidth(), shadowCasters.getHeight());
    GL20.glUniformMatrix4(mvpDistanceProgram, false, mat4Buffer);

    glBindVertexArray(vao);
    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);
    GL30.glBindVertexArray(0);

    shaderTester.stopUsingProgram();
    SlickCallable.leaveSafeBlock();


    //************ distortion step
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, distortionCalcFBO);
    SlickCallable.enterSafeBlock();
    shaderTester.useProgram(shaderTester.distortionProgram);

    GL13.glActiveTexture(GL13.GL_TEXTURE0);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, distanceCalcTexture);
    GL20.glUniform1i(inputSamplerLocation_DistortionProgram, 0);

    GL20.glUniformMatrix4(mvpDistortionProgam, false, mat4Buffer);

    glBindVertexArray(vao);
    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);
    GL30.glBindVertexArray(0);

    shaderTester.stopUsingProgram();
    SlickCallable.leaveSafeBlock();

    //*************** reductionCalcFBO
    shaderTester.useProgram(shaderTester.reductionProgram);
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, reductionCalcFBO.get(reductionCalcFBO.size() - 1));

    GL20.glUniform2i(targetTextureDimensionLocation_ReductionProgram, w / 2, h);
    GL13.glActiveTexture(GL13.GL_TEXTURE0);
    GL11.glBindTexture(GL_TEXTURE_2D, reductionCalcTexture.get(reductionCalcTexture.size() - 1));
    GL20.glUniform1i(targetTextureLocation_ReductionProgram, 0);
    GL13.glActiveTexture(GL13.GL_TEXTURE1);
    GL11.glBindTexture(GL_TEXTURE_2D,distortionCalcTexture);
    GL20.glUniform1i(inputSamplerLoaction_ReductionProgram, 1);

    reductionCalcFBO.get(reductionCalcTexture.size() - 1).drawImage(distortionCalcTexture, 0, 0);

    for (int i = 1; i < reductionCalcFBO.size(); i++) {
      int n = reductionCalcFBO.size() - 1 - i;
      glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, (reductionCalcFBO.get(n)));
      int currentTexture = reductionCalcTexture.get(n + 1);
      GL20.glUniform2i(targetTextureDimensionLocation_ReductionProgram, reductionCalcTexture.get(n).getWidth(), h);
      GL13.glActiveTexture(GL13.GL_TEXTURE0);
      GL11.glBindTexture(GL_TEXTURE_2D, reductionCalcTexture.get(n));
      GL20.glUniform1i(targetTextureLocation_ReductionProgram, 0);
      GL13.glActiveTexture(GL13.GL_TEXTURE1);
      GL11.glBindTexture(GL_TEXTURE_2D,currentTexture);
      GL20.glUniform1i(inputSamplerLoaction_ReductionProgram, 1);
      
      reductionCalcFBO.get(n).drawImage(currentTexture, 0, 0);
    }
    shaderTester.stopUsingProgram();

    //************* draw shadows
    Graphics.setCurrent(shadowsFBO);
    SlickCallable.enterSafeBlock();
    shaderTester.useProgram(shaderTester.drawProgram);

    GL13.glActiveTexture(GL13.GL_TEXTURE0);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, reductionCalcTexture.get(0).getTexture().getTextureID());
    GL20.glUniform1i(shadowMapSamplerLoaction_DrawProgram, 0);

    GL20.glUniformMatrix4(mvpDrawProgam, false, mat4Buffer);
    GL20.glUniform2f(renderTargetSizeLoaction_DrawProgram,shadowsTexture.getWidth(), shadowsTexture.getHeight());

    glBindVertexArray(vao);
    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);
    GL30.glBindVertexArray(0);

    shaderTester.stopUsingProgram();
    SlickCallable.leaveSafeBlock();


    //******** start drawing to screen
    Graphics.setCurrent(g);
    g.clear();
    g.drawImage(distortionCalcTexture,0,0);


    g.flush();

  }
}