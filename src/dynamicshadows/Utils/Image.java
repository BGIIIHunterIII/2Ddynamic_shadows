package dynamicshadows.Utils;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glGenTextures;

/**
 * Created by P on 29.12.2015.
 */
public class Image {
  int width;
  int height;
  int nComponents; //3 == rgb, 4==rbga
  final ByteBuffer image;
  final int texture;



  public Image(ByteBuffer buff, int w, int h, int comp){
    this.image = buff;
    this.width = w;
    this.height = h;

    this.nComponents = comp;

    texture = loadImageGL();

  }

  private int loadImageGL(){
    deleteTexture();

    int texID = glGenTextures();

    glBindTexture(GL_TEXTURE_2D, texID);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

    if ( nComponents == 3 ) //rgb
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, image);
    else {
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
      System.out.println("rgba texure loaded, make sure you enable gl_blend!");
    }
    glBindTexture(GL_TEXTURE_2D,0);
    return texID;
  }
  private void deleteTexture(){
    if( texture!= 0){
      glDeleteTextures(texture);
    }
  }
  public void cleanup(){
    deleteTexture();
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getnComponents() {
    return nComponents;
  }

  public int getTexture() {
    return texture;
  }
}
