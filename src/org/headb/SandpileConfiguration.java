package org.headb;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author headb
 */

import java.util.ArrayList;

public class SandpileConfiguration extends ArrayList<Integer>{

	public SandpileConfiguration() {
		super();
	}

	public SandpileConfiguration(SandpileConfiguration other) {
		super(other);
	}

	public SandpileConfiguration(int size) {
		super(size);
	}

	public SandpileConfiguration plus(SandpileConfiguration other) {
		SandpileConfiguration result = new SandpileConfiguration();
		assert this.size() == other.size() : "Tried to add configurations of different size";
		for(int i=0;i<this.size();i++)
			result.add(this.get(i)+other.get(i));
		return result;
	}

	public SandpileConfiguration times(int scalar) {
		SandpileConfiguration result = new SandpileConfiguration();
		for(int i=0;i<this.size();i++)
			result.add(this.get(i)*scalar);
		return result;
	}
}
