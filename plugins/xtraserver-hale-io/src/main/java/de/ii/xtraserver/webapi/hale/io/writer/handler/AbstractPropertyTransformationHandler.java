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
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.hale.io.writer.handler.CellParentWrapper;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.schema.model.ChildDefinition;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.Reference;
import eu.esdihumboldt.hale.io.xsd.constraint.XmlAppInfo;
import eu.esdihumboldt.hale.io.xsd.constraint.XmlAttributeFlag;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.w3c.dom.Node;

/**
 * Abstract Property Transformation Handler
 */
abstract class AbstractPropertyTransformationHandler implements PropertyTransformationHandler {

  protected final MappingContext mappingContext;

  protected AbstractPropertyTransformationHandler(final MappingContext mappingContext) {
    this.mappingContext = mappingContext;
  }

  protected List<QName> buildPath(final List<ChildContext> path) {
    return buildPath(path, false);
  }

  protected List<QName> buildPathWithoutLast(final List<ChildContext> path) {
    return buildPath(path, true);
  }

  protected List<QName> buildPath(final List<ChildContext> path, final boolean withoutLast) {
    return path.stream().map(segment -> segment.getChild().asProperty())
        .filter(Objects::nonNull).map(toPropertyNameWithAttributePrefix())
        .limit(withoutLast ? path.size() - 1 : path.size()).collect(Collectors.toList());
  }

  private Function<PropertyDefinition, QName> toPropertyNameWithAttributePrefix() {
    return property -> property.getConstraint(XmlAttributeFlag.class).isEnabled() ? new QName(
        property.getName().getNamespaceURI(), "@" + property.getName().getLocalPart())
        : property.getName();
  }

  protected static String propertyName(final List<ChildContext> path) {
    if (path == null || path.isEmpty()) {
      return "";
    }
    return path.get(path.size() - 1).getChild().getName().getLocalPart();
  }

  protected static String getSingleProperty(final ListMultimap<String, ParameterValue> parameters,
      final String name) {
    if (parameters != null) {
      final List<ParameterValue> parameterValues = parameters.get(name);
      if (parameterValues != null && !parameterValues.isEmpty()) {
        return parameterValues.get(0).as(String.class);
      }
    }
    return null;
  }

  /**
   * Check if the property cell is a reference and if yes return the association target that is
   * found in the schema.
   *
   * @param propertyCell Property cell
   * @return association target
   */
  protected Optional<String> getAssociationTarget(final Cell propertyCell) {
    final Property targetProperty = XtraServerMappingUtils.getTargetProperty(propertyCell);
    if (targetProperty.getDefinition().getDefinition().getConstraint(Reference.class)
        .isReference()) {
      final String associationTargetRef = getTargetFromSchema(targetProperty);
      if (associationTargetRef != null) {
        return Optional.of(associationTargetRef);
      }
    }

    return Optional.empty();
  }

  /**
   * Find the association target from the AppInfo annotation in the XSD
   *
   * @param targetProperty target property to analyze
   * @return association target as String
   */
  private String getTargetFromSchema(final Property targetProperty) {
    if (targetProperty.getDefinition().getPropertyPath().isEmpty()) {
      return null;
    }
    int refElemIndex = Math.max(0, targetProperty.getDefinition().getPropertyPath().size() - 2);
    final ChildDefinition<?> firstChild = targetProperty.getDefinition().getPropertyPath()
        .get(refElemIndex).getChild();
    if (!(firstChild instanceof PropertyDefinition)) {
      return null;
    }
    final XmlAppInfo appInfoAnnotation = ((PropertyDefinition) firstChild)
        .getConstraint(XmlAppInfo.class);

    for (final XmlSchemaAppInfo appInfo : appInfoAnnotation.getAppInfos()) {
      for (int i = 0; i < appInfo.getMarkup().getLength(); i++) {
        final Node item = appInfo.getMarkup().item(i);
        if ("targetElement".equals(item.getNodeName())) {
          final String target = item.getTextContent();
          return target;
        }
      }
    }
    return null;
  }

