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
import de.ii.xtraserver.hale.io.writer.handler.TransformationHandler;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.functions.RenameFunction;
import java.util.Optional;

/**
 * Transforms the {@link RenameFunction} to a {@link FeatureSchema}
 */
class SqlExpressionHandler extends AbstractPropertyTransformationHandler {

	final static String PARAMETER_VALUE = "sql";

	SqlExpressionHandler(final MappingContext mappingContext) {
		super(mappingContext);
	}

	/**
	 * @see TransformationHandler#handle(Cell)
	 */
	@Override
	public Optional<FeatureSchema.Builder> doHandle(final Cell propertyCell, final Property targetProperty) {

		return Optional.empty();
	}

}
