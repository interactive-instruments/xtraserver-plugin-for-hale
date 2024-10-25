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

import static eu.esdihumboldt.hale.common.align.model.functions.JoinFunction.PARAMETER_JOIN;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraserver.hale.io.writer.handler.TransformationHandler;
import eu.esdihumboldt.hale.common.align.model.AlignmentUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.functions.JoinFunction;
import eu.esdihumboldt.hale.common.align.model.functions.join.JoinParameter;
import eu.esdihumboldt.hale.common.align.model.impl.TypeEntityDefinition;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transforms the {@link JoinFunction} to a {@link FeatureSchema}
 */
class JoinHandler extends AbstractTypeTransformationHandler {

	JoinHandler(final MappingContext mappingContext) {
		super(mappingContext);
	}

	/**
	 * @see TransformationHandler#handle(Cell, String)
	 */
	@Override
	public void doHandle(final Collection<? extends Entity> sourceTypes, final Entity targetType,
			final Cell typeCell) {

		List<ParameterValue> typeCellJoinParameters = typeCell.getTransformationParameters()
				.get(PARAMETER_JOIN);

		for (final ParameterValue transParam : typeCellJoinParameters) {

			final JoinParameter joinParameter = transParam.as(JoinParameter.class);
			final String validation = joinParameter.validate();
			if (validation != null) {
				throw new IllegalArgumentException("Join parameter invalid: " + validation);
			}

			// get the (ordered) list of type definitions that are defined for the Join
			// the first type definition defines the main table for the type mapping
			List<TypeEntityDefinition> joinParameterTypes = joinParameter.getTypes();
			TypeEntityDefinition mainTypeEntityDefinition = joinParameterTypes.get(0);
			this.mappingContext.setMainEntityDefinition(mainTypeEntityDefinition);

			// Extract table mapping infos from join conditions
			final List<JoinParameter.JoinCondition> sortedConditions = transformSortedConditions(joinParameter);
			for(JoinParameter.JoinCondition jc : sortedConditions) {
				String baseTableName = jc.baseProperty.getType().getName().getLocalPart();
				String baseTableJoinField = jc.baseProperty.getLastPathElement().getChild().getName().getLocalPart();
				String joinTableName = jc.joinProperty.getType().getName().getLocalPart();
				String joinTableJoinField = jc.joinProperty.getLastPathElement().getChild().getName().getLocalPart();

				JoinInfo ji = new JoinInfo(baseTableName,baseTableJoinField,joinTableName,joinTableJoinField);
				this.mappingContext.addJoinInfo(ji);
			}

			// TODO - FUTURE WORK - multiple joins for same target type
		}
	}

	private List<JoinParameter.JoinCondition> transformSortedConditions(final JoinParameter joinParameter) {

		return joinParameter.getConditions().stream().sorted((o1, o2) -> {
			TypeEntityDefinition o1Type = AlignmentUtil.getTypeEntity(o1.joinProperty);
			TypeEntityDefinition o2Type = AlignmentUtil.getTypeEntity(o2.joinProperty);
			return joinParameter.getTypes().indexOf(o1Type)
					- joinParameter.getTypes().indexOf(o2Type);
		}).collect(Collectors.toList());
	}

}
