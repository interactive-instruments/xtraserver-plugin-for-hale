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
import de.ii.xtraserver.hale.io.compatibility.XtraServerCompatibilityMode;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.PrimaryKey;
import eu.esdihumboldt.hale.io.xsd.constraint.XmlElements;
import java.util.Collection;
import javax.xml.namespace.QName;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

/**
 * Abstract Type Transformation Handler
 */
public abstract class AbstractTypeTransformationHandler implements TypeTransformationHandler {

	protected final MappingContext mappingContext;

	protected AbstractTypeTransformationHandler(final MappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}

	protected QName getFeatureTypeName(final Cell cell) {

		final ListMultimap<String, ? extends Entity> targetEntities = cell.getTarget();
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

	protected String getPrimaryKey(final TypeDefinition definition) {
		final PrimaryKey primaryKey = definition.getConstraint(PrimaryKey.class);
		if (primaryKey == null || primaryKey.getPrimaryKeyPath() == null
				|| primaryKey.getPrimaryKeyPath().isEmpty()) {
			return null;
		}
		return primaryKey.getPrimaryKeyPath().iterator().next().getLocalPart();
	}

	@Override
	public final FeatureSchema.Builder handle(final Cell cell) {

		mappingContext.addNextFeatureSchema(getFeatureTypeName(cell));

		final ListMultimap<String, ? extends Entity> sourceEntities = cell.getSource();
		if (sourceEntities == null || sourceEntities.size() == 0) {
			throw new IllegalStateException("No source type has been specified.");
		}
		if (XtraServerCompatibilityMode.hasFilters(cell.getSource())) {
			mappingContext.getReporter().warn(
					"Filters are not supported and are ignored during type transformation of Feature Type \"{0}\"",
					mappingContext.getFeatureTypeName());
		}

		final ListMultimap<String, ? extends Entity> targetEntities = cell.getTarget();
		if (targetEntities == null || targetEntities.size() == 0) {
			throw new IllegalStateException("No target type has been specified.");
		}
		final Entity targetType = targetEntities.values().iterator().next();
		final Collection<? extends Entity> sourceTypes = sourceEntities.values();
		doHandle(sourceTypes, targetType, cell);

		return null;
	}

	public abstract void doHandle(final Collection<? extends Entity> sourceTypes,
			final Entity targetType, final Cell typeCell);

	private class ResolvePropertyNamesFilterVisitor extends DuplicatingFilterVisitor {

		final FilterFactory2 filterFactory = new FilterFactoryImpl();
		final String tableName;

		ResolvePropertyNamesFilterVisitor(String tableName) {
			this.tableName = tableName;
		}

		@Override
		public Object visit(PropertyName expression, Object extraData) {
			return filterFactory.property(tableName + expression.getPropertyName());
		}
	}
}
