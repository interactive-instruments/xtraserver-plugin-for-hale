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
import de.ii.xtraserver.hale.io.compatibility.XtraServerCompatibilityMode;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.webapi.hale.io.writer.XtraServerWebApiUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Entity;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
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

//	public String getPrimaryKey(final TypeDefinition definition) {
//		final PrimaryKey primaryKey = definition.getConstraint(PrimaryKey.class);
//		if (primaryKey == null || primaryKey.getPrimaryKeyPath() == null
//				|| primaryKey.getPrimaryKeyPath().isEmpty()) {
//			return null;
//		}
//		return primaryKey.getPrimaryKeyPath().iterator().next().getLocalPart();
//	}

	@Override
	public final ImmutableFeatureSchema.Builder handle(final Cell cell) {

		QName featureTypeName = XtraServerMappingUtils.getFeatureTypeName(cell);
		ImmutableFeatureSchema.Builder typeBuilder = mappingContext.addNextFeatureSchema(featureTypeName);

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

		String schemaDescription = targetType.getDefinition().getType().getDescription();
		String label = labelValue(schemaDescription, featureTypeName.getLocalPart());
		typeBuilder.label(label);
		String description = descriptionValue(schemaDescription, featureTypeName.getLocalPart());
		typeBuilder.description(description);

		return typeBuilder;
	}

	/**
	 * @param schemaDescription documentation as defined in the (target) type schema
	 * @param schemaTypeName name of the XML element that represents the (target) type
	 * @return the value to use for the label within the provider configuration
	 */
	private String labelValue(String schemaDescription, String schemaTypeName) {

		Map<String, String> documentationFacets = XtraServerWebApiUtil.parseDescription(schemaDescription);

		String result = "${"+schemaTypeName.toLowerCase(Locale.ENGLISH)+".label:-";
		result += documentationFacets.getOrDefault("name", schemaTypeName);
		result += "}";

		return result;
	}

	/**
	 * @param schemaDescription documentation as defined in the (target) type schema
	 * @param schemaTypeName name of the XML element that represents the (target) type
	 * @return the value to use for the label within the provider configuration
	 */
	private String descriptionValue(String schemaDescription, String schemaTypeName) {

		Map<String, String> documentationFacets = XtraServerWebApiUtil.parseDescription(schemaDescription);

		String result = "${"+schemaTypeName.toLowerCase(Locale.ENGLISH)+".description:-";
		if(documentationFacets.containsKey("definition")) {
			result += documentationFacets.get("definition");
		}
		result += "}";

		return result;
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
