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

import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraserver.hale.io.writer.handler.TransformationHandler;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.PrimaryKey;

/**
 * Handler for Type transformations
 */
@FunctionalInterface
public interface TypeTransformationHandler extends
    TransformationHandler<ImmutableFeatureSchema.Builder> {

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
   * Check if a transformation is supported by the {@link TypeTransformationHandlerFactory}
   *
   * @param typeTransformationIdentifier the hale identifier of the transformation
   * @return true if the transformation is supported, false otherwise
   */
  public static boolean isTransformationSupported(final String typeTransformationIdentifier) {
    return TypeTransformationHandlerFactory.SUPPORTED_TYPES
        .contains(typeTransformationIdentifier);
  }

  /**
   * @param definition the type definition from the main source entity definition that is specified
   *                   for the type mapping
   * @return the name of the primary key field, if such a constraint is defined in the definition,
   * else null
   */
  public static String getPrimaryKey(final TypeDefinition definition) {
    final PrimaryKey primaryKey = definition.getConstraint(PrimaryKey.class);
    if (primaryKey == null || primaryKey.getPrimaryKeyPath() == null
        || primaryKey.getPrimaryKeyPath().isEmpty()) {
      return null;
    }
    return primaryKey.getPrimaryKeyPath().iterator().next().getLocalPart();
  }
}
