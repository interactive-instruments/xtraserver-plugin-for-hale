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
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.hale.io.writer.handler.TransformationHandler;
import de.ii.xtraserver.webapi.hale.io.writer.XtraServerWebApiUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.functions.AssignFunction;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import java.util.List;
import java.util.Optional;

/**
 * Transforms the {@link AssignFunction} to a {@link FeatureSchema}
 */
class AssignHandler extends AbstractPropertyTransformationHandler {

//  private final static String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
//  private final static String NIL_REASON = "@nilReason";

  AssignHandler(final MappingContext mappingContext) {
    super(mappingContext);
  }

  /**
   * @see TransformationHandler#handle(Cell)
   */
  @Override
  public Optional<ImmutableFeatureSchema.Builder> doHandle(final Cell propertyCell,
      final Property targetProperty) {

    PropertyDefinition pdTgtLast = getLastPropertyDefinition(targetProperty);
    String lastTgtPropName = pdTgtLast.getName().getLocalPart();
    TypeDefinition tgtLastTypeDef = pdTgtLast.getPropertyType();

    if (propertyCell.getTransformationIdentifier().equals(AssignFunction.ID_BOUND)
        && !isGmlUomProperty(pdTgtLast)) {
      // TODO - FUTURE WORK (especially the case of of Assign (Bound) with additional Assign as fallback -> choice)
      return Optional.empty();
    }

    // Assign constant value from parameters
    final ListMultimap<String, ParameterValue> parameters = propertyCell
        .getTransformationParameters();
    final List<ParameterValue> valueParams = parameters.get(PARAMETER_VALUE);
    String value = valueParams.get(0).getStringRepresentation();
    value = mappingContext.resolveProjectVars(value);
    value = reformatVariable(value);

    /*
     * Check if the assignment is a case to generally be ignored.
     */
    // TODO - check that codeSpace is an XML attribute (like for nilReason)?
    if (lastTgtPropName.equals("codeSpace") && value.equals("http://inspire.ec.europa.eu/ids")) {
//			mappingContext.getReporter().info("Ignoring Assign relationship for target property 'codeSpace' with value 'http://inspire.ec.europa.eu/ids'.");
      return Optional.empty();
    }

    // handle cases of ISO 19139 encoded, code list valued property elements
    if (tgtLastTypeDef.getName().toString()
        .equalsIgnoreCase("{http://www.isotc211.org/2005/gco}CodeListValue_Type")) {
      // The textual value of an ISO 19139 encoded, code list valued property element.
      // In AdV-INSPIRE-alignments typically a copy of the value for @codeListValue.
      // We ignore this property relation. If we did not, the type of the property that will
      // itself have properties (codeList and codeListValue) would be changed from OBJECT to STRING.
      return Optional.empty();
    }

    // property creation only after ignore-checks (see above):
    ImmutableFeatureSchema.Builder propertyBuilder = buildPropertyPath(targetProperty,
        null);

    if (isGmlUomProperty(pdTgtLast)) {

      propertyBuilder.unit(value);

    } else {

      propertyBuilder.constantValue(value);

      SchemaBase.Type baseType = XtraServerWebApiUtil.getWebApiType(pdTgtLast.getPropertyType(),
          this.mappingContext.getReporter());
      if (isMultiValuedPropertyPerSchemaDefinition(pdTgtLast)) {
        propertyBuilder.type(Type.VALUE_ARRAY);
        propertyBuilder.valueType(baseType);
      } else {
        propertyBuilder.type(baseType);
      }
    }

    return Optional.of(propertyBuilder);
  }
}
