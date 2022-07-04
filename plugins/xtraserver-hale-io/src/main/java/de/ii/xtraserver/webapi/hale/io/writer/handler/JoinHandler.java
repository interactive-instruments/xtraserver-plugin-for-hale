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
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.EntityDefinition;
import eu.esdihumboldt.hale.common.align.model.functions.JoinFunction;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;

import java.util.Collection;

/**
 * Transforms the {@link JoinFunction} to a {@link FeatureSchema}
 */
class JoinHandler extends AbstractTypeTransformationHandler {

	JoinHandler(final MappingContext mappingContext) {
		super(mappingContext);
	}

	/**
	 * @see TypeTransformationHandler#handle(Cell)
	 */
	@Override
	public void doHandle(final Collection<? extends Entity> sourceTypes, final Entity targetType,
			final Cell typeCell) {

//		EntityDefinition sourceType = sourceTypes.iterator().next().getDefinition();
//
//		final TypeDefinition sourceTypeDefinition = sourceType.getType();
//		final String tableName = sourceTypeDefinition.getDisplayName();
//
//		ImmutableFeatureSchema.Builder builder = (ImmutableFeatureSchema.Builder)mappingContext.getFeatureBuilder();
//		builder.sourcePath("/" + tableName).label(mappingContext.getFeatureTypeName());
	}

}
