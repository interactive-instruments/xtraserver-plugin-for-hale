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

import static eu.esdihumboldt.hale.common.align.model.functions.AssignFunction.PARAMETER_VALUE;

import com.google.common.collect.ListMultimap;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.hale.io.writer.handler.TransformationHandler;
import de.interactive_instruments.xtraserver.config.api.Hints;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.functions.AssignFunction;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import java.util.List;
import java.util.Optional;

/**
 * Transforms the {@link AssignFunction} to a {@link FeatureSchema}
 */
class AssignHandler extends AbstractPropertyTransformationHandler {

	private final static String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
	private final static String NIL_REASON = "@nilReason";

	AssignHandler(final MappingContext mappingContext) {
		super(mappingContext);
	}

	/**
	 * @see TransformationHandler#handle(Cell)
	 */
	@Override
	public Optional<ImmutableFeatureSchema.Builder> doHandle(final Cell propertyCell, final Property targetProperty) {

		ImmutableFeatureSchema.Builder propertyBuilder = buildPropertyPath(targetProperty);

		// Assign constant value from parameters
		final ListMultimap<String, ParameterValue> parameters = propertyCell
				.getTransformationParameters();
		final List<ParameterValue> valueParams = parameters.get(PARAMETER_VALUE);
		final String value = valueParams.get(0).getStringRepresentation();

		propertyBuilder.constantValue(value);
		propertyBuilder.type(SchemaBase.Type.STRING);

		if (propertyCell.getTransformationIdentifier().equals(AssignFunction.ID_BOUND)) {
			// TODO - will be implemented later on (in scope of P109n)
		}

		return Optional.of(propertyBuilder);
	}
}
