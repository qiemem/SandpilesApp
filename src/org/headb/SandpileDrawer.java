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

import java.awt.Canvas;
import java.util.List;
import gnu.trove.TIntArrayList;

/**
 *
 * @author headb
 */
public interface SandpileDrawer{
	public enum ColorMode {
		NUM_OF_GRAINS,
		STABILITY,
		FIRINGS,
		DIFFERENCE
	}
	public Canvas getCanvas();
	public void setColorMode(ColorMode cm);
	public void setColors(Float2dArrayList colors, Float2dArrayList inDebtColors);
	public ColorMode getColorMode();
	public void paintSandpileGraph(SandpileGraph graph, Float2dArrayList vertexLocations, SandpileConfiguration config, TIntArrayList firings, TIntArrayList selectedVertices);
	public void setSelectionBox(float maxX, float maxY, float minX, float minY);
	public void clearSelectionBox();
	public float[] transformCanvasCoords(int x, int y);
}
