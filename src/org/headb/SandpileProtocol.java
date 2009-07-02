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

/**
 *
 * @author headb
 */
public class SandpileProtocol {
	SandpileController sc;
	public SandpileProtocol(SandpileController sc){
		this.sc = sc;
	}
	public String processInput(String input){
		//System.err.println("input received: " + input);
		String output = "done";
		System.err.println(input);
		String[] command = input.split(" ");
		if(command[0].equals("update")){
			sc.update();
		}
		else if(command[0].equals("stabilize")){
			sc.stabilize();
		}
		else if(command[0].equals("repaint")){
			sc.repaint();
		}
		else if(command[0].equals("get_vertices")){
			if(sc.vertexData.isEmpty())
				output="";
			else{
				float[] v = sc.vertexData.get(0);
				output = String.format("%f %f", v[0], v[1]);
				for(int i=0; i<sc.vertexData.size(); i++){
					v = sc.vertexData.get(i);
					output += String.format(",%f %f", v[0], v[1]);
				}
			}
		}
		else if(command[0].equals("get_edges")){
			output = "";
			for(int v=0; v<sc.getConfig().size(); v++){
				boolean needsComma = false;
				for(int[] e : sc.getGraph().getOutgoingEdges(v)){
					if(needsComma)
						output+=",";
					else
						needsComma=true;
					output += String.format("%i %i %i", e[0], e[1], e[2]);
				}
			}
		}
		else if(command[0].equals("add_vertex")){
			sc.addVertex(Integer.valueOf(command[0]), Integer.valueOf(command[1]));
		}
		else if(command[0].equals("add_vertices")){
			for(int i=1; i<command.length-1; i+=1){
				sc.addVertex(Integer.valueOf(command[0]), Integer.valueOf(command[1]));
			}
		}
		else if(command[0].equals("add_sand")){
			sc.addSand(Integer.valueOf(command[1]),Integer.valueOf(command[2]));
		}
		else if(command[0].equals("set_sand")){
			sc.setSand(Integer.valueOf(command[1]), Integer.valueOf(command[2]));
		}
		else if(command[0].equals("get_num_unstables")){
			output = String.valueOf(sc.getGraph().getUnstables(sc.getConfig()).size());
		}
		else if(command[0].equals("addrandomsand")){
			sc.addSandToRandomControl(Integer.valueOf(command[1]),1);
		}
		else{
			System.err.println("Could not understand message: " + input);
		}
		return output;
	}
}
