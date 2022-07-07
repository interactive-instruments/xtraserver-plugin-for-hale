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
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.hale.io.writer.handler.TransformationHandler;
import de.ii.xtraserver.webapi.hale.io.writer.XtraServerWebApiTypeUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.functions.RenameFunction;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import java.util.Optional;

/**
 * Transforms the {@link RenameFunction} to a {@link FeatureSchema}
 */
class RenameHandler extends AbstractPropertyTransformationHandler {

  RenameHandler(final MappingContext mappingContext) {
    super(mappingContext);
  }

  /**
   * @see TransformationHandler#handle(Cell)
   */
  @Override
  public Optional<ImmutableFeatureSchema.Builder> doHandle(final Cell propertyCell,
      final Property targetProperty) {

    ImmutableFeatureSchema.Builder propertyBuilder = buildPropertyPath(targetProperty);

    Property sourceProperty = XtraServerMappingUtils.getSourceProperty(propertyCell);
    String sourcePath = this.mappingContext.computeSourcePath(sourceProperty
        .getDefinition());

    setTypesAndSourcePaths(propertyBuilder, targetProperty, sourcePath);

    return Optional.of(propertyBuilder);
  }

  /**
   * Will set type fields in the builder, i.e. 'type', and 'valueType' (if applicable) - depending
   * upon the type definition and the cardinality of the target property (more precisely, the last
   * property in its property path). In addition, if multiple sourcePaths are added to the builder
   * (e.g. because of another property relation with the same target property), 'type' will be set
   * to VALUE_ARRAY, and 'valueType' will be set depending upon the type definition. In that case,
   * the value of 'sourcePath' will be moved to 'sourcePaths' as well (if not already done).
   *
   * @param propertyBuilder the builder created for the target property
   * @param targetProperty  the target of a property relation (as provided to property handlers)
   * @param sourcePath      the source path to set/add
   */
  private void setTypesAndSourcePaths(Builder propertyBuilder, Property targetProperty,
      String sourcePath) {

    PropertyDefinition targetPd = getLastPropertyDefinition(targetProperty);
    TypeDefinition td = targetPd.getPropertyType();
    SchemaBase.Type baseType = XtraServerWebApiTypeUtil.getWebApiType(td,
        this.mappingContext.getReporter());

    // build the current schema structure for inspection
    FeatureSchema fs = propertyBuilder.build();

    if (fs.getEffectiveSourcePaths().contains(sourcePath)) {
      // then ignore the property - at least for now
      // TODO - FUTURE WORK (Joins with multiple main tables)
//            mappingContext.getReporter().warn("Encountered sourcePath "+sourcePath+" again (" +pName +")");
    } else {

      if (!fs.getSourcePath().isPresent() && fs.getSourcePaths().isEmpty()) {

        // the property has not been created yet
        propertyBuilder.sourcePath(sourcePath);
        if (isMultiValuedPropertyPerSchemaDefinition(targetPd)) {
          propertyBuilder.type(Type.VALUE_ARRAY);
          propertyBuilder.valueType(baseType);
        } else {
          propertyBuilder.type(baseType);
        }

      } else {

        if (fs.getSourcePath().isPresent() && fs.getSourcePaths().isEmpty()) {

          /* We encountered another cell that applies to the same target property
              (with different source path). */

          // move current sourcePath to sourcePaths, then unset sourcePath
          propertyBuilder.addSourcePaths(fs.getSourcePath().get());
          propertyBuilder.sourcePath(Optional.empty());

          propertyBuilder.valueType(baseType);
          propertyBuilder.type(SchemaBase.Type.VALUE_ARRAY);
        }

        // add new source path to sourcePaths
        propertyBuilder.addSourcePaths(sourcePath);
      }
    }

  }
}