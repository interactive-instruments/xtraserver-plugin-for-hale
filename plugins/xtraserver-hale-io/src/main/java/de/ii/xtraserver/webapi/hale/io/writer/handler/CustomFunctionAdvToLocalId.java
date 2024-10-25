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
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.webapi.hale.io.writer.XtraServerWebApiUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import java.util.Optional;

/**
 * Transforms the custom function 'custom:alignment:adv.inspire.id.default' to a {@link
 * FeatureSchema}
 */
class CustomFunctionAdvToLocalId extends FormattedStringHandler {

  public final static String FUNCTION_ID = "custom:alignment:adv.inspire.id.default";

  CustomFunctionAdvToLocalId(final MappingContext mappingContext) {
    super(mappingContext);
  }

  /**
   * @see AbstractPropertyTransformationHandler#doHandle(Cell, Property, String)
   */
  @Override
  public Optional<ImmutableFeatureSchema.Builder> doHandle(final Cell propertyCell,
                                                           final Property targetProperty, String providerId) {

    Property sourceProperty = XtraServerMappingUtils.getSourceProperty(propertyCell);

    ImmutableFeatureSchema.Builder propertyBuilder = buildPropertyPath(propertyCell, targetProperty);

    String sourcePropertyName = propertyName(sourceProperty);

    String sourcePath = this.mappingContext.computeSourcePropertyName(sourceProperty
        .getDefinition());
    propertyBuilder.sourcePath(sourcePath);

    PropertyDefinition pd = getLastPropertyDefinition(targetProperty);
    TypeDefinition td = pd.getPropertyType();

    SchemaBase.Type baseType = XtraServerWebApiUtil.getWebApiType(td,
        this.mappingContext.getReporter());
    if (isMultiValuedPropertyPerSchemaDefinition(pd)) {
      propertyBuilder.type(Type.VALUE_ARRAY);
      propertyBuilder.valueType(baseType);
    } else {
      propertyBuilder.type(baseType);
    }

    String targetPropertyName = propertyName(targetProperty);

    if(targetPropertyName.equals("id")) {

      propertyBuilder.role(Role.ID);

      this.mappingContext.setMainSortKeyField(sourcePropertyName);

    } else {

      ImmutablePropertyTransformation.Builder trfBuilder = new ImmutablePropertyTransformation.Builder();
      String value = mappingContext.getFeatureTypeName() + "_{{value}}";
      trfBuilder.stringFormat(value);

      propertyBuilder.addAllTransformationsBuilders(trfBuilder);
    }
    return Optional.of(propertyBuilder);
  }

}
