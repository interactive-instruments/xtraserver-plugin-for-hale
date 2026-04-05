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
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.webapi.hale.io.writer.XtraServerWebApiUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Transforms the custom function 'custom:alignment:adv.landnutzung.identifier' to a {@link
 * FeatureSchema}
 */
class CustomFunctionAdvLnToIdentifier extends ClassificationMappingHandler {

  public static final String FUNCTION_ID = "custom:alignment:adv.landnutzung.identifier";

  CustomFunctionAdvLnToIdentifier(final MappingContext mappingContext) {
    super(mappingContext);
  }

  /**
   * @see AbstractPropertyTransformationHandler#doHandle(Cell, Property, String)
   */
  @Override
  public Optional<ImmutableFeatureSchema.Builder> doHandle(
      final Cell propertyCell, final Property targetProperty, String providerId) {

    Property sourceProperty = XtraServerMappingUtils.getSourceProperty(propertyCell);
    Map<String, Property> sourceProperties =
        XtraServerMappingUtils.getSourceProperties(propertyCell);
    Map<String, Collection<ParameterValue>> transformationParameters =
        Objects.nonNull(propertyCell.getTransformationParameters())
            ? propertyCell.getTransformationParameters().asMap()
            : Map.of();

    ImmutableFeatureSchema.Builder propertyBuilder =
        buildPropertyPath(propertyCell, targetProperty);

    String sourcePropertyName = propertyName(sourceProperty);

    String sourcePath =
        this.mappingContext.computeSourcePropertyName(sourceProperty.getDefinition());
    Map<String, String> sourcePaths =
        sourceProperties.entrySet().stream()
            .map(
                p ->
                    Map.entry(
                        p.getKey(),
                        this.mappingContext.computeSourcePropertyName(
                            p.getValue().getDefinition())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//TODO: error if no id
    String level =
        transformationParameters.containsKey("level")
                && transformationParameters.get("level").size() > 0
            ? transformationParameters.get("level").iterator().next().getStringRepresentation()
            : "0";
    if (sourcePaths.containsKey("is_additional_use") && !Objects.equals(level, "1")) {
      level =
          String.format(
              "CASE WHEN (%s = '1000') THEN '1' ELSE '%s' END",
              sourcePaths.get("is_additional_use"), level);
    } else {
      level = String.format("'%s'", level);
    }
    String expression =
        String.format(
            "SUBSTR(%1$s, 1, 4) || 'N' || (%2$s) || SUBSTR(%1$s, 7)",
            sourcePaths.get("id"), level);

    propertyBuilder.sourcePath(String.format("[EXPRESSION]{sql=%s}", expression));

    PropertyDefinition pd = getLastPropertyDefinition(targetProperty);
    TypeDefinition td = pd.getPropertyType();

    SchemaBase.Type baseType =
        XtraServerWebApiUtil.getWebApiType(td, this.mappingContext.getReporter());
    if (isMultiValuedPropertyPerSchemaDefinition(pd)) {
      propertyBuilder.type(SchemaBase.Type.VALUE_ARRAY);
      propertyBuilder.valueType(baseType);
    } else {
      propertyBuilder.type(baseType);
    }

    String targetPropertyName = propertyName(targetProperty);

    if (targetPropertyName.equals("id")) {

      propertyBuilder.role(SchemaBase.Role.ID);

      this.mappingContext.setMainSortKeyField(sourcePaths.get("id"));

    } else {

      ImmutablePropertyTransformation.Builder trfBuilder =
          new ImmutablePropertyTransformation.Builder();
      String value = mappingContext.getFeatureTypeName() + "_{{value}}";
      trfBuilder.stringFormat(value);

      propertyBuilder.addAllTransformationsBuilders(trfBuilder);
    }
    return Optional.of(propertyBuilder);
  }
}
