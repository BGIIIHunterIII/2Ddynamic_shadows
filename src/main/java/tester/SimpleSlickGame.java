package tester;

import org.lwjgl.opengl.*;
import org.lwjgl.util.glu.GLU;
import org.newdawn.slick.*;
import org.newdawn.slick.opengl.pbuffer.FBOGraphics;


import java.util.ArrayList;

import static org.lwjgl.opengl.EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_RENDERBUFFER_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glBindFramebufferEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glBindRenderbufferEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glFramebufferRenderbufferEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glFramebufferTexture2DEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glGenFramebuffersEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glGenRenderbuffersEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glRenderbufferStorageEXT;
import static org.lwjgl.opengl.GL11.*;

public class SimpleSlickGame extends BasicGame {
  int colorTextureID;
  int framebufferID;
  int depthRenderBufferID;
  int angle = 0;
  ShaderTester shaderTester;


  Image gandalf;
  Image empty;
  Image shadowCasters;
  FBOGraphics shadowCastersFBO;

  Image distanceCalcTexture;
  FBOGraphics distanceCalcFBO;

  Image distortionCalcTexture;
  FBOGraphics distortionCalcFBO;

  ArrayList<FBOGraphics> reductionCalcFBO = new ArrayList<FBOGraphics>();
  ArrayList<Image> reductionCalcTexture = new ArrayList<Image>();

  Image shadowsTexture;
  FBOGraphics shadowsFBO;

  int targetTextureDimensionLocation_ReductionProgram;
  int textureDimensionsLocation_DistanceProgram;
  int renderTargetSizeLoaction_DrawProgram;
  int shadowMapSamplerLoaction_DrawProgram;
  int shadowTextureSamplerLoaction_DrawProgram;
  int targetTextureLocation_ReductionProgram;
  int inputSamplerLoaction_ReductionProgram;


  int textureLocation;
  int resolutionLocation;
  final static int w = 1024;
  final static int h = 1024;

  public SimpleSlickGame(String gamename) {
    super(gamename);
  }

