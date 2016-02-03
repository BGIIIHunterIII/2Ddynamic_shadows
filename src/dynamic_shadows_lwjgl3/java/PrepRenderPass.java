package main.java;

import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import static main.java.Utils.Utils.*;

/**
 * Created by P on 28.12.2015.
 */
public class PrepRenderPass {
  int vao;
  int distanceProgram;
  int uniformArrayProgram;

  /**
   * Simple fullscreen quad.
   */
  private void createVao() {
    this.vao = glGenVertexArrays();
    int vbo = glGenBuffers();
    glBindVertexArray(vao);
    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    ByteBuffer bb = BufferUtils.createByteBuffer(4 * 2 * 6);
    FloatBuffer fv = bb.asFloatBuffer();
    fv.put(-1.0f).put(-1.0f);
    fv.put(1.0f).put(-1.0f);
    fv.put(1.0f).put(1.0f);
    fv.put(1.0f).put(1.0f);
    fv.put(-1.0f).put(1.0f);
    fv.put(-1.0f).put(-1.0f);
    glBufferData(GL_ARRAY_BUFFER, bb, GL_STATIC_DRAW);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
  }

  public PrepRenderPass(){
  }

  public void init() throws IOException {
    createVao();
    distanceProgram = createRasterProgram("res/shaderinos/distance.vert","res/shaderinos/distance.frag");
    uniformArrayProgram = createRasterProgram("res/shaderinos/uniformarray.vert","res/shaderinos/uniformarray.frag");


  }

}
