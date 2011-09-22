/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.filter.derivative;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.filter.derivative.ImageHessian;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.image.BufferedImage;

import static boofcv.factory.filter.derivative.FactoryDerivative.*;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
public class ShowImageDerivative<T extends ImageBase, D extends ImageBase>
	extends SelectAlgorithmImagePanel implements ProcessInput
{
	Class<T> imageType;
	Class<D> derivType;

	ListDisplayPanel panel = new ListDisplayPanel();

	T image;
	BufferedImage original;
	boolean processedImage = false;

	public ShowImageDerivative(Class<T> imageType, Class<D> derivType) {
		super(1);
		this.imageType = imageType;
		this.derivType = derivType;

		Helper h;

		h = new Helper(prewitt(imageType,derivType),hessianPrewitt(derivType));
		addAlgorithm(0, "Prewitt", h);
		h = new Helper(sobel(imageType,derivType),hessianSobel(derivType));
		addAlgorithm(0, "Sobel",h);
		h = new Helper(three(imageType,derivType),hessianThree(derivType));
		addAlgorithm(0, "Three",h);
		h = new Helper(gaussian(-1,3,imageType,derivType),hessianGaussian(-1,3,derivType));
		addAlgorithm(0, "Gaussian", h);

		setMainGUI(panel);
	}

	public void process( final BufferedImage original ) {
		setInputImage(original);

		this.original = original;
		image = ConvertBufferedImage.convertFrom(original,null,imageType);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// adjust the preferred size for the list panel
				int width = panel.getListWidth();

//				setPreferredSize(new Dimension(original.getWidth()+width+10,original.getHeight()+30));
				doRefreshAll();
			}});
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, final Object cookie) {
		if( image == null )
			return;

		D derivX = GeneralizedImageOps.createImage(derivType,image.width,image.height);
		D derivY = GeneralizedImageOps.createImage(derivType,image.width,image.height);

		panel.reset();

		Helper h = (Helper)cookie;

		D derivXX = GeneralizedImageOps.createImage(derivType,image.width,image.height);
		D derivYY = GeneralizedImageOps.createImage(derivType,image.width,image.height);
		D derivXY = GeneralizedImageOps.createImage(derivType,image.width,image.height);

		h.gradient.process(image,derivX,derivY);
		h.hessian.process(derivX,derivY,derivXX,derivYY,derivXY);

		double max;

		max = GPixelMath.maxAbs(derivX);
		panel.addImage(VisualizeImageData.colorizeSign(derivX,null,max),"X-derivative");
		max = GPixelMath.maxAbs(derivY);
		panel.addImage(VisualizeImageData.colorizeSign(derivY,null,max),"Y-derivative");
		max = GPixelMath.maxAbs(derivXX);
		panel.addImage(VisualizeImageData.colorizeSign(derivXX,null,max),"XX-derivative");
		max = GPixelMath.maxAbs(derivYY);
		panel.addImage(VisualizeImageData.colorizeSign(derivYY,null,max),"YY-derivative");
		max = GPixelMath.maxAbs(derivXY);
		panel.addImage(VisualizeImageData.colorizeSign(derivXY,null,max),"XY-derivative");

		processedImage = true;
		repaint();
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager manager = getInputManager();

		BufferedImage image = manager.loadImage(index);
		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	private class Helper
	{
		public ImageGradient<T,D> gradient;
		public ImageHessian<D> hessian;

		private Helper(ImageGradient<T, D> gradient, ImageHessian<D> hessian) {
			this.gradient = gradient;
			this.hessian = hessian;
		}
	}

	public static void main(String args[]) {

		ShowImageDerivative<ImageFloat32,ImageFloat32> app
				= new ShowImageDerivative<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);
//		ShowImageDerivative<ImageUInt8, ImageSInt16> app
//				= new ShowImageDerivative<ImageUInt8,ImageSInt16>(ImageUInt8.class,ImageSInt16.class);

		ImageListManager manager = new ImageListManager();
		manager.add("shapes","data/shapes01.png");
		manager.add("sunflowers","data/sunflowers.png");
		manager.add("beach","data/scale/beach02.jpg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Image Derivative");
	}
}