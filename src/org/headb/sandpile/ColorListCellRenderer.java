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
package org.headb.sandpile;

import javax.swing.*;
import java.awt.Component;
import java.awt.Color;

/**
 * A ListCellRenderer for JLists that have colors as their items.
 * @author Bryan Head
 */
public class ColorListCellRenderer extends JLabel implements ListCellRenderer {

    private Float2dArrayList colors;
    private int start, step;

    public ColorListCellRenderer(int start, int step) {
        this.start = start;
        this.step = step;
        colors = new Float2dArrayList(3);
    }

    public void setColors(Float2dArrayList colors) {
        this.colors = colors;
    }

    public Component getListCellRendererComponent(JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        setOpaque(true);
        Color color = (Color) value;
        setText(start + step * index + ": (" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")");
        Color background = Color.BLACK;
        Color foreground;
        if (index < colors.rows()) {
            background = new Color(colors.get(index, 0), colors.get(index, 1), colors.get(index, 2));
        } else {
            background = Color.WHITE;
        }
        if (background.getRed() + background.getBlue() + background.getGreen() > (3 * 255 / 2)) {
            foreground = Color.BLACK;
        } else {
            foreground = Color.WHITE;
        }
        if (isSelected) {
            Color temp = foreground;
            foreground = background;
            background = temp;
        }
        setBackground(background);
        setForeground(foreground);
        return this;
    }
}
