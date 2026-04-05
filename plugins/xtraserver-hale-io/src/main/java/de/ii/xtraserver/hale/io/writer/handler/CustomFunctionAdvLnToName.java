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
import eu.esdihumboldt.hale.common.align.model.Property;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

/**
 * Transforms the custom function 'custom:alignment:adv.landnutzung.lagebezeichnung2name' to a
 * {@link MappingValue}
 */
class CustomFunctionAdvLnToName
    extends de.ii.xtraserver.hale.io.writer.handler.ClassificationMappingHandler {

  public static final String FUNCTION_ID = "custom:alignment:adv.landnutzung.lagebezeichnung2name";

  CustomFunctionAdvLnToName(final MappingContext mappingContext) {
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

    Map<String, String> sourcePaths =
        sourceProperties.entrySet().stream()
            .map(
                p ->
                    Map.entry(
                        p.getKey(), propertyName(p.getValue().getDefinition().getPropertyPath())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // TODO: error if no params?

    String lookup =
        String.format(
            "xsv_get_aaa_katalog('73013', $T$.%s || $T$.%s || $T$.%s || $T$.%s || $T$.%s)",
            sourcePaths.get("land"),
            sourcePaths.get("regierungsbezirk"),
            sourcePaths.get("kreis"),
            sourcePaths.get("gemeinde"),
            sourcePaths.get("lage"));

    String expression =
        String.format(
            "CASE WHEN (%1$s IS NULL) THEN %2$s ELSE %1$s END",
            "$T$." + sourcePaths.get("unverschluesselt"), lookup);

    final List<QName> path = buildPath(targetProperty.getDefinition().getPropertyPath());

    final MappingValue mappingValue =
        new MappingValueBuilder().expression().qualifiedTargetPath(path).value(expression).build();

    return Optional.of(mappingValue);
  }
}
