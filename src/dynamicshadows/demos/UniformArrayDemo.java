package dynamicshadows.demos;
/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */


import dynamicshadows.Utils.FBO;
import dynamicshadows.Utils.Image;
import dynamicshadows.Utils.Utils;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.libffi.Closure;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
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
import static org.lwjgl.glfw.GLFW.nglfwGetFramebufferSize;
import static org.lwjgl.opengl.EXTFramebufferObject.*;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glBindAttribLocation;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform3fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindFragDataLocation;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;

/**
 * Simple demo to showcase the use of {@link GL20#glUniform3fv(int, FloatBuffer)}.
 *
 * @author Kai Burjack
 */
public class UniformArrayDemo {

  private long window;
  private int width = 512;
  private int height = 512;
  private Image catImage;

  private int vao;
  private int distanceProgram;
  private int uniformArrayProgram;
  private int distortionProgram;
  private int inverseDistortionProgram;

  private int vec3ArrayUniform;
  private int chosenUniform;
  private int arrayProgramTextureUniform;
  private int chosen = 0;
  private FloatBuffer colors = BufferUtils.createFloatBuffer(3 * 4);
  {
    colors.put(1).put(0).put(0); // red
    colors.put(0).put(1).put(0); // green
    colors.put(0).put(0).put(1); // blue
    colors.put(1).put(1).put(0); // yellow
    colors.flip();
  }
  private FBO distance = new FBO();
  private FBO distortion = new FBO();

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
          System.err
              .println("This demo requires OpenGL 3.0 or higher.");
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

    window = glfwCreateWindow(width, height, "Uniform array test", NULL,
        NULL);
    if (window == NULL) {
      throw new AssertionError("Failed to create the GLFW window");
    }
    System.out
        .println("Press 'up' or 'down' to cycle through some colors.");

    glfwSetFramebufferSizeCallback(window,
        fbCallback = new GLFWFramebufferSizeCallback() {
          @Override
          public void invoke(long window, int width, int height) {
            if (width > 0
                && height > 0
                && (UniformArrayDemo.this.width != width || UniformArrayDemo.this.height != height)) {
              UniformArrayDemo.this.width = width;
              UniformArrayDemo.this.height = height;
            }
          }
        });

    glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
      @Override
      public void invoke(long window, int key, int scancode, int action,
                         int mods) {
        if (action != GLFW_RELEASE)
          return;

        if (key == GLFW_KEY_ESCAPE) {
          glfwSetWindowShouldClose(window, GL_TRUE);
        } else if (key == GLFW_KEY_UP) {
          chosen = (chosen + 1) % 4;
        } else if (key == GLFW_KEY_DOWN) {
          chosen = (chosen + 3) % 4;
        }
      }
    });

    GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
    glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
    glfwMakeContextCurrent(window);
    glfwSwapInterval(0);
    glfwShowWindow(window);

    IntBuffer framebufferSize = BufferUtils.createIntBuffer(2);
    nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
    width = framebufferSize.get(0);
    height = framebufferSize.get(1);

    GL.createCapabilities();
    debugProc = GLUtil.setupDebugMessageCallback();

		/* Create all needed GL resources */
    createVao();
    distanceProgram = Utils.createRasterProgram("res/shaderinos/distance.vert","res/shaderinos/distance.frag");
    uniformArrayProgram = Utils.createRasterProgram("res/shaderinos/uniformarray.vert","res/shaderinos/uniformarray.frag");
    distortionProgram = Utils.createRasterProgram("res/shaderinos/distortion.vert","res/shaderinos/distortion.frag");
    inverseDistortionProgram = Utils.createRasterProgram("res/shaderinos/inversedistortion.vert","res/shaderinos/inversedistortion.frag");
    initPrograms();
    Utils.createFbo(distance,width,height);
    catImage = Utils.loadImage("images/cat4.png");
  }

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

  /**
   * Initialize the shader program.
   */
  private void initPrograms() {
    glUseProgram(this.uniformArrayProgram);
    vec3ArrayUniform = glGetUniformLocation(this.uniformArrayProgram, "cols");
    chosenUniform = glGetUniformLocation(this.uniformArrayProgram, "chosen");
    glUseProgram(0);


  }

  private void render() {

    //////////////////////////////////////////////
    //render to distance fbo
    glBindFramebuffer(GL_FRAMEBUFFER,0);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glUseProgram(distanceProgram);

    GL11.glBindTexture(GL_TEXTURE_2D,catImage.getTexture());

    glBindVertexArray(vao);
    glDrawArrays(GL_TRIANGLES, 0, 6);
    glBindTexture(GL_TEXTURE_2D,0);

    glBindVertexArray(0);
    glUseProgram(0);

    //////////////////////////////////////////////
    //render to distortion fbo
    glBindFramebuffer(GL_FRAMEBUFFER,distortion.fbo);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glUseProgram(distortionProgram);

    GL11.glBindTexture(GL_TEXTURE_2D,distance.normalTexture);

    glBindVertexArray(vao);
    glDrawArrays(GL_TRIANGLES, 0, 6);
    glBindTexture(GL_TEXTURE_2D,0);

    glBindVertexArray(0);
    glUseProgram(0);

    //////////////////////////////////////////////
    //render to screen
    glBindFramebuffer(GL_FRAMEBUFFER,distortion.fbo);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glUseProgram(this.inverseDistortionProgram);

//		/* Set uniform array. */
//    glUniform3fv(vec3ArrayUniform, colors);
//    /* Set chosen color (index into array) */
//    glUniform1i(chosenUniform, chosen);
    GL11.glBindTexture(GL_TEXTURE_2D, distortion.normalTexture);

    glBindVertexArray(vao);
    glDrawArrays(GL_TRIANGLES, 0, 6);

    glBindVertexArray(0);
    glUseProgram(0);

    glBindFramebuffer(GL_FRAMEBUFFER,0);



  }

  private void loop() {
    glClearColor(1,1,1,1);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    while (glfwWindowShouldClose(window) == GL_FALSE) {
      glfwPollEvents();
      glViewport(0, 0, width, height);

      render();

      glfwSwapBuffers(window);
    }
  }

  private void run() {
    try {
      init();
      loop();

      errCallback.release();
      keyCallback.release();
      fbCallback.release();
      if (debugProc != null)
        debugProc.release();
      glfwDestroyWindow(window);
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      glfwTerminate();
    }
  }

  public static void main(String[] args) {
    new UniformArrayDemo().run();
  }

}