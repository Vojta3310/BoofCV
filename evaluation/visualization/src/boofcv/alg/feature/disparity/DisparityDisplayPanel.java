/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.feature.disparity;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Controls GUI and settings for disparity calculation
 *
 * @author Peter Abeles
 */
public class DisparityDisplayPanel extends StandardAlgConfigPanel
		implements ChangeListener, ItemListener
{

	// selects which image to view
	JComboBox viewSelector;

	JSpinner minDisparitySpinner;
	JSpinner maxDisparitySpinner;
	JSpinner radiusSpinner;
	JSpinner errorSpinner;
	JSpinner reverseSpinner;
	JSpinner textureSpinner;

	// which image to show
	int selectedView;

	// minimum disparity to calculate
	int minDisparity = 0;
	// maximum disparity to calculate
	int maxDisparity = 100;
	// maximum allowed per pixel error
	int pixelError = 30;
	// reverse association tolerance
	int reverseTol = 6;
	// how large the region radius is
	int regionRadius = 3;
	// How diverse the texture needs to be
	double texture = 0.1;

	// listener for changes in states
	Listener listener;

	public DisparityDisplayPanel() {
		viewSelector = new JComboBox();
		viewSelector.addItem("Disparity");
		viewSelector.addItem("Left");
		viewSelector.addItem("Right");
		viewSelector.addItemListener(this);
		viewSelector.setMaximumSize(viewSelector.getPreferredSize());

		minDisparitySpinner = new JSpinner(new SpinnerNumberModel(minDisparity,0, 255, 5));
		minDisparitySpinner.addChangeListener(this);
		minDisparitySpinner.setMaximumSize(minDisparitySpinner.getPreferredSize());

		maxDisparitySpinner = new JSpinner(new SpinnerNumberModel(maxDisparity,1, 255, 5));
		maxDisparitySpinner.addChangeListener(this);
		maxDisparitySpinner.setMaximumSize(maxDisparitySpinner.getPreferredSize());

		radiusSpinner = new JSpinner(new SpinnerNumberModel(regionRadius,1, 30, 1));
		radiusSpinner.addChangeListener(this);
		radiusSpinner.setMaximumSize(radiusSpinner.getPreferredSize());

		errorSpinner = new JSpinner(new SpinnerNumberModel(pixelError,-1, 80, 5));
		errorSpinner.addChangeListener(this);
		errorSpinner.setMaximumSize(errorSpinner.getPreferredSize());

		reverseSpinner = new JSpinner(new SpinnerNumberModel(reverseTol,-1, 50, 1));
		reverseSpinner.addChangeListener(this);
		reverseSpinner.setMaximumSize(reverseSpinner.getPreferredSize());

		textureSpinner = new JSpinner(new SpinnerNumberModel(texture,0.0, 1, 0.02));
		textureSpinner.addChangeListener(this);
		textureSpinner.setPreferredSize(new Dimension(60,reverseSpinner.getPreferredSize().height));
		textureSpinner.setMaximumSize(textureSpinner.getPreferredSize());

		addLabeled(viewSelector, "View ", this);
		addSeparator(100);
		addLabeled(minDisparitySpinner, "Min Disparity", this);
		addLabeled(maxDisparitySpinner, "Max Disparity", this);
		addLabeled(radiusSpinner,    "Region Radius", this);
		addLabeled(errorSpinner,     "Max Error", this);
		addLabeled(textureSpinner,   "Texture", this);
		addLabeled(reverseSpinner,   "Reverse", this);
		addVerticalGlue(this);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == reverseSpinner) {
			reverseTol = ((Number) reverseSpinner.getValue()).intValue();
		} else if( e.getSource() == minDisparitySpinner) {
			minDisparity = ((Number) minDisparitySpinner.getValue()).intValue();
		} else if( e.getSource() == maxDisparitySpinner) {
			maxDisparity = ((Number) maxDisparitySpinner.getValue()).intValue();
		} else if( e.getSource() == errorSpinner) {
			pixelError = ((Number) errorSpinner.getValue()).intValue();
		} else if( e.getSource() == radiusSpinner) {
			regionRadius = ((Number) radiusSpinner.getValue()).intValue();
		} else if( e.getSource() == textureSpinner) {
			texture = ((Number) textureSpinner.getValue()).doubleValue();
		}

		if( minDisparity >= maxDisparity ) {
			minDisparity = maxDisparity-1;
			minDisparitySpinner.setValue(minDisparity);
		}

		listener.disparitySettingChange();
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == viewSelector ) {
			selectedView = viewSelector.getSelectedIndex();
			listener.disparityGuiChange();
		}
	}

	public void setActiveGui( boolean error , boolean reverse ) {
		setEnabled(6,error);
		setEnabled(7,reverse);
		setEnabled(8,error);
		setEnabled(9,reverse);
	}

	public void setListener(Listener listener ) {
		this.listener = listener;
	}

	public int getReverseTol() {
		return reverseTol;
	}

	public int getMaxDisparity() {

		return maxDisparity;
	}

	public int getPixelError() {
		return pixelError;
	}

	public int getSelectedView() {
		return selectedView;
	}

	public int getRegionRadius() {
		return regionRadius;
	}

	public double getTexture() {
		return texture;
	}

	public static interface Listener
	{
		public void disparitySettingChange();

		public void disparityGuiChange();
	}
}