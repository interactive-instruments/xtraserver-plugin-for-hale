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

import de.ii.xtraserver.hale.io.writer.handler.AbstractTransformationHandlerFactory;
import de.ii.xtraserver.hale.io.writer.handler.AssignHandler;
import de.ii.xtraserver.hale.io.writer.handler.ClassificationMappingHandler;
import de.ii.xtraserver.hale.io.writer.handler.CustomFunctionAdvToGeographicalNameSimple;
import de.ii.xtraserver.hale.io.writer.handler.CustomFunctionAdvToIdentifier;
import de.ii.xtraserver.hale.io.writer.handler.CustomFunctionAdvToLocalId;
import de.ii.xtraserver.hale.io.writer.handler.CustomFunctionAdvToNamespace;
import de.ii.xtraserver.hale.io.writer.handler.CustomFunctionAdvToUCUM;
import de.ii.xtraserver.hale.io.writer.handler.FormattedStringHandler;
import de.ii.xtraserver.hale.io.writer.handler.MathematicalExpressionHandler;
import de.ii.xtraserver.hale.io.writer.handler.RegexHandler;
import de.ii.xtraserver.hale.io.writer.handler.RenameHandler;
import de.ii.xtraserver.hale.io.writer.handler.SqlExpressionHandler;
import eu.esdihumboldt.cst.functions.numeric.MathematicalExpressionFunction;
import eu.esdihumboldt.cst.functions.string.RegexAnalysisFunction;
import eu.esdihumboldt.hale.common.align.model.functions.AssignFunction;
import eu.esdihumboldt.hale.common.align.model.functions.ClassificationMappingFunction;
import eu.esdihumboldt.hale.common.align.model.functions.FormattedStringFunction;
import eu.esdihumboldt.hale.common.align.model.functions.RenameFunction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Factory for creating Property Transformation Handlers
 * 
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class PropertyTransformationHandlerFactory
		extends AbstractTransformationHandlerFactory<PropertyTransformationHandler> {

	private final static String SQL_EXPRESSION_ID = "eu.esdihumboldt.hale.io.xtraserver.sqlExpression";

	private final static String[] supportedTypes = { RenameFunction.ID,
			MathematicalExpressionFunction.ID, AssignFunction.ID, AssignFunction.ID_BOUND,
			FormattedStringFunction.ID, RegexAnalysisFunction.ID, ClassificationMappingFunction.ID,
			CustomFunctionAdvToGeographicalNameSimple.FUNCTION_ID,
			CustomFunctionAdvToIdentifier.FUNCTION_ID, CustomFunctionAdvToLocalId.FUNCTION_ID,
			CustomFunctionAdvToNamespace.FUNCTION_ID, CustomFunctionAdvToUCUM.FUNCTION_ID,
			SQL_EXPRESSION_ID };
	final static Set<String> SUPPORTED_TYPES = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList(supportedTypes)));

	@SuppressWarnings("serial")
	PropertyTransformationHandlerFactory(final MappingContext mappingContext) {
		super(mappingContext, new HashMap<String, PropertyTransformationHandler>() {

			{
				int i = 0;
				put(supportedTypes[i++], new RenameHandler(mappingContext));
				put(supportedTypes[i++], new MathematicalExpressionHandler(mappingContext));
				put(supportedTypes[i++], new AssignHandler(mappingContext));
				put(supportedTypes[i++], new AssignHandler(mappingContext));
				put(supportedTypes[i++], new FormattedStringHandler(mappingContext));
				put(supportedTypes[i++], new RegexHandler(mappingContext));
				put(supportedTypes[i++], new ClassificationMappingHandler(mappingContext));
				put(supportedTypes[i++],
						new CustomFunctionAdvToGeographicalNameSimple(mappingContext));
				put(supportedTypes[i++], new CustomFunctionAdvToIdentifier(mappingContext));
				put(supportedTypes[i++], new CustomFunctionAdvToLocalId(mappingContext));
				put(supportedTypes[i++], new CustomFunctionAdvToNamespace(mappingContext));
				put(supportedTypes[i++], new CustomFunctionAdvToUCUM(mappingContext));
				put(supportedTypes[i++], new SqlExpressionHandler(mappingContext));
			}
		});
	}

}
