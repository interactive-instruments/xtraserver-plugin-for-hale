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
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.hale.io.writer.handler.CellParentWrapper;
import de.ii.xtraserver.webapi.hale.io.writer.XtraServerWebApiUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition;
import eu.esdihumboldt.hale.common.schema.model.ChildDefinition;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.Cardinality;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.Reference;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultGroupPropertyDefinition;
import eu.esdihumboldt.hale.io.xsd.constraint.XmlAppInfo;
import eu.esdihumboldt.hale.io.xsd.constraint.XmlAttributeFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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

//  protected List<QName> buildPath(final List<ChildContext> path) {
//    return buildPath(path, false);
//  }

//  protected List<QName> buildPathWithoutLast(final List<ChildContext> path) {
//    return buildPath(path, true);
//  }

//  protected List<QName> buildPath(final List<ChildContext> path, final boolean withoutLast) {
//    return path.stream().map(segment -> segment.getChild().asProperty())
//        .filter(Objects::nonNull).map(toPropertyNameWithAttributePrefix())
//        .limit(withoutLast ? path.size() - 1 : path.size()).collect(Collectors.toList());
//  }

  /**
   * Creates a string-representation of the full (property) path for the given property. The result
   * can, for example, be used in report messages.
   *
   * @param property the property for which to create the display path, typically the source or
   *                 target property of a property relationship
   * @return local names of the path properties, dot-concatenated
   */
  protected String fullDisplayPath(final Property property) {
    final List<ChildContext> path = property.getDefinition().getPropertyPath();
    return path.stream().map(segment -> segment.getChild().asProperty())
        .filter(Objects::nonNull).map(segment -> segment.getName().getLocalPart())
        .collect(Collectors.joining("."));
  }

