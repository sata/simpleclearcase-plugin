/**
 * The MIT License
 * 
 * Copyright (c) 2011, Sun Microsystems, Inc., Sam Tavakoli
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.simpleclearcase;

/**
 * @author Sam Tavakoli
 *
 * This class represents a file element inside the repository. It's a simple container 
 */
public class FileElement {
	public static final String INIT_VERSION = "0"; 
	
	private String filePath;
	private String version;
	
	public FileElement() {
		
	}
	
	public FileElement(String path, String version) {
		setFilePath(path);
		this.version  = version;
	}

	public FileElement(String path) {
		this(path, INIT_VERSION);
	}
	
	public String getFilePath() {
		return filePath;
	}
	
	public void setFilePath(String path) {
		this.filePath = path;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
}
