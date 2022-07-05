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
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * TBD
 */
public class XtraServerWebApiTypeUtil {

    public static final String GML_NS_URI_PREFIX = "http://www.opengis.net/gml/";

    public static final String XSD_NS_URI_PREFIX = "http://www.w3.org/2001/XMLSchema";

    public static final Map<String,SchemaBase.Type> xsdToTypeMap = new HashMap<String,SchemaBase.Type>() { {
        put("anyURI", SchemaBase.Type.STRING);
        put("boolean", SchemaBase.Type.BOOLEAN);
        put("date", SchemaBase.Type.DATE);
        put("dateTime", SchemaBase.Type.DATETIME);
        put("decimal", SchemaBase.Type.FLOAT);
        put("double", SchemaBase.Type.FLOAT);
        put("PointPropertyType", SchemaBase.Type.GEOMETRY);
        put("integer", SchemaBase.Type.INTEGER);
        put("string", SchemaBase.Type.STRING);

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
}
