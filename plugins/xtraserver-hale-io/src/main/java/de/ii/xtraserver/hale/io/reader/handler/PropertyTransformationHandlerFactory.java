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

package de.ii.xtraserver.hale.io.reader.handler;

import java.util.Optional;

import de.interactive_instruments.xtraserver.config.api.MappingValue;

/**
 * Factory for creating Property Transformation Handlers
 * 
 * @author zahnen
 */
public class PropertyTransformationHandlerFactory {

	private final TransformationContext transformationContext;

	/**
	 * @param transformationContext the transformation context
	 */
	public PropertyTransformationHandlerFactory(TransformationContext transformationContext) {
		this.transformationContext = transformationContext;
	}

	/**
	 * Create a new Property Transformation Handler
	 * 
	 * @param mappingValue the mapping that should be transformed
	 * 
	 * @return new TypeHandler
	 */
	public Optional<PropertyTransformationHandler> create(final MappingValue mappingValue) {
		if (mappingValue.isExpression()
				&& FormattedStringHandler.isFormattedStringExpression(mappingValue.getValue())) {
			return Optional.of(new FormattedStringHandler(transformationContext));
		}
		else if (mappingValue.isConstant()) {
			return Optional.of(new AssignHandler(transformationContext));
		}
		else if (mappingValue.isColumn()) {
			return Optional.of(new RenameHandler(transformationContext));
		}

		return Optional.empty();
	}

}
