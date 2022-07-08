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
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Property;
import java.util.Locale;
import java.util.Optional;

/**
 * * Transforms the custom function 'custom:alignment:adv.inspire.GeographicalName.simple' to a
 * {@link FeatureSchema}
 */
class CustomFunctionAdvToGeographicalNameSimple extends FormattedStringHandler {

  public final static String FUNCTION_ID = "custom:alignment:adv.inspire.GeographicalName.simple";

  CustomFunctionAdvToGeographicalNameSimple(final MappingContext mappingContext) {
    super(mappingContext);
  }

  /**
   * @see AbstractPropertyTransformationHandler#doHandle(Cell, Property)
   */
  @Override
  public Optional<ImmutableFeatureSchema.Builder> doHandle(final Cell propertyCell,
      final Property targetProperty) {

    ImmutableFeatureSchema.Builder typeBuilder = mappingContext.getFeatureBuilder();
    String featureTypeNameLowerCase = mappingContext.getFeatureTypeName()
        .toLowerCase(Locale.ENGLISH);
    ImmutableFeatureSchema.Builder propertyBuilder = new ImmutableFeatureSchema.Builder();
    String pName = "name_deu";
    propertyBuilder.name(pName);
    String documentationVariableFacetPrefix = "${" + featureTypeNameLowerCase + "." + pName;
    propertyBuilder.label(documentationVariableFacetPrefix + ".label:-}");
    propertyBuilder.description(documentationVariableFacetPrefix + ".description:-}");
    typeBuilder.getPropertyMap().put(pName, propertyBuilder);

    Property sourceProperty = XtraServerMappingUtils.getSourceProperty(propertyCell);
    String sourcePath = this.mappingContext.computeSourcePath(sourceProperty
        .getDefinition());
    propertyBuilder.sourcePath(sourcePath);

    propertyBuilder.type(SchemaBase.Type.STRING);

    return Optional.of(propertyBuilder);
  }

}
