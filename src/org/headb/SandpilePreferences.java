/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

	public static SandpilePreferences getPreferences(){
		SandpilePreferences prefs = null;
		if(new File("preferences.ser").exists()){
			System.err.println("Preferences file found.");
			try {
				ObjectInputStream objstream = new ObjectInputStream(new FileInputStream(FILENAME));
				prefs = (SandpilePreferences)objstream.readObject();
				objstream.close();
				System.err.println("Preferences loaded successfully.");
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
		//return prefs;
		return new SandpilePreferences();
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
}