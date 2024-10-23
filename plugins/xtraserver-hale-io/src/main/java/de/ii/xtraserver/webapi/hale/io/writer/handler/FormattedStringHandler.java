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

import static eu.esdihumboldt.hale.common.align.model.functions.AssignFunction.PARAMETER_VALUE;

import com.google.common.collect.ListMultimap;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.hale.io.writer.handler.TransformationHandler;
import de.ii.xtraserver.webapi.hale.io.writer.XtraServerWebApiUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.functions.AssignFunction;
import eu.esdihumboldt.hale.common.align.model.functions.FormattedStringFunction;
import eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.Reference;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/** Transforms the {@link FormattedStringFunction} to a {@link FeatureSchema} */
class FormattedStringHandler extends AbstractPropertyTransformationHandler {

  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^$].+?)\\}");

  FormattedStringHandler(final MappingContext mappingContext) {
    super(mappingContext);
  }

  /**
   * @see TransformationHandler#handle(Cell)
   */
  @Override
  public Optional<ImmutableFeatureSchema.Builder> doHandle(
      final Cell propertyCell, final Property targetProperty) {

    // Get the formatted string from parameters
    final ListMultimap<String, ParameterValue> parameters =
        propertyCell.getTransformationParameters();
    List<ParameterValue> patterns =
        propertyCell.getTransformationIdentifier().equals(AssignFunction.ID_BOUND)
            ? parameters.get(PARAMETER_VALUE)
            : parameters.get("pattern");

    if (patterns == null || patterns.isEmpty() || patterns.get(0).isEmpty()) {
      mappingContext.getReporter().warn("Formatted string was ignored, no pattern set.");
      return Optional.empty();
    }
    final String pattern = patterns.get(0).as(String.class);
    final StringBuilder formattedStr =
        new StringBuilder(mappingContext.resolveProjectVars(pattern));

    Property sourceProperty = XtraServerMappingUtils.getSourceProperty(propertyCell);
    ImmutableFeatureSchema.Builder propertyBuilder;

    PropertyDefinition pd = getLastPropertyDefinition(targetProperty);
    TypeDefinition td = pd.getPropertyType();

    if (propertyCell.getSource() != null
        && propertyCell.getSource().asMap().get("var") != null
        && !propertyCell.getSource().asMap().get("var").isEmpty()) {

      Collection<? extends Entity> variableSources = propertyCell.getSource().asMap().get("var");
      List<PropertyEntityDefinition> variableSourceEntities =
          variableSources.stream()
              .map(var -> (PropertyEntityDefinition) var.getDefinition())
              .collect(Collectors.toList());

      final List<String> variables =
          variableSources.stream()
              .map(var -> propertyName(var.getDefinition().getPropertyPath()))
              .collect(Collectors.toList());

      final List<String> varList = new ArrayList<>();
      final Matcher m = VARIABLE_PATTERN.matcher(formattedStr);
      while (m.find()) {
        varList.add(m.group(1)); // the variable name, without curly
        // braces
      }

      List<String> missingVars =
          varList.stream().filter(var -> !variables.contains(var)).collect(Collectors.toList());
      if (!missingVars.isEmpty()) {
        mappingContext
            .getReporter()
            .warn(
                "Formatted string was ignored. Variables \""
                    + missingVars
                    + "\" were used in pattern, but do not exist in source properties.");
        return Optional.empty();
      }

      String resultingString = formattedStr.toString();
      for (String var : varList) {

        String varReplacementValue = var;
        if (varList.size() == 1) {
          // then the source property name does not work, 'value' or the target property name works
          // TODO - FUTURE WORK - MULTIPLICITY - Do we need to change the behavior in case of
          // multiple Property-Relations?
          varReplacementValue = "value";
        }

        resultingString =
            resultingString.replaceAll(
                "\\{" + var + "\\}", "\\{\\{" + varReplacementValue + "\\}\\}");
      }

      boolean isObjectReference = false;
      if (targetProperty
          .getDefinition()
          .getDefinition()
          .getConstraint(Reference.class)
          .isReference()) {
        final String associationTargetRef = getTargetFromSchema(targetProperty);
        if (associationTargetRef != null) {
          isObjectReference = true;
        }
      }

      Optional<String> refType = Optional.empty();

      if (isObjectReference) {

        // get feature type name from string pattern
        Pattern typePattern = Pattern.compile("^#?(\\w+_)(.*)$");
        Matcher mtp = typePattern.matcher(resultingString);

        if (mtp.matches()) {

          String typeName = mtp.group(1).toLowerCase(Locale.ENGLISH);
          if (typeName.endsWith("_")) {
            typeName = StringUtils.chop(typeName);
          }

          refType = Optional.of(typeName);

          resultingString = mtp.group(2);

        } else {
          mappingContext
              .getReporter()
              .warn(
                  "Formatted string for object reference encountered."
                      + "The pattern - {0} - does not match the expected format. Object reference was not created.",
                  pattern);
        }
      }

      propertyBuilder = buildPropertyPath(propertyCell, targetProperty, refType);

      // check if the property has already been established
      // TODO - FUTURE WORK (multiplicity not supported yet)
      if (!propertyBuilder.build().getEffectiveSourcePaths().isEmpty()) {
        mappingContext
            .getReporter()
            .warn(
                "Multiple 'Formatted string'-relations for same target property ({0}) not supported yet. Only the first encountered relationship will be encoded. Ignoring pattern {1}.",
                fullDisplayPath(targetProperty), pattern);
        return Optional.of(propertyBuilder);
      }

      Optional<String> joinSourcePath =
          this.mappingContext.computeJoinSourcePath(sourceProperty.getDefinition());
      if (variables.size() == 1) {
        String sourcePath =
            this.mappingContext.computeSourcePropertyName(variableSourceEntities.get(0));
        if (joinSourcePath.isPresent()) {
          if (this.mappingContext.hasFirstObjectBuilderMapping(targetProperty)) {
            this.mappingContext
                .getFirstObjectBuilder(targetProperty)
                .sourcePath(joinSourcePath.get());
            propertyBuilder.sourcePath(sourcePath);
          } else {
            propertyBuilder.sourcePath(joinSourcePath.get() + "/" + sourcePath);
          }
        } else {
          propertyBuilder.sourcePath(sourcePath);
        }
      } else {
        for (PropertyEntityDefinition ped : variableSourceEntities) {
          String sourcePath = this.mappingContext.computeSourcePropertyName(ped);
          if (joinSourcePath.isPresent()) {
            if (this.mappingContext.hasFirstObjectBuilderMapping(targetProperty)) {
              this.mappingContext
                  .getFirstObjectBuilder(targetProperty)
                  .sourcePath(joinSourcePath.get());
              propertyBuilder.addSourcePaths(sourcePath);
            } else {
              propertyBuilder.addSourcePaths(joinSourcePath.get() + "/" + sourcePath);
            }
          } else {
            propertyBuilder.addSourcePaths(sourcePath);
          }
        }
      }

      if (!Objects.equals(resultingString, "{{value}}")) {
        ImmutablePropertyTransformation.Builder trfBuilder =
            new ImmutablePropertyTransformation.Builder();
        trfBuilder.stringFormat(resultingString);

        propertyBuilder.addAllTransformationsBuilders(trfBuilder);
      }

    } else {
      // Simple string without formatting

      propertyBuilder = buildPropertyPath(propertyCell, targetProperty);

      Optional<String> joinSourcePath =
          this.mappingContext.computeJoinSourcePath(sourceProperty.getDefinition());
      String sourcePath =
          this.mappingContext.computeSourcePropertyName(sourceProperty.getDefinition());
      if (joinSourcePath.isPresent()) {
        if (this.mappingContext.hasFirstObjectBuilderMapping(targetProperty)) {
          this.mappingContext
              .getFirstObjectBuilder(targetProperty)
              .sourcePath(joinSourcePath.get());
        } else {
          sourcePath = joinSourcePath.get() + "/" + sourcePath;
        }
      }

      propertyBuilder.sourcePath(sourcePath);

      ImmutablePropertyTransformation.Builder trfBuilder =
          new ImmutablePropertyTransformation.Builder();
      trfBuilder.stringFormat(formattedStr.toString());

      propertyBuilder.addAllTransformationsBuilders(trfBuilder);
    }

    SchemaBase.Type baseType =
        XtraServerWebApiUtil.getWebApiType(td, this.mappingContext.getReporter());
    if (isMultiValuedPropertyPerSchemaDefinition(pd)) {
      propertyBuilder.type(Type.VALUE_ARRAY);
      propertyBuilder.valueType(baseType);
    } else {
      propertyBuilder.type(baseType);
    }

    return Optional.of(propertyBuilder);
  }
}
