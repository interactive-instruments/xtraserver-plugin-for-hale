/*
 * Copyright (c) 2018 wetransform GmbH
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
 *     wetransform GmbH <http://www.wetransform.to>
 */

package de.ii.xtraserver.hale.io.writer;

import com.google.common.collect.ListMultimap;

import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.io.xsd.constraint.XmlElements;
import javax.xml.namespace.QName;

/**
 * Utility methods for mapping generation.
 * 
 * @author Stefano Costa, GeoSolutions
 */
public class XtraServerMappingUtils {

	/**
	 * Return the first source {@link Entity}, which is assumed to be a
	 * {@link Property}.
	 * 
	 * @param propertyCell the property cell
	 * @return the target {@link Property}
	 */
	public static Property getSourceProperty(Cell propertyCell) {
		ListMultimap<String, ? extends Entity> sourceEntities = propertyCell.getSource();
		if (sourceEntities != null && !sourceEntities.isEmpty()) {
			return (Property) sourceEntities.values().iterator().next();
		}

		return null;
	}

	/**
	 * Return the first target {@link Entity}, which is assumed to be a
	 * {@link Property}.
	 * 
	 * @param propertyCell the property cell
	 * @return the target {@link Property}
	 */
	public static Property getTargetProperty(Cell propertyCell) {
		ListMultimap<String, ? extends Entity> targetEntities = propertyCell.getTarget();
		if (targetEntities != null && !targetEntities.isEmpty()) {
			return (Property) targetEntities.values().iterator().next();
		}

		return null;
	}

	public static QName getFeatureTypeName(final Cell typeCell) {

		final ListMultimap<String, ? extends Entity> targetEntities = typeCell.getTarget();
		if (targetEntities == null || targetEntities.size() == 0) {
			throw new IllegalStateException("No target type has been specified.");
		}
		final Entity targetType = targetEntities.values().iterator().next();
		final TypeDefinition targetTypeDefinition = targetType.getDefinition().getType();
		final XmlElements constraints = targetTypeDefinition.getConstraint(XmlElements.class);
		if (constraints == null || constraints.getElements().size() == 0) {
			throw new IllegalStateException("No constraint has been specified.");
		}
		else if (constraints.getElements().size() > 1) {
			throw new IllegalStateException("More than one constraint has been specified.");
		}
		return constraints.getElements().iterator().next().getName();
	}

}
