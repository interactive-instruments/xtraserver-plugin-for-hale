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

package de.ii.xtraserver.hale.io.writer.handler;

import com.google.common.base.Strings;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.xml.namespace.QName;

import com.google.common.collect.ListMultimap;

import de.interactive_instruments.xtraserver.config.api.MappingValue;
import de.interactive_instruments.xtraserver.config.api.MappingValueBuilder;
import de.interactive_instruments.xtraserver.config.api.MappingValueBuilder.ValueClassification;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.functions.ClassificationMappingFunction;
import eu.esdihumboldt.hale.common.align.model.functions.ClassificationMappingUtil;
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.service.ServiceManager;
import eu.esdihumboldt.hale.common.lookup.LookupTable;

/**
 * Transforms the {@link ClassificationMappingFunction} to a
 * {@link MappingValue}
 * 
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
class ClassificationMappingHandler extends AbstractPropertyTransformationHandler {

	private final static String NIL_REASON = "@nilReason";
	private final static String NOT_CLASSIFIED_ACTION = "notClassifiedAction";
	private enum NotClassifiedActions {NULL, SOURCE, FIXED}

	ClassificationMappingHandler(final MappingContext mappingContext) {
		super(mappingContext);
	}

	/**
	 * @see TransformationHandler#handle(eu.esdihumboldt.hale.common.align.model.Cell)
	 */
	@Override
	public Optional<MappingValue> doHandle(final Cell propertyCell, final Property targetProperty) {

		final ValueClassification mappingValue;

		final List<QName> path = buildPath(targetProperty.getDefinition().getPropertyPath());

		if (path.get(path.size() - 1).getLocalPart().equals(NIL_REASON)) {
			mappingValue = new MappingValueBuilder().nil();
			mappingValue.qualifiedTargetPath(path.subList(0, path.size() - 1));
		}
		else {
			mappingValue = new MappingValueBuilder().classification();
			mappingValue.qualifiedTargetPath(path);
		}

		mappingValue.value(propertyName(XtraServerMappingUtils.getSourceProperty(propertyCell)
				.getDefinition().getPropertyPath()));

		final ListMultimap<String, ParameterValue> parameters = propertyCell
				.getTransformationParameters();

		// Assign DB codes and values from the lookup table
		final LookupTable lookup = ClassificationMappingUtil.getClassificationLookup(parameters,
				new ServiceManager(ServiceManager.SCOPE_PROJECT));
		if (lookup != null) {
			final Map<Value, Value> valueMap = lookup.asMap();
			final Iterator<Value> it = valueMap.keySet().iterator();
			while (it.hasNext()) {
				final Value sourceValue = it.next();
				final String targetValueStr = '\'' + mappingContext.resolveProjectVars(valueMap.get(sourceValue).as(String.class))
						+ '\'';
				String sourceValueStr = sourceValue.as(String.class);
				sourceValueStr = "true".equals(sourceValueStr) ? "t" : sourceValueStr;
				sourceValueStr = "false".equals(sourceValueStr) ? "f" : sourceValueStr;

				mappingValue.keyValue(sourceValueStr, targetValueStr);
			}

			if (parameters.containsKey(NOT_CLASSIFIED_ACTION) && !parameters.get(NOT_CLASSIFIED_ACTION).isEmpty()) {
				String action = parameters.get(NOT_CLASSIFIED_ACTION).get(0).getStringRepresentation();

				if (Objects.equals(NotClassifiedActions.NULL.name(), action.toUpperCase())) {
					mappingValue.defaultValue("NULL");
				} else if (Objects.equals(NotClassifiedActions.FIXED.name(), Strings.commonPrefix(NotClassifiedActions.FIXED.name(), action.toUpperCase()))) {
					final String targetValueStr = '\'' + mappingContext.resolveProjectVars(action.substring(action.indexOf(":")+1)) + '\'';
					mappingValue.defaultValue(targetValueStr);
				} else if (Objects.equals(NotClassifiedActions.SOURCE.name(), action.toUpperCase())) {
					//nothing to do
				}
			}
		}
		else {
			mappingValue.keyValue("NULL", "");
		}

		return Optional.of(mappingValue.build());
	}

}