  @Override
  public void init(GameContainer gc) throws SlickException {
    gandalf = new Image("res/sprites/entities/testEntity1.png");
    empty = new Image(w,h);
    shaderTester = new ShaderTester();
    GL11.glEnable(GL11.GL_TEXTURE);

    textureLocation = GL20.glGetUniformLocation(shaderTester.testProgram,"texture");
    resolutionLocation = GL20.glGetUniformLocation(shaderTester.testProgram,"resolution");
    targetTextureDimensionLocation_ReductionProgram = GL20.glGetUniformLocation(shaderTester.reductionProgram,"targetTextureDimensions");
    textureDimensionsLocation_DistanceProgram = GL20.glGetUniformLocation(shaderTester.distanceProgram,"textureDimension");
    renderTargetSizeLoaction_DrawProgram = GL20.glGetUniformLocation(shaderTester.drawProgram,"renderTargetSize");
    shadowMapSamplerLoaction_DrawProgram = GL20.glGetUniformLocation(shaderTester.drawProgram,"shadowMapSampler");
    shadowTextureSamplerLoaction_DrawProgram = GL20.glGetUniformLocation(shaderTester.drawProgram,"shadowTextureSampler");
    targetTextureLocation_ReductionProgram = GL20.glGetUniformLocation(shaderTester.reductionProgram,"targetTexture");
    inputSamplerLoaction_ReductionProgram = GL20.glGetUniformLocation(shaderTester.reductionProgram,"inputSampler");

    shadowCasters = new Image(w,h);
    shadowCastersFBO = new FBOGraphics(shadowCasters);

    distanceCalcTexture = new Image(w,h);
    distanceCalcFBO = new FBOGraphics(distanceCalcTexture);

    distortionCalcTexture = new Image(w,h);
    distortionCalcFBO = new FBOGraphics(distortionCalcTexture);

    shadowsTexture = new Image(w,h);
    shadowsFBO = new FBOGraphics(shadowsTexture);

    int nReductions = (int)(Math.log(distortionCalcTexture.getWidth())/Math.log(2)+1e-12);
    for (int i = 1; i < nReductions ; i++) {
      reductionCalcTexture.add(new Image((int) Math.pow(2, i), h));
      reductionCalcFBO.add(new FBOGraphics(reductionCalcTexture.get(i - 1)));
    }



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
    Graphics.setCurrent(distanceCalcFBO);
    shaderTester.useProgram(shaderTester.distanceProgram);
    GL20.glUniform2f(textureDimensionsLocation_DistanceProgram, distanceCalcTexture.getWidth(), distanceCalcTexture.getHeight());

    distanceCalcFBO.drawImage(shadowCasters, 0, 0);
    shaderTester.stopUsingProgram();

    //************ distortion step
    Graphics.setCurrent(distortionCalcFBO);
    shaderTester.useProgram(shaderTester.distortionProgram);
    distortionCalcFBO.drawImage(distanceCalcTexture, 0, 0);
    shaderTester.stopUsingProgram();

    //*************** reductionCalcFBO
    shaderTester.useProgram(shaderTester.reductionProgram);
    Graphics.setCurrent(reductionCalcFBO.get(reductionCalcFBO.size() - 1));
    GL20.glUniform2i(targetTextureDimensionLocation_ReductionProgram, reductionCalcTexture.get(reductionCalcTexture.size()-1).getWidth(), h);
    GL13.glActiveTexture(GL13.GL_TEXTURE0);
    reductionCalcTexture.get(reductionCalcTexture.size()-1).bind();
    GL20.glUniform1i(targetTextureLocation_ReductionProgram, 0);
    GL13.glActiveTexture(GL13.GL_TEXTURE1);
    distortionCalcTexture.bind();
    GL20.glUniform1i(inputSamplerLoaction_ReductionProgram, 1);

    reductionCalcFBO.get(reductionCalcTexture.size() - 1).drawImage(distortionCalcTexture, 0, 0);

    for (int i = 1; i < reductionCalcFBO.size(); i++) {
      int n = reductionCalcFBO.size()-1-i;
      Graphics.setCurrent(reductionCalcFBO.get(n));
      Image currentTexture = reductionCalcTexture.get(n+1);
      GL20.glUniform2i(targetTextureDimensionLocation_ReductionProgram,reductionCalcTexture.get(n).getWidth(),h);
      GL13.glActiveTexture(GL13.GL_TEXTURE0);
      reductionCalcTexture.get(n).bind(); //target
      GL20.glUniform1i(targetTextureLocation_ReductionProgram, 0);
      GL13.glActiveTexture(GL13.GL_TEXTURE1);
      currentTexture.bind(); //  inputSampler
      GL20.glUniform1i(inputSamplerLoaction_ReductionProgram, 1);
      reductionCalcFBO.get(n).drawImage(currentTexture,0,0);
    }
    shaderTester.stopUsingProgram();

    //************* draw shadows
    Graphics.setCurrent(shadowsFBO);
    shaderTester.useProgram(shaderTester.drawProgram);
    GL20.glUniform2f(renderTargetSizeLoaction_DrawProgram, shadowsTexture.getWidth(), shadowsTexture.getHeight());

    GL13.glActiveTexture(GL13.GL_TEXTURE0);
    shadowsTexture.bind();
    GL20.glUniform1i(shadowTextureSamplerLoaction_DrawProgram, 0);

    GL13.glActiveTexture(GL13.GL_TEXTURE1);
    reductionCalcTexture.get(0).bind();
    GL20.glUniform1i(shadowMapSamplerLoaction_DrawProgram, 1);

    shadowsFBO.drawImage(shadowsTexture, 0, 0);
    shadowsFBO.flush();

    shaderTester.stopUsingProgram();



    //******** start drawing to screen
    Graphics.setCurrent(g);
    g.clear();

    int offset=0;
    for(Image i:reductionCalcTexture){
      g.drawImage(i,offset,0);
      offset+=i.getWidth()+1;
    }
    g.drawImage(shadowsTexture,0,0);
    //g.drawImage(shadowCasters,0,0);
    //blend shadows

    g.flush();

  }

  public static void main(String[] args) {
    try {
      AppGameContainer appgc;
      appgc = new AppGameContainer(new SimpleSlickGame("Simple Slick Game"));
      appgc.setDisplayMode(w, h, false);
      appgc.start();
    } catch (SlickException ex) {

    }
  }