  /**
   * NOTE: The feature schema builder returned by this implementation is the one on the leaf level
   * in the generated JSON tree (of the provider configuration). Returning the builder for the
   * topmost child property within the feature type definition would defeat the purpose (since many
   * mappings could make use of it).
   */
  @Override
  public final ImmutableFeatureSchema.Builder handle(final Cell propertyCell) {

    final Property targetProperty = XtraServerMappingUtils.getTargetProperty(propertyCell);
    final Property sourceProperty = XtraServerMappingUtils.getSourceProperty(propertyCell);

    if (targetProperty == null || (sourceProperty == null && !((this instanceof AssignHandler)
        || (this instanceof CustomFunctionAdvToNamespace)
        || (this instanceof SqlExpressionHandler)
        || (this instanceof FormattedStringHandler)))) {
      CellParentWrapper cellParentWrapper = (CellParentWrapper) propertyCell;
      mappingContext.getReporter().warn(
          "Cell could not be exported, source or target property is not set (Table: {0}, Source: {1}, Target: {2})",
          cellParentWrapper.getTableName(), sourceProperty, targetProperty);
      return null;
    }

    final Optional<ImmutableFeatureSchema.Builder> optionalMappingValue = doHandle(propertyCell,
        targetProperty);

    final String tableName = ((CellParentWrapper) propertyCell).getTableName();

    /* TODO: if needed - e.g. for some postprocessing (like a transformation) - we can
     * enable this again (being aware with which feature schema builder [for the leaf property]
     * the value mapping has been created).
     */
//		optionalMappingValue.ifPresent(mappingValue -> {
//			mappingContext.addValueMappingForTable(targetProperty, mappingValue, tableName);
//		});

    return optionalMappingValue.orElse(null);
  }

  protected abstract Optional<ImmutableFeatureSchema.Builder> doHandle(final Cell propertyCell,
      final Property targetProperty);

  protected ImmutableFeatureSchema.Builder buildPropertyPath(Property targetProperty) {

    ImmutableFeatureSchema.Builder typeBuilder = mappingContext.getFeatureBuilder();

    List<ChildContext> propertyPath = targetProperty.getDefinition().getPropertyPath();

    Map<String, Builder> propMap = typeBuilder.getPropertyMap();
    ImmutableFeatureSchema.Builder propertyBuilder = null;

    for (int i = 0; i < propertyPath.size(); i++) {

      ChildDefinition cd = propertyPath.get(i).getChild();

      PropertyDefinition pd = cd.asProperty();
      String pName = pd.getName().getLocalPart();

      boolean isClassRepresentingElement = Character.isUpperCase(pName.codePointAt(0));

      if (isClassRepresentingElement) {

        propertyBuilder.objectType(pName);

      } else {

        if (propMap.containsKey(pName)) {
          propertyBuilder = propMap.get(pName);
        } else {
          propertyBuilder = new ImmutableFeatureSchema.Builder();
          propMap.put(pName, propertyBuilder);

          propertyBuilder.name(pName);

          if (i < propertyPath.size() - 1) {
            // still within the path, create object
            propertyBuilder.type(SchemaBase.Type.OBJECT);

            // TODO How to identify when to set objectType = LINK?
            ChildDefinition cdNext = propertyPath.get(i + 1).getChild();
            PropertyDefinition pdNext = cdNext.asProperty();
            if (pdNext.getName().toString().equals("{http://www.w3.org/1999/xlink}href")) {
              propertyBuilder.objectType("LINK");
            }
          }
        }

        propMap = propertyBuilder.getPropertyMap();
      }
    }

    return propertyBuilder;
  }

  protected PropertyDefinition getLastPropertyDefinition(Property property) {
    List<ChildContext> propertyPath = property.getDefinition().getPropertyPath();
    return propertyPath.get(propertyPath.size() - 1).getChild().asProperty();
  }
}

