/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
package main.java.demos;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.libffi.Closure;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static main.java.Utils.Utils.createRasterProgram;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_VERSION_UNAVAILABLE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetFloat;
import static org.lwjgl.opengl.GL11.glTexParameterf;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL12.glTexSubImage3D;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Texture2DArrayMipmapping {

  private long window;
  private int width = 1024;
  private int height = 768;
  private int texSize = 1024;

  private int tex;
  private int vao;
  private int program;
  private int viewProjMatrixUniform;

  private Matrix4f viewProjMatrix = new Matrix4f();
  private ByteBuffer matrixByteBuffer = BufferUtils.createByteBuffer(4 * 16);

  GLCapabilities caps;
  GLFWErrorCallback errCallback;
  GLFWKeyCallback keyCallback;
  GLFWFramebufferSizeCallback fbCallback;
  Closure debugProc;

  private void init() throws IOException {
    glfwSetErrorCallback(errCallback = new GLFWErrorCallback() {
      private GLFWErrorCallback delegate = GLFWErrorCallback.createPrint(System.err);

      @Override
      public void invoke(int error, long description) {
        if (error == GLFW_VERSION_UNAVAILABLE)
          System.err.println("This demo requires OpenGL 3.0 or higher.");
        delegate.invoke(error, description);
      }

      @Override
      public void release() {
        delegate.release();
        super.release();
      }
    });

    if (glfwInit() != GL_TRUE)
      throw new IllegalStateException("Unable to initialize GLFW");

    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
    glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);

    window = glfwCreateWindow(width, height, "Mipmapping with 2D array textures", NULL, NULL);
    if (window == NULL) {
      throw new AssertionError("Failed to create the GLFW window");
    }

    glfwSetFramebufferSizeCallback(window, fbCallback = new GLFWFramebufferSizeCallback() {
      @Override
      public void invoke(long window, int width, int height) {
        if (width > 0
            && height > 0
            && (Texture2DArrayMipmapping.this.width != width || Texture2DArrayMipmapping.this.height != height)) {
          Texture2DArrayMipmapping.this.width = width;
          Texture2DArrayMipmapping.this.height = height;
        }
      }
    });

    glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
      @Override
      public void invoke(long window, int key, int scancode, int action, int mods) {
        if (action != GLFW_RELEASE)
          return;

        if (key == GLFW_KEY_ESCAPE) {
          glfwSetWindowShouldClose(window, GL_TRUE);
        }
      }
    });

    GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
    glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
    glfwMakeContextCurrent(window);
    glfwSwapInterval(0);
    glfwShowWindow(window);
    caps = GL.createCapabilities();
    debugProc = GLUtil.setupDebugMessageCallback();

		/* Create all needed GL resources */
    createTexture();
    createVao();
    program = createRasterProgram("res/shaderinos/texture2dArrayMipmap.vert", "res/shaderinos/texture2dArrayMipmap.frag");
    initProgram();
  }

  private void createTexture() {
    this.tex = glGenTextures();
    glBindTexture(GL_TEXTURE_2D_ARRAY, this.tex);
    glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    /* Add maximum anisotropic filtering, if available */
    if (caps.GL_EXT_texture_filter_anisotropic) {
      glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
      float maxAnisotropy = glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
      glTexParameterf(GL_TEXTURE_2D_ARRAY, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
    } else {
      glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
    }
    glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA32F, texSize, texSize, 2, 0, GL_RGBA,
        GL_FLOAT, (ByteBuffer) null);
    ByteBuffer bb = BufferUtils.createByteBuffer(3 * texSize * texSize);
    int checkSize = 5;
		/* Generate some checker board pattern */
    for (int y = 0; y < texSize; y++) {
      for (int x = 0; x < texSize; x++) {
        if (((x / checkSize + y / checkSize) % 2) == 0) {
          bb.put((byte) 255).put((byte) 255).put((byte) 255);
        } else {
          bb.put((byte) 0).put((byte) 0).put((byte) 0);
        }
      }
    }
    bb.flip();
    glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, 0, texSize, texSize, 1, GL_RGB, GL_UNSIGNED_BYTE, bb);
		/* Generate some diagonal lines for the second layer */
    for (int y = 0; y < texSize; y++) {
      for (int x = 0; x < texSize; x++) {
        if ((x + y) / 3 % 3 == 0) {
          bb.put((byte) 255).put((byte) 255).put((byte) 255);
        } else {
          bb.put((byte) 0).put((byte) 0).put((byte) 0);
        }
      }
    }
    bb.flip();
    glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, 1, texSize, texSize, 1, GL_RGB, GL_UNSIGNED_BYTE, bb);
    glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
    glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
  }


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


  /**
   * Initialize the shader program.
   */
  private void initProgram() {
    glUseProgram(this.program);
    viewProjMatrixUniform = glGetUniformLocation(this.program, "viewProjMatrix");
    glUseProgram(0);
  }

  private void update() {
    viewProjMatrix.setPerspective((float) Math.toRadians(60.0f), (float) width / height, 0.01f, 100.0f)
        .lookAt(0.0f, 1.0f, 5.0f,
            0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f);
  }

  private void render() {
    glUseProgram(this.program);

    glUniformMatrix4fv(viewProjMatrixUniform, 1, false, viewProjMatrix.get(matrixByteBuffer));

    glBindVertexArray(vao);
    glBindTexture(GL_TEXTURE_2D_ARRAY, tex);
    glDrawArrays(GL_TRIANGLES, 0, 6);
    glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
    glBindVertexArray(0);

    glUseProgram(0);
  }

  private void loop() {
    while (glfwWindowShouldClose(window) == GL_FALSE) {
      glfwPollEvents();
      glViewport(0, 0, width, height);
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

      update();
      render();

      glfwSwapBuffers(window);
    }
  }

  private void run() {
    try {
      init();
      loop();

      if (debugProc != null) {
        debugProc.release();
      }

      errCallback.release();
      keyCallback.release();
      fbCallback.release();
      glfwDestroyWindow(window);
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      glfwTerminate();
    }
  }

  public static void main(String[] args) {
    new Texture2DArrayMipmapping().run();
  }

}