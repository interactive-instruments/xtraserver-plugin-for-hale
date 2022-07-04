/*
 * Copyright (c) 2017 interactive instruments GmbH
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     interactive instruments GmbH <http://www.interactive-instruments.de>
 */

package de.ii.xtraserver.webapi.hale.io.writer.handler;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Property;
import java.util.Optional;

/**
 * Transforms the custom function 'custom:alignment:adv.uom.toucum' to a
 * {@link FeatureSchema}
 */
class CustomFunctionAdvToUCUM extends
    ClassificationMappingHandler {

	public final static String FUNCTION_ID = "custom:alignment:adv.uom.toucum";

	CustomFunctionAdvToUCUM(final MappingContext mappingContext) {
		super(mappingContext);
	}

	/**
	 * @see AbstractPropertyTransformationHandler#doHandle(Cell,
	 *      Property)
	 */
	@Override
	public Optional<ImmutableFeatureSchema.Builder> doHandle(final Cell propertyCell, final Property targetProperty) {

		return Optional.empty();
	}

}
