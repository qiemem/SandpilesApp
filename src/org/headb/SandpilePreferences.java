/*
Copyright (c) 2008-2009 Bryan Head
All Rights Reserved

[This software is released under the "MIT License"]

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the
Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall
be included in all copies or substantial portions of the
Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package org.headb;
import java.io.*;

/**
 *
 * @author headb
 */
public class SandpilePreferences implements Serializable{
	static private final long serialVersionUID = 181081L;
	static public String FILENAME = "preferences.ser";

	private Float2dArrayList colors;
	private Float2dArrayList inDebtColors;
	private float[] backgroundColor;

	public static SandpilePreferences getPreferences(){
		SandpilePreferences prefs = null;
		if(new File("preferences.ser").exists()){
			System.err.println("Preferences file found.");
			try {
				ObjectInputStream objstream = new ObjectInputStream(new FileInputStream(FILENAME));
				prefs = (SandpilePreferences)objstream.readObject();
				objstream.close();
				System.err.println("Preferences loaded successfully.");
				// For some reason, deserialization seems to lose the cols field
				// of Float2dArrayList. Hence, we set it properly here.
				prefs.colors = new Float2dArrayList(prefs.colors.toNativeArray(), 3);
			}catch(IOException e){
				System.err.println("IOException while trying to read preferences file.");
				System.err.println(e.getMessage());
				e.printStackTrace();
				prefs = new SandpilePreferences();
			}catch(ClassNotFoundException e){
				System.err.println("ClassNotFoundException while trying to read preferences file.");
				System.err.println(e.getMessage());
				e.printStackTrace();
				prefs = new SandpilePreferences();
			}
		}else{
			System.err.println("No preferences file found; using defaults.");
			prefs = new SandpilePreferences();
		}
		return prefs;
		//return new SandpilePreferences();
	}

	public SandpilePreferences() {
		float[] colorArray = {0.3f, 0.3f, 0.3f,
			0f, 0f, 1f,
			0f, 1f, 1f,
			0f, 1f, 0f,
			1f, 0f, 0f,
			1f, .5f, 0f,
			1f, 1f, 0f,
			1f, 1f, 1f};
		colors = new Float2dArrayList(colorArray, 3);
		float[] inDebtColorArray = {0.2f, 0f, 0f};
		inDebtColors = new Float2dArrayList(inDebtColorArray, 3);
		backgroundColor = new float[3];
	}

	public boolean save() {
		try{
			ObjectOutputStream objstream = new ObjectOutputStream(new FileOutputStream(FILENAME));
			objstream.writeObject(this);
			objstream.close();
			return true;
		}catch(IOException e){
			return false;
		}
	}

	public Float2dArrayList getColors() {
		return colors;
	}

	public Float2dArrayList getInDebtColors() {
		return inDebtColors;
	}

	public  float[] getBackgroundColor() {
		return backgroundColor;
	}
	public void setBackgroundColor(float[] newBGColor) {
		backgroundColor = newBGColor;
	}
}