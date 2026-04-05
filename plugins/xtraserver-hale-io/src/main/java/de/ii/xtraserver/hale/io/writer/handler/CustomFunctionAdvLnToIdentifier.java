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

import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.interactive_instruments.xtraserver.config.api.MappingValue;
import de.interactive_instruments.xtraserver.config.api.MappingValueBuilder;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.Property;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

/**
 * Transforms the custom function 'custom:alignment:adv.landnutzung.identifier' to a {@link
 * MappingValue}
 */
class CustomFunctionAdvLnToIdentifier
    extends de.ii.xtraserver.hale.io.writer.handler.FormattedStringHandler {

  public static final String FUNCTION_ID = "custom:alignment:adv.landnutzung.identifier";

  CustomFunctionAdvLnToIdentifier(final MappingContext mappingContext) {
    super(mappingContext);
  }

  /**
   * @see
   *     AbstractPropertyTransformationHandler#doHandle(eu.esdihumboldt.hale.common.align.model.Cell,
   *     eu.esdihumboldt.hale.common.align.model.Property)
   */
  @Override
  public Optional<MappingValue> doHandle(final Cell propertyCell, final Property targetProperty) {

    Map<String, Property> sourceProperties =
        XtraServerMappingUtils.getSourceProperties(propertyCell);
    Map<String, Collection<ParameterValue>> transformationParameters =
        Objects.nonNull(propertyCell.getTransformationParameters())
            ? propertyCell.getTransformationParameters().asMap()
            : Map.of();

    Map<String, String> sourcePaths =
        sourceProperties.entrySet().stream()
            .map(
                p ->
                    Map.entry(
                        p.getKey(), propertyName(p.getValue().getDefinition().getPropertyPath())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // TODO: error if no id

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
            "$T$." + sourcePaths.get("id"), level);

    final List<QName> path = buildPath(targetProperty.getDefinition().getPropertyPath());

    final MappingValue mappingValue =
        new MappingValueBuilder().expression().qualifiedTargetPath(path).value(expression).build();

    return Optional.of(mappingValue);
  }
}