  public void initGL() {

    glViewport(0, 0, 512, 512);                                // Reset The Current Viewport
    glMatrixMode(GL_PROJECTION);                               // Select The Projection Matrix
    glLoadIdentity();                                          // Reset The Projection Matrix
    GLU.gluPerspective(45.0f, 512f / 512f, 1.0f, 100.0f);        // Calculate The Aspect Ratio Of The Window
    glMatrixMode(GL_MODELVIEW);                                // Select The Modelview Matrix
    glLoadIdentity();                                          // Reset The Modelview Matrix

    glClearColor(0.0f, 0.0f, 0.0f, 0.5f);                      // Black Background
    glClearDepth(1.0f);                                        // Depth Buffer Setup
    glDepthFunc(GL_LEQUAL);                                    // The Type Of Depth Testing (Less Or Equal)
    glEnable(GL_DEPTH_TEST);                                   // Enable Depth Testing
    glShadeModel(GL_SMOOTH);                                   // Select Smooth Shading
    glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);         // Set Perspective Calculations To Most Accurate


    // check if GL_EXT_framebuffer_object can be use on this system
    if (!GLContext.getCapabilities().GL_EXT_framebuffer_object) {
      System.out.println("FBO not supported!!!");
      System.exit(0);
    } else {

      System.out.println("FBO is supported!!!");

      // init our fbo

      framebufferID = glGenFramebuffersEXT();                                         // create a new framebuffer
      colorTextureID = glGenTextures();                                               // and a new texture used as a color buffer
      depthRenderBufferID = glGenRenderbuffersEXT();                                  // And finally a new depthbuffer

      glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, framebufferID);                        // switch to the new framebuffer

