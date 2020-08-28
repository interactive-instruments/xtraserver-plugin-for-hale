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

import java.util.Collection;

import de.interactive_instruments.xtraserver.config.api.FeatureTypeMapping;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.functions.RetypeFunction;

/**
 * Transforms the {@link RetypeFunction} to a {@link FeatureTypeMapping}
 * 
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
class RetypeHandler extends AbstractTypeTransformationHandler {

	RetypeHandler(final MappingContext mappingContext) {
		super(mappingContext);
	}

	/**
	 * @see TypeTransformationHandler#handle(eu.esdihumboldt.hale.common.align.model.Cell)
	 */
	@Override
	public void doHandle(final Collection<? extends Entity> sourceTypes, final Entity targetType,
			final Cell typeCell) {
		createTableIfAbsent(sourceTypes.iterator().next().getDefinition());
	}

}
