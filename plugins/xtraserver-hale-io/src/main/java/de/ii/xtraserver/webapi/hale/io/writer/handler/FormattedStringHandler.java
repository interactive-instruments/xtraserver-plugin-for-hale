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
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraserver.hale.io.writer.handler.TransformationHandler;
import de.ii.xtraserver.webapi.hale.io.writer.XtraServerWebApiTypeUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.functions.FormattedStringFunction;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.Reference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Transforms the {@link FormattedStringFunction} to a {@link FeatureSchema}
 */
class FormattedStringHandler extends AbstractPropertyTransformationHandler {

  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^$].+?)\\}");

  FormattedStringHandler(final MappingContext mappingContext) {
    super(mappingContext);
  }

  /**
   * @see TransformationHandler#handle(Cell)
   */
  @Override
  public Optional<ImmutableFeatureSchema.Builder> doHandle(final Cell propertyCell,
      final Property targetProperty) {

    // Get the formatted string from parameters
    final ListMultimap<String, ParameterValue> parameters = propertyCell
        .getTransformationParameters();
    final List<ParameterValue> patterns = parameters.get("pattern");
    if (patterns == null || patterns.isEmpty() || patterns.get(0).isEmpty()) {
      mappingContext.getReporter().warn("Formatted string was ignored, no pattern set.");
      return Optional.empty();
    }
    final String pattern = patterns.get(0).as(String.class);
    final StringBuilder formattedStr = new StringBuilder(
        mappingContext.resolveProjectVars(pattern));
    final int patternLength = formattedStr.length();

    ImmutableFeatureSchema.Builder propertyBuilder = buildPropertyPath(targetProperty);

    PropertyDefinition pd = getLastPropertyDefinition(targetProperty);
    TypeDefinition td = pd.getPropertyType();

    // check if the property has already been established
    // TODO - FUTURE WORK (multiplicity not supported yet)
    if (!propertyBuilder.build().getEffectiveSourcePaths().isEmpty()) {
      String targetPropertyName = targetProperty.getDefinition().getDefinition().getDisplayName();
      mappingContext.getReporter().warn(
          "Multiple 'Formatted string'-relations for same target property ({0}) not supported yet. Only the first encountered relationship will be encoded. Ignoring pattern {1}.",
          fullDisplayPath(targetProperty), pattern);
      return Optional.of(propertyBuilder);
    }

    if (propertyCell.getSource() != null && propertyCell.getSource().asMap().get("var") != null
        && !propertyCell.getSource().asMap().get("var").isEmpty()) {

      final List<String> variables = propertyCell.getSource().asMap().get("var").stream()
          .map(var -> propertyName(var.getDefinition().getPropertyPath()))
          .collect(Collectors.toList());

      final List<int[]> startEndList = new ArrayList<int[]>();
      final List<String> varList = new ArrayList<String>();
      final Matcher m = VARIABLE_PATTERN.matcher(formattedStr);
      while (m.find()) {
        int[] startEnd = new int[2];
        startEnd[0] = m.start(); // index of '{' character
        startEnd[1] = m.end(); // index of '}' character
        startEndList.add(startEnd);
        varList.add(m.group(1)); // the variable name, without curly
        // braces
      }

      List<String> missingVars = varList.stream().filter(var -> !variables.contains(var))
          .collect(Collectors.toList());
      if (!missingVars.isEmpty()) {
        mappingContext.getReporter().warn("Formatted string was ignored. Variables \""
            + missingVars
            + "\" were used in pattern, but do not exist in source properties.");
        return Optional.empty();
      }

      String resultingString = formattedStr.toString();
      for (String var : varList) {
        resultingString = resultingString.replaceAll("\\{" + var + "\\}",
            "\\{\\{" + var + "\\}\\}");
      }

      boolean isObjectReference = false;
      if (targetProperty.getDefinition().getDefinition().getConstraint(Reference.class)
          .isReference()) {
        final String associationTargetRef = getTargetFromSchema(targetProperty);
        if (associationTargetRef != null) {
          isObjectReference = true;
        }
      }

      if (isObjectReference) {

        // get feature type name from string pattern
        Pattern typePattern = Pattern.compile("^#?(\\w+_)(.*)$");
        Matcher mtp = typePattern.matcher(resultingString);

        if (mtp.matches()) {

          String typeName = mtp.group(1);
          if (typeName.endsWith("_")) {
            typeName = StringUtils.chop(typeName);
          }

          String part2 = mtp.group(2);

          // create service URL
          resultingString =
              "{{serviceUrl}}/collections/" + typeName.toLowerCase(Locale.ENGLISH) + "/items/"
                  + part2;

        } else {
          mappingContext.getReporter().warn("Formatted string for object reference encountered." +
                  "The pattern - {0} - does not match the expected format. Object reference was not created.",
              pattern);
        }


      }

      if (variables.size() == 1) {
        propertyBuilder.sourcePath(variables.get(0));
      } else {
        propertyBuilder.addAllSourcePaths(variables);
      }

      ImmutablePropertyTransformation.Builder trfBuilder = new ImmutablePropertyTransformation.Builder();
      trfBuilder.stringFormat(resultingString);

      propertyBuilder.addAllTransformationsBuilders(trfBuilder);

    } else {

      // Simple string without formatting
      propertyBuilder.constantValue(formattedStr.toString());
    }

    SchemaBase.Type baseType = XtraServerWebApiTypeUtil.getWebApiType(td,
        this.mappingContext.getReporter());
    if (isMultiValuedPropertyPerSchemaDefinition(pd)) {
      propertyBuilder.type(Type.VALUE_ARRAY);
      propertyBuilder.valueType(baseType);
    } else {
      propertyBuilder.type(baseType);
    }

    return Optional.of(propertyBuilder);
  }

}
