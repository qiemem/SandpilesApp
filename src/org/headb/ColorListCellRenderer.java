/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.headb;
import javax.swing.*;
import java.awt.Component;
import java.awt.Color;
/**
 *
 * @author headb
 */
public class ColorListCellRenderer extends JLabel implements ListCellRenderer{
	private Float2dArrayList colors;
	private int start, step;

	public ColorListCellRenderer(int start, int step) {
		this.start = start;
		this.step = step;
		colors = new Float2dArrayList(3);
	}

	public void setColors(Float2dArrayList colors){
		this.colors = colors;
	}

	public Component getListCellRendererComponent(JList list,
			Object value,
			int index,
			boolean isSelected,
			boolean cellHasFocus){
		setOpaque(true);
		Color color = (Color)value;
		setText(start+step*index+": ("+color.getRed()+", "+color.getGreen()+", "+color.getBlue()+")");
		Color background = Color.BLACK;
		Color foreground;
		if(index<colors.size()){
			background = new Color(colors.get(index, 0), colors.get(index, 1), colors.get(index, 2));
		}else{
			background = Color.WHITE;
		}
		if(background.getRed()+background.getBlue()+background.getGreen() > (3*255/2)){
			foreground = Color.BLACK;
		}else{
			foreground = Color.WHITE;
		}
		if(isSelected){
			Color temp = foreground;
			foreground = background;
			background = temp;
		}
		setBackground(background);
		setForeground(foreground);
		return this;
	}

}
