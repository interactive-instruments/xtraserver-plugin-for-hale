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

import com.google.common.base.Strings;
import com.google.common.collect.ListMultimap;
import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.codelists.domain.CodelistData.ImportType;
import de.ii.xtraplatform.codelists.domain.ImmutableCodelistData;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.hale.io.writer.handler.TransformationHandler;
import de.ii.xtraserver.webapi.hale.io.writer.XtraServerWebApiUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.functions.ClassificationMappingFunction;
import eu.esdihumboldt.hale.common.align.model.functions.ClassificationMappingUtil;
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.service.ServiceManager;
import eu.esdihumboldt.hale.common.lookup.LookupTable;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Transforms the {@link ClassificationMappingFunction} to a {@link FeatureSchema}
 */
class ClassificationMappingHandler extends AbstractPropertyTransformationHandler {

  private final static String NOT_CLASSIFIED_ACTION = "notClassifiedAction";

  private final static String NULLIFY_VALUE = "NULL_FALLBACK_VALUE";

  private enum NotClassifiedActions {NULL, SOURCE, FIXED}

  ClassificationMappingHandler(final MappingContext mappingContext) {
    super(mappingContext);
  }

  /**
   * @see TransformationHandler#handle(Cell)
   */
  @Override
  public Optional<ImmutableFeatureSchema.Builder> doHandle(final Cell propertyCell,
      final Property targetProperty) {

    final ListMultimap<String, ParameterValue> parameters = propertyCell
        .getTransformationParameters();

    // Assign DB codes and values from the lookup table
    final LookupTable lookup = ClassificationMappingUtil.getClassificationLookup(parameters,
        new ServiceManager(ServiceManager.SCOPE_PROJECT));

    if (lookup != null) {

      final SortedMap<String, String> codeMappings = new TreeMap<>();
      final Map<Value, Value> valueMap = lookup.asMap();

      for (Value sourceValue : valueMap.keySet()) {
        final String targetValueStr = mappingContext.resolveProjectVars(
            valueMap.get(sourceValue).as(String.class));
        String sourceValueStr = sourceValue.as(String.class);
        // TODO - do we need any specific mapping here?
//				sourceValueStr = "true".equals(sourceValueStr) ? "t" : sourceValueStr;
//				sourceValueStr = "false".equals(sourceValueStr) ? "f" : sourceValueStr;
        codeMappings.put(sourceValueStr, targetValueStr);
      }

      Optional<String> fallbackValue = Optional.empty();
      boolean nullifyFallback = false;
      if (parameters.containsKey(NOT_CLASSIFIED_ACTION) && !parameters.get(NOT_CLASSIFIED_ACTION)
          .isEmpty()) {
        String action = parameters.get(NOT_CLASSIFIED_ACTION).get(0).getStringRepresentation();

        if (Objects.equals(
            ClassificationMappingHandler.NotClassifiedActions.NULL.name(), action.toUpperCase())) {
          nullifyFallback = true;
          fallbackValue = Optional.of(NULLIFY_VALUE);

        } else if (Objects.equals(
            ClassificationMappingHandler.NotClassifiedActions.FIXED.name(), Strings.commonPrefix(
                ClassificationMappingHandler.NotClassifiedActions.FIXED.name(),
                action.toUpperCase()))) {

          final String targetValueStr =
              mappingContext.resolveProjectVars(action.substring(action.indexOf(":") + 1));
          fallbackValue = Optional.of(targetValueStr);

        } else if (Objects.equals(
            ClassificationMappingHandler.NotClassifiedActions.SOURCE.name(),
            action.toUpperCase())) {
          // the default value for 'fallback' is the actual source value, so nothing to do here
        }
      }

      PropertyDefinition targetPd = getLastPropertyDefinition(targetProperty);
      String targetPropertyName = targetPd.getName().getLocalPart();
      TypeDefinition td = targetPd.getPropertyType();

      Property sourceProperty = XtraServerMappingUtils.getSourceProperty(propertyCell);
      String sourcePropertyName = sourceProperty.getDefinition().getDefinition().getName()
          .getLocalPart();

      String mainTableNameForCodelist = mappingContext.getMainTableName();
      String codelistId =
          mainTableNameForCodelist + "." + sourcePropertyName + "-to-" + targetPropertyName
              + "-classification";

      String codelistLabel = codelistId.replaceAll("[.-]", " ");

//      System.out.println("---");
//      System.out.println("id: " + codelistId);
//      System.out.println("label: " + codelistLabel);
//      System.out.println("sourceType: TEMPLATES");
//      System.out.println("entries:");
//      codeMappings.forEach((key, value) -> System.out.println("   " + key + ": " + value));
//      fallbackValue.ifPresent(s -> System.out.println("fallback: " + s));

      ImmutableFeatureSchema.Builder propertyBuilder = buildPropertyPath(targetProperty,
          sourceProperty.getDefinition());

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

      ImmutablePropertyTransformation.Builder codelistTrfBuilder = new ImmutablePropertyTransformation.Builder();
      codelistTrfBuilder.codelist(codelistId);
      propertyBuilder.addAllTransformationsBuilders(codelistTrfBuilder);

      if (nullifyFallback) {
        ImmutablePropertyTransformation.Builder nullifyTrfBuilder = new ImmutablePropertyTransformation.Builder();
        nullifyTrfBuilder.addNullify(NULLIFY_VALUE);
        propertyBuilder.addAllTransformationsBuilders(nullifyTrfBuilder);
      }

      SchemaBase.Type baseType = XtraServerWebApiUtil.getWebApiType(td,
          this.mappingContext.getReporter());
      if (isMultiValuedPropertyPerSchemaDefinition(targetPd)) {
        propertyBuilder.type(Type.VALUE_ARRAY);
        propertyBuilder.valueType(baseType);
      } else {
        propertyBuilder.type(baseType);
      }

      // create actual codelist entity with ldproxyCfg
      LdproxyCfg ldproxyCfg = mappingContext.getLdproxyCfg();
      ImmutableCodelistData.Builder clBuilder = ldproxyCfg.builder().entity().codelist();
      clBuilder.id(codelistId).label(codelistLabel).sourceType(ImportType.TEMPLATES);
      clBuilder.entries(codeMappings);
      clBuilder.fallback(fallbackValue);

      // add codelist entity to mapping context
      this.mappingContext.addCodeList(clBuilder.build());

      return Optional.of(propertyBuilder);
    }

    return Optional.empty();
  }

}
