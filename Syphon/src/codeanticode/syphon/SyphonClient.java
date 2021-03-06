/**
 * The Processing-Syphon library allows to create Syphon clients
 * and servers in a Processing sketch to share frames with other
 * applications. It only works on MacOSX and requires the P3D
 * renderer.
 *
 * (c) 2011-2012
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author    Andres Colubri http://interfaze.info/
 * @modified  02/21/2012
 * @version   0.4
 */

/**
    Processing Syphon library 
 
    Copyright 2011 Andres Colubri.
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
    ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
    WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS BE LIABLE FOR ANY
    DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
    (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
    LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
    ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package codeanticode.syphon;

import java.nio.IntBuffer;
import java.util.Dictionary;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import processing.core.*;
import processing.opengl.*;
import jsyphon.*;

/**
 * Syphon client class. It receives textures from a Syphon
 * server.
 *
 */
public class SyphonClient {
  protected PApplet parent;
  protected PGraphicsOpenGL pgl;
  protected JSyphonClient client;
  protected IntBuffer getBuffer;
  
  /**
   * Constructor that binds this client to the
   * specified named server.
   * 
   * @param parent
   * @param serverName
   */  
  public SyphonClient(PApplet parent, String serverName) {
    this.parent = parent;
    pgl = (PGraphicsOpenGL)parent.g;
    client = new JSyphonClient();
    
    Syphon.init();
    
    client.init();
    client.setApplicationName(serverName);    
    //PApplet.println(client.isValid());
  }

  /**
   * Returns a description of the server.
   * 
   * @return Dictionary
   */   
  public Dictionary<String, String> description() {
    return client.serverDescription();  
  }

  /**
   * Returns true if a new frame is available.
   * 
   * @return boolean 
   */   
  public boolean available() {
    return client.hasNewFrame();
  }

  /**
   * Copies the new frame to a PGraphics object.
   * It initializes dest if it is null or has the 
   * wrong size. It uses FBOs for fast copy.
   * 
   * @param dest
   */     
  public PGraphics getGraphics(PGraphics dest) {
    JSyphonImage img = client.newFrameImageForContext();
    
    int texId = img.textureName();
    int texWidth = img.textureWidth();
    int texHeight = img.textureHeight();
    
    if (dest == null || dest.width != texWidth || dest.height != texHeight) {            
      dest = parent.createGraphics(texWidth, texHeight, PConstants.P3D);
    }
    
    PGraphicsOpenGL destgl = (PGraphicsOpenGL)dest;
    destgl.beginDraw();
    GL gl = destgl.beginGL();
    GL2 gl2 = gl.getGL2();
      
    gl.glEnable(GL2.GL_TEXTURE_RECTANGLE_ARB);
    gl.glBindTexture(GL2.GL_TEXTURE_RECTANGLE_ARB, texId);
    gl2.glBegin(GL2.GL_QUADS);
    // Texture coordinates are inverted along Y.
    gl2.glTexCoord2f(0, texHeight);
    gl2.glVertex2f(0, 0);
    gl2.glTexCoord2f(texWidth, texHeight);
    gl2.glVertex2f(texWidth, 0);      
    gl2.glTexCoord2f(texWidth, 0);
    gl2.glVertex2f(texWidth, texHeight);      
    gl2.glTexCoord2f(0, 0);
    gl2.glVertex2f(0, texHeight);            
    gl2.glEnd();
    gl.glBindTexture(GL2.GL_TEXTURE_RECTANGLE_ARB, 0);
    gl.glDisable(GL2.GL_TEXTURE_RECTANGLE_ARB);
      
    destgl.endGL();
    destgl.endDraw();
        
    return dest;      
  }
  
  /**
   * Copies the new frame to a PImage object.
   * It initializes dest if it is null or has the 
   * wrong size. It is relatively slow because
   * it copies the content of the texture frame
   * to the pixels array of the image.
   * 
   * @param dest
   */    
  public PImage getImage(PImage dest) {
    JSyphonImage img = client.newFrameImageForContext();
    
    int texId = img.textureName();
    int texWidth = img.textureWidth();
    int texHeight = img.textureHeight();
    
    if (dest == null || dest.width != texWidth || dest.height != texHeight) {            
      dest = parent.createImage(texWidth, texHeight, PConstants.ARGB);
    }

    if (getBuffer == null || getBuffer.capacity() != texWidth * texHeight) {
      getBuffer = IntBuffer.allocate(texWidth * texHeight);
    }
    getBuffer.rewind();
    
    GL gl = pgl.gl;
    GL2 gl2 = gl.getGL2();    
    
    gl.glEnable(GL2.GL_TEXTURE_RECTANGLE_ARB);
    gl.glBindTexture(GL2.GL_TEXTURE_RECTANGLE_ARB, texId);    
    gl2.glGetTexImage(GL2.GL_TEXTURE_RECTANGLE_ARB, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, getBuffer);
    gl.glBindTexture(GL2.GL_TEXTURE_RECTANGLE_ARB, 0);
    gl.glDisable(GL2.GL_TEXTURE_RECTANGLE_ARB);
    
    dest.loadPixels();
    getBuffer.rewind();
    getBuffer.get(dest.pixels);
    
    // Converts pixels stored in OpenGL format to Processing's ARGB
    // plus flipping the image along Y.
    PGraphicsOpenGL.nativeToJavaARGB(dest);
    dest.updatePixels();
    
    return dest;
  }
  
  /**
   * Stops the client.
   * 
   */    
  public void stop() {
    client.stop();
  }
}

