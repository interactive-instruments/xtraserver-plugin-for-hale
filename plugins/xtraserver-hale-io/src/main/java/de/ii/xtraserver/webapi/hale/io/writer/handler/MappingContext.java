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

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData;
import de.interactive_instruments.xtraserver.config.api.MappingTableBuilder;
import de.interactive_instruments.xtraserver.config.api.MappingValue;
import de.interactive_instruments.xtraserver.config.api.XtraServerMappingBuilder;
import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.io.project.ProjectInfo;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.schema.model.Schema;
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;

/**
 * The mapping context provides access to the {@link Alignment} and holds all {@link
 * FeatureSchema}s.
 */
public final class MappingContext {

  static final String PROPERTY_ADV_MODELLART = "ADV_MODELLART";
  static final String PROPERTY_INSPIRE_NAMESPACE = "INSPIRE_NAMESPACE";
  static final ImmutableList<QName> EMPTY_PATH = ImmutableList.of(new QName("__EMPTY__"));

  private final Alignment alignment;
  private final Map<String, Value> transformationProperties;
  private static final Pattern projectVarPattern = Pattern.compile("\\{\\{project:([^}]+)\\}\\}");

  private final Map<String, FeatureSchema.Builder> featureTypeMappings = new LinkedHashMap<>();
  private FeatureSchema.Builder currentFeatureTypeMapping;
  private String currentFeatureTypeMappingName;
  private final Map<String, FeatureSchema.Builder> currentMappingTables = new LinkedHashMap<>();
  private final Set<String> missingAssociationTargets = new TreeSet<String>();
  private final URI applicationSchemaUri;
  private final ProjectInfo projectInfo;
  private final URI projectLocation;
  private final IOReporter reporter;
  private final LdproxyCfg ldproxyCfg;

  /**
   * Constructor Only the first schema is used
   *
   * @param alignment                the Alignment with all cells
   * @param schemaspace              the target schema
   * @param transformationProperties Properties used in transformations
   * @param projectInfo              project info
   * @param projectLocation          project file
   * @param reporter                 reporter
   * @param ldproxyCfg               ldproxyCfg
   */
  public MappingContext(
      final Alignment alignment,
      final SchemaSpace schemaspace,
      final Map<String, Value> transformationProperties,
      final ProjectInfo projectInfo,
      final URI projectLocation,
      final IOReporter reporter, LdproxyCfg ldproxyCfg) {
    this.alignment = Objects.requireNonNull(alignment);
    this.transformationProperties = Objects.requireNonNull(transformationProperties);

    final Iterator<? extends Schema> it =
        Objects.requireNonNull(schemaspace, "Schemaspace not provided").getSchemas().iterator();
    if (!it.hasNext()) {
      throw new IllegalArgumentException("Schemaspace does not contain a schema");
    }
    final Schema schema = it.next();
    this.applicationSchemaUri = schema.getLocation();
    this.projectInfo = projectInfo;
    this.projectLocation = projectLocation;
    this.reporter = reporter;
    this.ldproxyCfg = ldproxyCfg;
  }

  /**
   * Add a new FeatureSchema to the mapping context
   *
   * @param featureTypeName feature type name
   * @return the same FeatureSchema for chaining method calls
   */
  FeatureSchema.Builder addNextFeatureSchema(final QName featureTypeName) {
    buildAndClearCurrentTables();

    final String key =
        Objects.requireNonNull(featureTypeName, "Feature Type name is null").toString();
    currentFeatureTypeMapping = featureTypeMappings.get(key);
    if (currentFeatureTypeMapping == null) {
      currentFeatureTypeMapping =
          new Builder().name(featureTypeName.getLocalPart()).type(Type.OBJECT);
      featureTypeMappings.put(key, currentFeatureTypeMapping);
    }

    this.currentFeatureTypeMappingName = featureTypeName.getLocalPart();

    return currentFeatureTypeMapping;
  }

