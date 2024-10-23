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
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.core.io.Value;
import java.util.Optional;

/**
 * Transforms the custom function 'custom:alignment:adv.inspire.namespace' to a
 * {@link FeatureSchema}
 */
class CustomFunctionAdvToNamespace extends FormattedStringHandler {

	public final static String FUNCTION_ID = "custom:alignment:adv.inspire.namespace";

	CustomFunctionAdvToNamespace(final MappingContext mappingContext) {
		super(mappingContext);
	}

	/**
	 * @see AbstractPropertyTransformationHandler#doHandle(Cell,
	 *      Property)
	 */
	@Override
	public Optional<ImmutableFeatureSchema.Builder> doHandle(final Cell propertyCell, final Property targetProperty) {

		final Value inspireNamespace = mappingContext
				.getTransformationProperty(MappingContext.PROPERTY_INSPIRE_NAMESPACE);

		if (inspireNamespace.isEmpty()) {

			return Optional.empty();

		} else {

			String value = inspireNamespace.as(String.class);
			value = reformatVariable(value);

			ImmutableFeatureSchema.Builder propertyBuilder = buildPropertyPath(propertyCell, targetProperty
			);

			propertyBuilder.constantValue(value);

			SchemaBase.Type baseType = SchemaBase.Type.STRING;
			if (isMultiValuedPropertyPerSchemaDefinition(getLastPropertyDefinition(targetProperty))) {
				propertyBuilder.type(Type.VALUE_ARRAY);
				propertyBuilder.valueType(baseType);
			} else {
				propertyBuilder.type(baseType);
			}

			return Optional.of(propertyBuilder);
		}
	}
}
