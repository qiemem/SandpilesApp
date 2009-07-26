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
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.util.List;
import gnu.trove.TIntArrayList;

public class SandpileTransferable implements Transferable {
	private final List<float[]> locationData;
	private final TIntArrayList configData;
	private final List<int[]> edgeData;


	public SandpileTransferable(List<float[]> locationData, TIntArrayList configData, List<int[]> edgeData){
		this.locationData  = locationData;
		this.configData = configData;
		this.edgeData = edgeData;
	}

	public List<float[]> getLocationData(){
		return locationData;
	}

	public TIntArrayList getConfigData(){
		return configData;
	}

	public List<int[]> getEdgeData(){
		return edgeData;
	}

	public Object getTransferData(DataFlavor flavor){
		Object[] data = {locationData, edgeData, configData};
		return data;
	}
	public DataFlavor[] getTransferDataFlavors(){
		DataFlavor[] flavors = {DataFlavor.getTextPlainUnicodeFlavor()};
		return flavors;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor){
		return flavor.isFlavorTextType();
	}
}