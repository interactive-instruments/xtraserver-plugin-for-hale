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

import com.google.common.collect.ListMultimap;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.hale.io.writer.handler.TransformationHandler;
import de.ii.xtraserver.webapi.hale.io.writer.XtraServerWebApiTypeUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.functions.RenameFunction;
import eu.esdihumboldt.hale.common.schema.model.*;

import javax.xml.namespace.QName;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    PropertyDefinition pd = getLastPropertyDefinition(targetProperty);

    TypeDefinition td = pd.getPropertyType();

    Property sourceProperty = XtraServerMappingUtils.getSourceProperty(propertyCell);
    String sourcePath = sourceProperty
        .getDefinition().getDefinition().getDisplayName();

    // build the current schema structure for inspection
    FeatureSchema fs = propertyBuilder.build();

    if (fs.getEffectiveSourcePaths().contains(sourcePath)) {
      // then ignore the property - at least for now
      // TODO Wie müssen wir bei Joins mit mehreren Haupttabellen vorgehen?
//            mappingContext.getReporter().warn("Encountered sourcePath "+sourcePath+" again (" +pName +")");
    } else {

      if (!fs.getSourcePath().isPresent() && fs.getSourcePaths().isEmpty()) {
        // the property has not been created yet
        propertyBuilder.sourcePath(sourcePath).type(
            XtraServerWebApiTypeUtil.getWebApiType(td, this.mappingContext.getReporter()));

      } else {

        if (fs.getSourcePath().isPresent() && fs.getSourcePaths().isEmpty()) {

              /* multiplicity needs to be considered -
              We encountered another cell that applies to the same target property
              (with different source path). */

          // move current sourcePath to sourcePaths, then unset sourcePath
          propertyBuilder.addSourcePaths(fs.getSourcePath().get());
          propertyBuilder.sourcePath(Optional.empty());

          // Switch type to value type, and set type to VALUE_ARRAY.
          propertyBuilder.valueType(fs.getType());
          propertyBuilder.type(SchemaBase.Type.VALUE_ARRAY);
        }

        // add new source path to sourcePaths
        propertyBuilder.addSourcePaths(sourcePath);
      }
    }

    return Optional.of(propertyBuilder);
  }
}