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
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.webapi.hale.io.writer.XtraServerWebApiTypeUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import java.util.Optional;

/**
 * Transforms the custom function 'custom:alignment:adv.inspire.identifier' to a
 * {@link FeatureSchema}
 */
class CustomFunctionAdvToIdentifier extends FormattedStringHandler {

	public final static String FUNCTION_ID = "custom:alignment:adv.inspire.identifier";

	CustomFunctionAdvToIdentifier(final MappingContext mappingContext) {
		super(mappingContext);
	}

	/**
	 * @see AbstractPropertyTransformationHandler#doHandle(Cell,
	 *      Property)
	 */
	@Override
	public Optional<ImmutableFeatureSchema.Builder> doHandle(final Cell propertyCell, final Property targetProperty) {

		final Value inspireNamespaceValue = mappingContext
				.getTransformationProperty(MappingContext.PROPERTY_INSPIRE_NAMESPACE);

		String inspireNamespace = reformatVariable(inspireNamespaceValue.as(String.class));

		String value = "";
		if (!inspireNamespace.isEmpty()) {
			value += inspireNamespace
					+ (inspireNamespace.endsWith("/") ? "" : "/");
		}
		value += mappingContext.getFeatureTypeName() + "_{{value}}";

		ImmutableFeatureSchema.Builder propertyBuilder = buildPropertyPath(targetProperty);

		PropertyDefinition pd = getLastPropertyDefinition(targetProperty);
		TypeDefinition td = pd.getPropertyType();

		Property sourceProperty = XtraServerMappingUtils.getSourceProperty(propertyCell);
		String sourcePath = sourceProperty
				.getDefinition().getDefinition().getDisplayName();
		propertyBuilder.sourcePath(sourcePath);

		ImmutablePropertyTransformation.Builder trfBuilder = new ImmutablePropertyTransformation.Builder();
		trfBuilder.stringFormat(value);

		propertyBuilder.addAllTransformationsBuilders(trfBuilder);

		propertyBuilder.type(
				XtraServerWebApiTypeUtil.getWebApiType(td, this.mappingContext.getReporter()));

		return Optional.of(propertyBuilder);
	}

}
