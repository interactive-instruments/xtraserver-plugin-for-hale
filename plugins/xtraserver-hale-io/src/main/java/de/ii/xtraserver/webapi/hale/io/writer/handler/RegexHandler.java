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
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.webapi.hale.io.writer.XtraServerWebApiUtil;
import eu.esdihumboldt.cst.functions.string.RegexAnalysisFunction;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.Reference;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Transforms the {@link RegexAnalysisFunction} to a {@link FeatureSchema}
 */
class RegexHandler extends AbstractPropertyTransformationHandler {

  RegexHandler(final MappingContext mappingContext) {
    super(mappingContext);
  }

  /**
   * @see AbstractPropertyTransformationHandler#doHandle(Cell, Property)
   */
  @Override
  protected Optional<ImmutableFeatureSchema.Builder> doHandle(final Cell propertyCell,
      final Property targetProperty) {

    // identify regex expression
    final ListMultimap<String, ParameterValue> parameters = propertyCell
        .getTransformationParameters();
    final List<ParameterValue> regexParam = parameters.get("regexPattern");
    if (regexParam.isEmpty()) {
      throw new IllegalArgumentException("Regular expression not set");
    }
    final String regex = regexParam.get(0).as(String.class);

    // identify output format expression
    final List<ParameterValue> outputFormatParam = parameters.get("outputFormat");
    if (outputFormatParam.isEmpty()) {
      throw new IllegalArgumentException("Output format for regular expression not set");
    }
    final String outputFormat = outputFormatParam.get(0).as(String.class);

    // NOTE: the following replaceAll uses the capture group mechanism
    String newOutputFormat = outputFormat.replaceAll("\\{(\\d+)\\}", "\\$$1");

    // TODO refactoring - quite similar to code in FormattedStringHandler
    boolean isObjectReference = false;
    if (targetProperty.getDefinition().getDefinition().getConstraint(Reference.class)
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
      Matcher mtp = typePattern.matcher(newOutputFormat);

      if (mtp.matches()) {

        String typeName = mtp.group(1).toLowerCase(Locale.ENGLISH);
        if (typeName.endsWith("_")) {
          typeName = StringUtils.chop(typeName);
        }

        refType = Optional.of(typeName);

        newOutputFormat = mtp.group(2);

      } else {
        mappingContext.getReporter().warn("'Regex Analysis'-relation for object reference encountered." +
                "The output format - {0} - does not match the expected format. Object reference was not created.",
            outputFormat);
      }
    }

    Property sourceProperty = XtraServerMappingUtils.getSourceProperty(propertyCell);
    ImmutableFeatureSchema.Builder propertyBuilder = buildPropertyPath(propertyCell, targetProperty, refType);

    PropertyDefinition pd = getLastPropertyDefinition(targetProperty);
    TypeDefinition td = pd.getPropertyType();

    // check if the property has already been established
    // TODO - FUTURE WORK (multiplicity not supported yet)
    if (!propertyBuilder.build().getEffectiveSourcePaths().isEmpty()) {
      mappingContext.getReporter().warn(
              "Multiple 'Regex Analysis'-relations for same target property ({0}) not supported yet. Only the first encountered relationship will be encoded. Ignoring regex {1}.",
              fullDisplayPath(targetProperty), regex);
      return Optional.of(propertyBuilder);
    }

    Optional<String> joinSourcePath = this.mappingContext.computeJoinSourcePath(
        sourceProperty.getDefinition());
    String sourcePath = this.mappingContext.computeSourcePropertyName(sourceProperty
        .getDefinition());
    if (joinSourcePath.isPresent()) {
      if (this.mappingContext.hasFirstObjectBuilderMapping(targetProperty)) {
        this.mappingContext.getFirstObjectBuilder(targetProperty)
            .sourcePath(joinSourcePath.get());
        propertyBuilder.sourcePath(sourcePath);
      } else {
        propertyBuilder.sourcePath(joinSourcePath.get() + "/" + sourcePath);
      }
    } else {
      propertyBuilder.sourcePath(sourcePath);
    }

    ImmutablePropertyTransformation.Builder trfBuilder = new ImmutablePropertyTransformation.Builder();
    String resultingString = "{{value | replace:'"+regex+"':'"+newOutputFormat+"'}}";
    trfBuilder.stringFormat(resultingString);

    propertyBuilder.addAllTransformationsBuilders(trfBuilder);

    SchemaBase.Type baseType = XtraServerWebApiUtil.getWebApiType(td,
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