      // initialize color texture
      glBindTexture(GL_TEXTURE_2D, colorTextureID);                                   // Bind the colorbuffer texture
      glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);               // make it linear filterd
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 512, 512, 0, GL_RGBA, GL_INT, (java.nio.ByteBuffer) null);  // Create the texture data
      glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_TEXTURE_2D, colorTextureID, 0); // attach it to the framebuffer


      // initialize depth renderbuffer
      glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, depthRenderBufferID);                // bind the depth renderbuffer
      glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL14.GL_DEPTH_COMPONENT24, 512, 512); // get the data space for it
      glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT, GL_RENDERBUFFER_EXT, depthRenderBufferID); // bind it to the renderbuffer

      glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);                                    // Swithch back to normal framebuffer rendering

    }

  }

  public void renderGL(Graphics g) {

    // FBO render pass

    glViewport(0, 0, 512, 512);                                    // set The Current Viewport to the fbo size

    glBindTexture(GL_TEXTURE_2D, 0);                                // unlink textures because if we dont it all is gonna fail
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, framebufferID);        // switch to rendering on our FBO

    glClearColor(1.0f, 1.0f, 1.0f, 1f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);            // Clear Screen And Depth Buffer on the fbo to red
    glLoadIdentity();                                              // Reset The Modelview Matrix
    glTranslatef(0.0f, 0.0f, -6.0f);                               // Translate 6 Units Into The Screen and then rotate


    glColor3f(0,0,1);                                               // set color to yellow
    drawBox();
    g.drawImage(gandalf,0,0);


    // Normal render pass, draw cube with texture

    glEnable(GL_TEXTURE_2D);                                        // enable texturing
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);                    // switch to rendering on the framebuffer

    glClearColor(0.0f, 1.0f, 0.0f, 0.5f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);            // Clear Screen And Depth Buffer on the framebuffer to black

    glBindTexture(GL_TEXTURE_2D, colorTextureID);                   // bind our FBO texture


    glViewport(0, 0, 512, 512);                                    // set The Current Viewport


    glLoadIdentity();                                              // Reset The Modelview Matrix
    glTranslatef(0.0f, 0.0f, -6.0f);                               // Translate 6 Units Into The Screen and then rotate

    glColor3f(1, 1, 1);                                               // set the color to white
    drawBox();                                                      // draw the box

    glDisable(GL_TEXTURE_2D);
    glFlush();

  }

  public void drawBox() {
    // this func just draws a perfectly normal box with some texture coordinates
    glBegin(GL_QUADS);
    // Front Face
    glTexCoord2f(0.0f, 0.0f);
    glVertex3f(-1.0f, -1.0f, 1.0f);  // Bottom Left Of The Texture and Quad
    glTexCoord2f(1.0f, 0.0f);
    glVertex3f(1.0f, -1.0f, 1.0f);  // Bottom Right Of The Texture and Quad
    glTexCoord2f(1.0f, 1.0f);
    glVertex3f(1.0f, 1.0f, 1.0f);  // Top Right Of The Texture and Quad
    glTexCoord2f(0.0f, 1.0f);
    glVertex3f(-1.0f, 1.0f, 1.0f);  // Top Left Of The Texture and Quad
    // Back Face
    glTexCoord2f(1.0f, 0.0f);
    glVertex3f(-1.0f, -1.0f, -1.0f);  // Bottom Right Of The Texture and Quad
    glTexCoord2f(1.0f, 1.0f);
    glVertex3f(-1.0f, 1.0f, -1.0f);  // Top Right Of The Texture and Quad
    glTexCoord2f(0.0f, 1.0f);
    glVertex3f(1.0f, 1.0f, -1.0f);  // Top Left Of The Texture and Quad
    glTexCoord2f(0.0f, 0.0f);
    glVertex3f(1.0f, -1.0f, -1.0f);  // Bottom Left Of The Texture and Quad
    // Top Face
    glTexCoord2f(0.0f, 1.0f);
    glVertex3f(-1.0f, 1.0f, -1.0f);  // Top Left Of The Texture and Quad
    glTexCoord2f(0.0f, 0.0f);
    glVertex3f(-1.0f, 1.0f, 1.0f);  // Bottom Left Of The Texture and Quad
    glTexCoord2f(1.0f, 0.0f);
    glVertex3f(1.0f, 1.0f, 1.0f);  // Bottom Right Of The Texture and Quad
    glTexCoord2f(1.0f, 1.0f);
    glVertex3f(1.0f, 1.0f, -1.0f);  // Top Right Of The Texture and Quad
    // Bottom Face
    glTexCoord2f(1.0f, 1.0f);
    glVertex3f(-1.0f, -1.0f, -1.0f);  // Top Right Of The Texture and Quad
    glTexCoord2f(0.0f, 1.0f);
    glVertex3f(1.0f, -1.0f, -1.0f);  // Top Left Of The Texture and Quad
    glTexCoord2f(0.0f, 0.0f);
    glVertex3f(1.0f, -1.0f, 1.0f);  // Bottom Left Of The Texture and Quad
    glTexCoord2f(1.0f, 0.0f);
    glVertex3f(-1.0f, -1.0f, 1.0f);  // Bottom Right Of The Texture and Quad
    // Right face
    glTexCoord2f(1.0f, 0.0f);
    glVertex3f(1.0f, -1.0f, -1.0f);  // Bottom Right Of The Texture and Quad
    glTexCoord2f(1.0f, 1.0f);
    glVertex3f(1.0f, 1.0f, -1.0f);  // Top Right Of The Texture and Quad
    glTexCoord2f(0.0f, 1.0f);
    glVertex3f(1.0f, 1.0f, 1.0f);  // Top Left Of The Texture and Quad
    glTexCoord2f(0.0f, 0.0f);
    glVertex3f(1.0f, -1.0f, 1.0f);  // Bottom Left Of The Texture and Quad
    // Left Face
    glTexCoord2f(0.0f, 0.0f);
    glVertex3f(-1.0f, -1.0f, -1.0f);  // Bottom Left Of The Texture and Quad
    glTexCoord2f(1.0f, 0.0f);
    glVertex3f(-1.0f, -1.0f, 1.0f);  // Bottom Right Of The Texture and Quad
    glTexCoord2f(1.0f, 1.0f);
    glVertex3f(-1.0f, 1.0f, 1.0f);  // Top Right Of The Texture and Quad
    glTexCoord2f(0.0f, 1.0f);
    glVertex3f(-1.0f, 1.0f, -1.0f);  // Top Left Of The Texture and Quad
    glEnd();
  }
}