  void addCurrentMappingTable(final String tableName, final FeatureSchema.Builder mappingTable) {
    this.currentMappingTables.put(tableName, mappingTable);
  }

  Collection<FeatureSchema.Builder> getCurrentMappingTables() {
    return this.currentMappingTables.values();
  }

  void buildAndClearCurrentTables() {
    if (this.currentFeatureTypeMapping == null) {
      return;
    }

    this.currentMappingTables.clear();
  }

  /**
   * Returns the name of the currently processed Feature Type Mapping
   *
   * @return Feature Type Mapping name
   */
  public String getFeatureTypeName() {
    return currentFeatureTypeMappingName;
  }

  /**
   * Return all property paths for which no association target could be found in the schema.
   *
   * @return list of properties with missing association targets
   */
  public Set<String> getMissingAssociationTargets() {
    return this.missingAssociationTargets;
  }

  void addMissingAssociationTarget(final String associationTarget) {
    this.missingAssociationTargets.add(associationTarget);
  }

  Value getTransformationProperty(final String name) {
    final Value val = this.transformationProperties.get(name);
    if (val != null) {
      return val;
    }
    return Value.NULL;
  }

  /**
   * Retrieve table from current FeatureTypeMapping
   *
   * @param tableName Mapping Table name
   * @return MappingTable
   */
  Optional<FeatureSchema.Builder> getTableMapping(String tableName) {
    return Optional.ofNullable(currentMappingTables.get(tableName));
  }

  void addValueMappingForTable(
      final Property target, final FeatureSchema.Builder valueMapping, final String tableName) {
    final FeatureSchema.Builder tableMapping =
        getTableMapping(tableName)
            .orElseThrow(() -> new IllegalArgumentException("Table " + tableName + " not found"));

    tableMapping.putProperties2(valueMapping.build().getName(), (Builder) valueMapping);
  }

  IOReporter getReporter() {
    return reporter;
  }

  /**
   * Return the property cells for a type cell
   *
   * @param typeCell the type cell
   * @return the property cells associated with type cell
   */
  Collection<? extends Cell> getPropertyCells(final Cell typeCell) {
    return this.alignment.getPropertyCells(typeCell);
  }

  /**
	 * Return the FeatureProviderDataV2 containing all FeatureSchemas that were
	 * propagated
   *
   * @return FeatureProviderDataV2 containing all FeatureSchemas
   */
  public FeatureProviderDataV2 getProviderData(String id) {
    buildAndClearCurrentTables();

    final XtraServerMappingBuilder xtraServerMappingBuilder = new XtraServerMappingBuilder();

    xtraServerMappingBuilder.description(
        String.format(
            "\n  Source:\n    - hale %s\n    - %s\n",
            projectInfo.getHaleVersion(),
            projectLocation != null ? projectLocation : projectInfo.getName()));

    ImmutableFeatureProviderSqlData.Builder providerData = ldproxyCfg.builder().entity().provider()
        .id(id);

    featureTypeMappings.values().stream()
        .map(FeatureSchema.Builder::build)
        .forEach(featureSchema -> providerData.putTypes(featureSchema.getName(), featureSchema));

    return providerData.build();
  }

  /**
   * Replace project variables in a string
   *
   * @param str input string
   * @return string with replaced project variables, unresolved variables are replaced with
   *     'PROJECT_VARIABLE_<VARIABLE_NAME>_NOT_SET'
   */
  public String resolveProjectVars(final String str) {
    final Matcher m = projectVarPattern.matcher(str);
    String repStr = str;
    while (m.find()) {
      final String varName = m.group(1);
      final Value val = transformationProperties.get(varName);
      final String replacement;
      if (val != null && !val.isEmpty()) {
        replacement = val.as(String.class);
      } else {
        replacement = "PROJECT_VARIABLE_" + varName + "_NOT_SET";
      }
      repStr =
          repStr.replaceAll(
              "\\{\\{project:" + varName + "\\}\\}", Matcher.quoteReplacement(replacement));
    }
    return repStr;
  }
}