//  private Function<PropertyDefinition, QName> toPropertyNameWithAttributePrefix() {
//    return property -> property.getConstraint(XmlAttributeFlag.class).isEnabled() ? new QName(
//        property.getName().getNamespaceURI(), "@" + property.getName().getLocalPart())
//        : property.getName();
//  }

  /**
   * @param path
   * @return The local name of the last child within the given path
   */
  protected static String propertyName(final List<ChildContext> path) {
    if (path == null || path.isEmpty()) {
      return "";
    }
    return path.get(path.size() - 1).getChild().getName().getLocalPart();
  }

  /**
   * @param p
   * @return The local name of the last child within the property path of the given property
   */
  protected static String propertyName(final Property p) {
    return propertyName(p.getDefinition().getPropertyPath());
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
  protected String getTargetFromSchema(final Property targetProperty) {
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

    String targetPropertyDisplayPath = fullDisplayPath(targetProperty);

    /*
     * Check if target property is a case to generally be ignored.
     */
    PropertyDefinition pdTgtLast = getLastPropertyDefinition(targetProperty);
    if (pdTgtLast.getConstraint(XmlAttributeFlag.class).isEnabled() &&
        pdTgtLast.getName().getLocalPart().equals("nilReason")) {
//      mappingContext.getReporter().info("Ignoring relationship for target property {0}.",targetPropertyDisplayPath);
      return null;
    }

    // Check for currently unsupported cases
    Map<String, List<PropertyTransformationHandler>> propertyHandlersByTargetPropertyPath =
        this.mappingContext.getPropertyHandlersByTargetPropertyPath();
    List<PropertyTransformationHandler> propertyHandlersForTargetPropertyPath;
    if (propertyHandlersByTargetPropertyPath.containsKey(targetPropertyDisplayPath)) {
      propertyHandlersForTargetPropertyPath = propertyHandlersByTargetPropertyPath.get(
          targetPropertyDisplayPath);
    } else {
      propertyHandlersForTargetPropertyPath = new ArrayList<>();
      propertyHandlersByTargetPropertyPath.put(targetPropertyDisplayPath,
          propertyHandlersForTargetPropertyPath);
    }

    if (this instanceof AssignHandler && propertyHandlersForTargetPropertyPath.stream().anyMatch(
        ph -> ph instanceof ClassificationMappingHandler)) {
      mappingContext.getReporter().warn(
          "Type relation for {0}: Unsupported case of 'choice' for target property {1} detected (Classification relation encoded, Assign relation ignored).",
          this.mappingContext.getFeatureTypeName(),
          targetPropertyDisplayPath);
      return null;
    } else if (this instanceof ClassificationMappingHandler
        && propertyHandlersForTargetPropertyPath.stream().anyMatch(
        ph -> ph instanceof AssignHandler)) {
      mappingContext.getReporter().warn(
          "Type relation for {0}: Unsupported case of 'choice' for target property {1} detected (Assign relation encoded, Classification relation ignored).",
          this.mappingContext.getFeatureTypeName(),
          targetPropertyDisplayPath);
      return null;
    } else if (this instanceof ClassificationMappingHandler
        && propertyHandlersForTargetPropertyPath.stream().anyMatch(
        ph -> ph instanceof ClassificationMappingHandler)) {
      mappingContext.getReporter().warn(
          "Type relation for {0}: Unsupported case of multiple Classification relations for target property {1} detected (only the first Classification relation is encoded).",
          this.mappingContext.getFeatureTypeName(),
          targetPropertyDisplayPath);
      return null;
    }

    // Apply the actual mapping
    final Optional<ImmutableFeatureSchema.Builder> optionalMappingValue = doHandle(propertyCell,
        targetProperty);

    // Keep track that this handler was applied in mappings for the target property
    propertyHandlersForTargetPropertyPath.add(this);

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

  protected ImmutableFeatureSchema.Builder buildPropertyPath(Property targetProperty,
      PropertyEntityDefinition sourceProperty) {

    ImmutableFeatureSchema.Builder typeBuilder = mappingContext.getFeatureBuilder();

    List<ChildContext> propertyPath = targetProperty.getDefinition().getPropertyPath();

    Map<String, Builder> propMap = typeBuilder.getPropertyMap();
    ImmutableFeatureSchema.Builder propertyBuilder = null;

    // keep track of real property path, to be used as variable name in label and description
    StringBuilder propertyPathTracker = new StringBuilder();
    propertyPathTracker.append(this.mappingContext.getFeatureTypeName().toLowerCase(Locale.ENGLISH))
        .append(".");

    ImmutableFeatureSchema.Builder firstObjectBuilder = null;

    for (int i = 0; i < propertyPath.size(); i++) {

      ChildDefinition cd = propertyPath.get(i).getChild();

      if(cd instanceof DefaultGroupPropertyDefinition) {
        // ignore choice group definition in the path
        continue;
      }

      PropertyDefinition pd = cd.asProperty();
      String pName = pd.getName().getLocalPart();

      boolean isClassRepresentingElement = Character.isUpperCase(pName.codePointAt(0));

      if (isClassRepresentingElement) {

        propertyBuilder.objectType(pName);

      } else if (isGmlUomProperty(pd) && i == propertyPath.size() - 1) {

        // ignore this property when building the path
        // return the builder for the second-to-last property instead
        // the current propertyBuilder (i.e. from the previous loop) represents that property (one before 'uom')
        // it can be used to assign the unit
        break;

      } else {

        propertyPathTracker.append(pName.toLowerCase(Locale.ENGLISH)).append(".");

        if (propMap.containsKey(pName)) {
          propertyBuilder = propMap.get(pName);
        } else {
          propertyBuilder = new ImmutableFeatureSchema.Builder();
          propMap.put(pName, propertyBuilder);

          propertyBuilder.name(pName);

          /* In the future, we could ignore setting label and description for other schema
          elements as well, e.g. XML attributes @codeList and @codeListValue.*/
          if (!(pd.getName().toString().equals("{http://www.w3.org/1999/xlink}title")
              || pd.getName().toString().equals("{http://www.w3.org/1999/xlink}href"))) {
            String label = labelValue(pd, propertyPathTracker.toString());
            propertyBuilder.label(label);
            String description = descriptionValue(pd, propertyPathTracker.toString());
            propertyBuilder.description(description);
          }

          /*
           * In the future, setting PRIMARY temporal properties may be done using explicit
           * flags contained in mapping notes.
           */
          if (pName.equalsIgnoreCase("beginLifespanVersion")) {
            propertyBuilder.role(Role.PRIMARY_INTERVAL_START);
          }
          if (pName.equalsIgnoreCase("endLifespanVersion")) {
            propertyBuilder.role(Role.PRIMARY_INTERVAL_END);
          }

          // cases in which second-to-last property was just created
          if (i < propertyPath.size() - 1) {

            // still within the path, create object / object array
            Cardinality card = pd.getConstraint(Cardinality.class);
            if (card != null && card.getMaxOccurs() != 1) {
              propertyBuilder.type(SchemaBase.Type.OBJECT_ARRAY);
            } else {
              propertyBuilder.type(SchemaBase.Type.OBJECT);
            }

            if (firstObjectBuilder == null) {
              firstObjectBuilder = propertyBuilder;
              this.mappingContext.addFirstObjectBuilderMapping(targetProperty, firstObjectBuilder);
//              Optional<String> joinSourcePath = this.mappingContext.computeJoinSourcePath(sourceProperty);
//              if (joinSourcePath.isPresent()) {
//                firstObjectBuilder.sourcePath(joinSourcePath.get());
//              }
            }
          }
        }

        // handle cases in which the second-to-last property may already have been created
        if (i < propertyPath.size() - 1) {

          // TODO How to identify when to set objectType = Link?
          // case: Association (appinfo/targetElement in schema -> getAssociationTarget / get TargetFromSchema)
          // case: Codelist URL
          ChildDefinition cdNext = propertyPath.get(i + 1).getChild();
          PropertyDefinition pdNext = cdNext.asProperty();
          // Workaround: using xlink:title as indicator for setting objectType=Link
          // because ldproxy did not create links for only href without title.
          // The workaround assumes that xlink:title is always accompanied by xlink:href.
          if (pdNext.getName().toString().equals("{http://www.w3.org/1999/xlink}title")) {
            propertyBuilder.objectType("Link");
          }
        }

        propMap = propertyBuilder.getPropertyMap();
      }
    }

    return propertyBuilder;
  }

  /**
   * @param pd           definition of a property (not representing an object type) that is in the
   *                     path of a property-relation target property
   * @param propertyPath path up to and including the given property, separated and ending with '.'
   * @return the value to use for the label within the provider configuration
   */
  private String labelValue(PropertyDefinition pd, String propertyPath) {

    Map<String, String> documentationFacets = XtraServerWebApiUtil.parseDescription(
        pd.getDescription());

    String result = "${" + propertyPath + "label:-";
    result += documentationFacets.getOrDefault("name", pd.getName().getLocalPart());
    result += "}";

    return result;
  }

  /**
   * @param pd           definition of a property (not representing an object type) that is in the
   *                     path of a property-relation target property
   * @param propertyPath path up to and including the given property, separated and ending with '.'
   * @return the value to use for the description within the provider configuration
   */
  private String descriptionValue(PropertyDefinition pd, String propertyPath) {

    Map<String, String> documentationFacets = XtraServerWebApiUtil.parseDescription(
        pd.getDescription());

    String result = "${" + propertyPath + "description:-";
    if (documentationFacets.containsKey("definition")) {
      result += documentationFacets.get("definition");
    }
    result += "}";

    return result;
  }

  /**
   * @param property
   * @return the last child (as a property) within the property path of the given property
   */
  protected PropertyDefinition getLastPropertyDefinition(Property property) {
    List<ChildContext> propertyPath = property.getDefinition().getPropertyPath();
    return propertyPath.get(propertyPath.size() - 1).getChild().asProperty();
  }

  /**
   * Turns a variable that uses the XtraServer variable syntax ('{$some.variable}') into a web api
   * variable name.
   *
   * @param value string that may be an XtraServer variable identifier
   * @return the reformatted variable name (if value was an XtraServer variable name), otherwise the
   * value as-is
   */
  protected String reformatVariable(final String value) {

    String result = value;

    if (value.startsWith("{$")) {

      result = value.substring(2, value.length() - 1);

      // TODO - FUTURE WORK: leave variable name as is, once they can be set via cfg.yml
      result = result.toUpperCase(Locale.ENGLISH);
      result = result.replaceAll("\\.", "_");

      result = "${" + result + "}";
    }

    return result;
  }

  protected boolean isMultiValuedPropertyPerSchemaDefinition(PropertyDefinition targetPd) {
    Cardinality card = targetPd.getConstraint(Cardinality.class);
    return card != null && card.getMaxOccurs() != 1;
  }

  protected boolean isGmlUomProperty(PropertyDefinition pd) {

    String propName = pd.getName().getLocalPart();
    TypeDefinition pdTypeDef = pd.getPropertyType();

    return propName.equals("uom")
        && pdTypeDef.getName().toString().equals("{http://www.opengis.net/gml/3.2}UomIdentifier");
  }
}

