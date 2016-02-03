package main.java.Utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import static org.lwjgl.opengl.ARBTextureFloat.GL_RGBA32F_ARB;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glBindFramebufferEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glCheckFramebufferStatusEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glDeleteFramebuffersEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glFramebufferTexture2DEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glGenFramebuffersEXT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_info_from_memory;
import static org.lwjgl.stb.STBImage.stbi_is_hdr_from_memory;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.system.MemoryUtil.memEncodeUTF8;


/**
 * Created by P on 28.12.2015.
 */
public class Utils {

  /**
   * Create a shader object from the given classpath resource.
   *
   * @param resource the class path
   * @param type     the shader type
   * @return the shader object id
   */
  private static int createShader(String resource, int type) throws IOException {
    return createShader(resource, type, null);
  }

  /**
   * Create a shader object from the given classpath resource.
   *
   * @param resource the class path
   * @param type     the shader type
   * @param version  the GLSL version to prepend to the shader source, or null
   * @return the shader object id
   */
  private static int createShader(String resource, int type, String version) throws IOException {
    int shader = glCreateShader(type);

    ByteBuffer source = ioResourceToByteBuffer(resource, 8192);

    if (version == null) {
      PointerBuffer strings = BufferUtils.createPointerBuffer(1);
      IntBuffer lengths = BufferUtils.createIntBuffer(1);

      strings.put(0, source);
      lengths.put(0, source.remaining());

      glShaderSource(shader, strings, lengths);
    } else {
      PointerBuffer strings = BufferUtils.createPointerBuffer(2);
      IntBuffer lengths = BufferUtils.createIntBuffer(2);

      ByteBuffer preamble = memEncodeUTF8("#version " + version + "\n", false);

      strings.put(0, preamble);
      lengths.put(0, preamble.remaining());

      strings.put(1, source);
      lengths.put(1, source.remaining());

      glShaderSource(shader, strings, lengths);
    }

    glCompileShader(shader);
    int compiled = glGetShaderi(shader, GL_COMPILE_STATUS);
    String shaderLog = glGetShaderInfoLog(shader);
    if (shaderLog.trim().length() > 0) {
      System.err.println(shaderLog);
    }
    if (compiled == 0) {
      throw new AssertionError("Could not compile shader");
    }
    return shader;
  }

  /**
   * Reads the specified resource and returns the raw data as a ByteBuffer.
   *
   * @param resource   the resource to read
   * @param bufferSize the initial buffer size
   * @return the resource data
   * @throws IOException if an IO error occurs
   */
  public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
    ByteBuffer buffer;

    File file = new File(resource);
    if (file.isFile()) {
      FileInputStream fis = new FileInputStream(file);
      FileChannel fc = fis.getChannel();
      buffer = BufferUtils.createByteBuffer((int) fc.size() + 1);

      while (fc.read(buffer) != -1) ;

      fc.close();
      fis.close();
    } else {
      buffer = BufferUtils.createByteBuffer(bufferSize);

      InputStream source = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
      if (source == null)
        throw new FileNotFoundException(resource);

      try {
        ReadableByteChannel rbc = Channels.newChannel(source);
        try {
          while (true) {
            int bytes = rbc.read(buffer);
            if (bytes == -1)
              break;
            if (buffer.remaining() == 0)
              buffer = resizeBuffer(buffer, buffer.capacity() * 2);
          }
        } finally {
          rbc.close();
        }
      } finally {
        source.close();
      }
    }

    buffer.flip();
    return buffer;
  }

  private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
    ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
    buffer.flip();
    newBuffer.put(buffer);
    return newBuffer;
  }

  /**
   * Create vertex/fragment shader pair
   */
  public static int createRasterProgram(String vertexShaderPath, String fragmentShaderPath) throws IOException {
    int program = glCreateProgram();
    int vshader = createShader(vertexShaderPath, GL_VERTEX_SHADER);
    int fshader = createShader(fragmentShaderPath, GL_FRAGMENT_SHADER);
    glAttachShader(program, vshader);
    glAttachShader(program, fshader);
    //glBindAttribLocation(program, 0, "position");
    //glBindFragDataLocation(program, 0, "color");
    glLinkProgram(program);
    int linked = glGetProgrami(program, GL_LINK_STATUS);
    String programLog = glGetProgramInfoLog(program);
    if (programLog.trim().length() > 0) {
      System.err.println(programLog);
    }
    if (linked == 0) {
      throw new AssertionError("Could not link program");
    }
    return program;
  }



  public static void createFbo(FBO inout, int width, int height) {
    createTex(inout, width, height);

    if (inout.fbo != 0) {
      glDeleteFramebuffersEXT(inout.fbo);
    }

    inout.fbo = glGenFramebuffersEXT();
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, inout.fbo);
    glBindTexture(GL_TEXTURE_2D, inout.depthTexture);
    glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT, GL_TEXTURE_2D, inout.depthTexture, 0);
    glBindTexture(GL_TEXTURE_2D, inout.normalTexture);
    glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_TEXTURE_2D, inout.normalTexture, 0);
    int status = glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT);
    if (status != GL_FRAMEBUFFER_COMPLETE_EXT) {
      throw new AssertionError("Incomplete framebuffer: " + status);
    }
    glBindTexture(GL_TEXTURE_2D, 0);
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
  }

  private static void createTex(FBO inout, int width, int height) {


    if (inout.normalTexture != 0) {
      glDeleteTextures(inout.normalTexture);
      glDeleteTextures(inout.depthTexture);
    }
    inout.normalTexture = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, inout.normalTexture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F_ARB, width, height, 0, GL_RGBA, GL_FLOAT, 0L);
    glBindTexture(GL_TEXTURE_2D, 0);
    inout.depthTexture = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, inout.depthTexture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, width, height, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_BYTE, 0L);
    glBindTexture(GL_TEXTURE_2D, 0);
  }

  /**
   * size is restricted to 1024 pix
   * @param imagePath
   * @return
   */
  public static Image loadImage(String imagePath){
    System.out.println("begin loading image....");

    ByteBuffer image;

    ByteBuffer imageBuffer;
    try {
      imageBuffer = ioResourceToByteBuffer(imagePath, 8 * 1024);
    } catch (IOException e) {
      throw new RuntimeException("failed to create ioResource when loading image",e);
    }

    IntBuffer w = BufferUtils.createIntBuffer(1);
    IntBuffer h = BufferUtils.createIntBuffer(1);
    IntBuffer comp = BufferUtils.createIntBuffer(1);

    // Use info to read image metadata without decoding the entire image.
    // We don't need this for this demo, just testing the API.
    if ( stbi_info_from_memory(imageBuffer, w, h, comp) == 0 )
      throw new RuntimeException("Failed to read image information: " + stbi_failure_reason());

    System.out.println("Image width: " + w.get(0));
    System.out.println("Image height: " + h.get(0));
    System.out.println("Image components: " + comp.get(0));
    System.out.println("Image HDR: " + (stbi_is_hdr_from_memory(imageBuffer) == 1));

    // Decode the image
    image = stbi_load_from_memory(imageBuffer, w, h, comp, 0);
    if ( image == null )
      throw new RuntimeException("Failed to load image: " + stbi_failure_reason());

    return new Image(image,w.get(0),h.get(0), comp.get(0));
  }

}
