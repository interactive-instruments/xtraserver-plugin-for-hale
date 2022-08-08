/*
 * Copyright (c) 2022 interactive instruments GmbH
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

package de.ii.xtraserver.webapi.hale.io.writer;

import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * TBD
 */
public class XtraServerWebApiUtil {

    public static final String GML_NS_URI_PREFIX = "http://www.opengis.net/gml/";

    public static final String XSD_NS_URI_PREFIX = "http://www.w3.org/2001/XMLSchema";

    public static final Map<String,SchemaBase.Type> xsdToTypeMap = new HashMap<String,SchemaBase.Type>() { {

        put("anyURI", SchemaBase.Type.STRING);
        put("boolean", SchemaBase.Type.BOOLEAN);
        put("CodeWithAuthorityType", SchemaBase.Type.STRING);
        put("date", SchemaBase.Type.DATE);
        put("dateTime", SchemaBase.Type.DATETIME);
        put("decimal", SchemaBase.Type.FLOAT);
        put("double", SchemaBase.Type.FLOAT);
        put("ID", SchemaBase.Type.STRING);
        put("integer", SchemaBase.Type.INTEGER);
        put("string", SchemaBase.Type.STRING);
        put("UomIdentifier", SchemaBase.Type.STRING);

        // GML Geometry types
        put("CurvePropertyType", SchemaBase.Type.GEOMETRY);
        put("GeometryPropertyType", SchemaBase.Type.GEOMETRY);
        put("MultiCurvePropertyType", SchemaBase.Type.GEOMETRY);
        put("MultiGeometryPropertyType", SchemaBase.Type.GEOMETRY);
        put("MultiPointPropertyType", SchemaBase.Type.GEOMETRY);
        put("MultiSurfacePropertyType", SchemaBase.Type.GEOMETRY);
        put("PointPropertyType", SchemaBase.Type.GEOMETRY);
        put("GeometricPrimitivePropertyType", SchemaBase.Type.GEOMETRY);

        // GML Measure types
        put("AngleType", SchemaBase.Type.FLOAT);
        put("AreaType", SchemaBase.Type.FLOAT);
        put("GridLengthType", SchemaBase.Type.FLOAT);
        put("LengthType", SchemaBase.Type.FLOAT);
        put("MeasureType", SchemaBase.Type.FLOAT);
        put("ScaleType", SchemaBase.Type.FLOAT);
        put("SpeedType", SchemaBase.Type.FLOAT);
        put("TimeType", SchemaBase.Type.FLOAT);
        put("VolumeType", SchemaBase.Type.FLOAT);
    }

    };

    public static SchemaBase.Type getWebApiType(TypeDefinition td, IOReporter reporter) {

        // SchemaBase.Type
        QName tdName = td.getName();
        String localPart = tdName.getLocalPart();

        if(tdName.getNamespaceURI().startsWith(GML_NS_URI_PREFIX)) {

            if(xsdToTypeMap.containsKey(localPart)) {
                return xsdToTypeMap.get(localPart);
            } else {
                reporter.warn("DEV - Add type mapping for GML type "+localPart);
                return SchemaBase.Type.STRING;
            }

        } else if(tdName.getNamespaceURI().equalsIgnoreCase(XSD_NS_URI_PREFIX)) {

            // TODO same as above, merge later
            if(xsdToTypeMap.containsKey(localPart)) {
                return xsdToTypeMap.get(localPart);
            } else {
                reporter.warn("DEV - Add type mapping for XSD type "+localPart);
                return SchemaBase.Type.STRING;
            }
        } else {

            if(td.getSuperType() != null) {
                return getWebApiType(td.getSuperType(), reporter);
            } else {
                reporter.warn("DEV - Add type mapping for type "+localPart);
                return SchemaBase.Type.STRING;
            }
        }
    }

    /**
     * Parses a schema description, using separators defined for INSPIRE documentation items.
     *
     * @param description description as defined by the schema
     * @return map (can be empty but not null) with key: documentation facet identifier, in lower case
     * (e.g. 'name', 'definition', 'description'); value: the according documentation facet
     */
    public static Map<String, String> parseDescription(String description) {

        Map<String, String> result = new HashMap<>();

        if (StringUtils.isNotBlank(description)) {
            String separatorRegex = "\\s*--\\s*(\\w+)\\s*--\\s*(.+)(?!--)";
            Pattern inspireDocFacetPattern = Pattern.compile(separatorRegex);
            Matcher m = inspireDocFacetPattern.matcher(description.trim());
            while (m.find()) {
                result.put(m.group(1).toLowerCase(Locale.ENGLISH),m.group(2).trim());
            }
        }

        return result;
    }
}
