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

/**
 * Handler for Type transformations
 */
@FunctionalInterface
public interface TypeTransformationHandler extends TransformationHandler<FeatureSchema.Builder> {

	/**
	 * Create a new {@link TypeTransformationHandlerFactory} object
	 * 
	 * @param mappingContext Mapping Context
	 * @return new Type Transformation Handler Factory object
	 */
	public static TypeTransformationHandlerFactory createFactory(
			final MappingContext mappingContext) {
		return new TypeTransformationHandlerFactory(mappingContext);
	}

	/**
	 * Check if a transformation is supported by the
	 * {@link TypeTransformationHandlerFactory}
	 * 
	 * @param typeTransformationIdentifier the hale identifier of the transformation
	 * @return true if the transformation is supported, false otherwise
	 */
	public static boolean isTransformationSupported(final String typeTransformationIdentifier) {
		return TypeTransformationHandlerFactory.SUPPORTED_TYPES
				.contains(typeTransformationIdentifier);
	}
}
