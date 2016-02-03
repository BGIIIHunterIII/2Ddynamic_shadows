package main.java;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

/**
 * Created by P on 27.12.2015.
 */
public class MainSceneController {
  private long window;
  GLFWKeyCallback keyCallback;
  GLFWErrorCallback errorCallback;

  MainSceneController(long window) {
    this.window = window;
  }

  public MainSceneController(long window, GLFWKeyCallback keyCallback, GLFWErrorCallback errorCallback) {

  }

  public void run() {

    // Set the clear color
    glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

    // Run the rendering loop until the user has attempted to close
    // the window or has pressed the ESCAPE key.
    while (glfwWindowShouldClose(window) == GLFW_FALSE) {
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

      //update
      update();

      //draw
      glfwSwapBuffers(window); // swap the color buffers

      // Poll for window events. The key callback above will only be
      // invoked during this call.
      glfwPollEvents();
    }
  }

  private void update() {

  }
}
