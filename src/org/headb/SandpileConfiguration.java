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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author headb
 */

import java.util.ArrayList;
import gnu.trove.TIntArrayList;

public class SandpileConfiguration extends TIntArrayList{

	public SandpileConfiguration() {
		super();
	}

	public SandpileConfiguration(SandpileConfiguration other) {
		super(other._data);
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
