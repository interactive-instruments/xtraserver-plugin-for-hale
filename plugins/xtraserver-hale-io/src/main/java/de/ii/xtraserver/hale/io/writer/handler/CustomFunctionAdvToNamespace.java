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

package de.ii.xtraserver.hale.io.writer.handler;

import java.util.Optional;

import de.interactive_instruments.xtraserver.config.api.MappingValue;
import de.interactive_instruments.xtraserver.config.api.MappingValueBuilder;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.core.io.Value;

/**
 * Transforms the custom function 'custom:alignment:adv.inspire.namespace' to a
 * {@link MappingValue}
 * 
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
class CustomFunctionAdvToNamespace extends FormattedStringHandler {

	public final static String FUNCTION_ID = "custom:alignment:adv.inspire.namespace";

	CustomFunctionAdvToNamespace(final MappingContext mappingContext) {
		super(mappingContext);
	}

	/**
	 * @see AbstractPropertyTransformationHandler#doHandle(eu.esdihumboldt.hale.common.align.model.Cell,
	 *      eu.esdihumboldt.hale.common.align.model.Property)
	 */
	@Override
	public Optional<MappingValue> doHandle(final Cell propertyCell, final Property targetProperty) {
		final Value inspireNamespace = mappingContext
				.getTransformationProperty(MappingContext.PROPERTY_INSPIRE_NAMESPACE);
		if (!inspireNamespace.isEmpty()) {
			final MappingValue mappingValue = new MappingValueBuilder().constant()
					.qualifiedTargetPath(
							buildPath(targetProperty.getDefinition().getPropertyPath()))
					.value(inspireNamespace.as(String.class)).build();
			return Optional.of(mappingValue);
		}

		return Optional.empty();
	}

}
