/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2012  FeatureIDE team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.fm.ui.editors.featuremodel.operations;

import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.ui.editors.featuremodel.layouts.FeatureDiagramLayoutHelper;

public class LayoutSelectionOperation extends AbstractFeatureModelOperation {

	private int newSelectedLayoutAlgorithm;
	private int oldSelectedLayoutAlgorithm;
	
	public LayoutSelectionOperation(FeatureModel featureModel, 
			int newSelectedLayoutAlgorithm, int oldSelectedLayoutAlgorithm) {
		super(featureModel, "Set "+FeatureDiagramLayoutHelper.getLayoutLabel(newSelectedLayoutAlgorithm));
		this.newSelectedLayoutAlgorithm = newSelectedLayoutAlgorithm;
		this.oldSelectedLayoutAlgorithm = oldSelectedLayoutAlgorithm;
	}

	@Override
	protected void redo() {
		featureModel.getLayout().setLayout(newSelectedLayoutAlgorithm);
	}

	@Override
	protected void undo() {
		featureModel.getLayout().setLayout(oldSelectedLayoutAlgorithm);
	}
	
}