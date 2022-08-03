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

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration.NESTED_OBJECTS;
import de.ii.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.CodelistData;
import de.ii.xtraplatform.codelists.domain.ImmutableCodelistData;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.transform.ImmutableFeaturePropertyTransformerFlatten;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraserver.hale.io.writer.XtraServerMappingUtils;
import de.ii.xtraserver.hale.io.writer.handler.CellParentWrapper;
import de.ii.xtraserver.hale.io.writer.handler.UnsupportedTransformationException;
import de.ii.xtraserver.webapi.hale.io.writer.handler.MappingContext;
import de.ii.xtraserver.webapi.hale.io.writer.handler.PropertyTransformationHandler;
import de.ii.xtraserver.webapi.hale.io.writer.handler.PropertyTransformationHandlerFactory;
import de.ii.xtraserver.webapi.hale.io.writer.handler.TypeTransformationHandler;
import de.ii.xtraserver.webapi.hale.io.writer.handler.TypeTransformationHandlerFactory;
import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.EntityDefinition;
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.io.project.ProjectInfo;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.filter.AbstractGeotoolsFilter;
import eu.esdihumboldt.hale.common.schema.model.Schema;
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;

/**
 * Translates an Alignment to a XtraServer Web API Mapping.
 */
public class XtraServerWebApiMappingGenerator {

  private final Alignment alignment;
  private final LdproxyCfg ldproxyCfg;
  private final MappingContext mappingContext;
  private final TypeTransformationHandlerFactory typeHandlerFactory;
  private final PropertyTransformationHandlerFactory propertyHandlerFactory;
  private final ProgressIndicator progress;
  private final String inspireSchemaName;

  /**
   * Constructor
   *
   * @param alignment         the Alignment with all cells
   * @param targetSchemaSpace the target schema
   * @param progress          Progress indicator
   * @param projectProperties project transformation properties
   * @param projectInfo       project info
   * @param projectLocation   project file
   * @param reporter          reporter
   */
  public XtraServerWebApiMappingGenerator(final Alignment alignment,
      final SchemaSpace targetSchemaSpace, final ProgressIndicator progress,
      final Map<String, Value> projectProperties, final ProjectInfo projectInfo,
      final URI projectLocation, final IOReporter reporter) throws IOException {

    this.alignment = alignment;
    Path dataDir = createTempDataDir();
    this.ldproxyCfg = new LdproxyCfg(dataDir);
    this.mappingContext = new MappingContext(alignment, targetSchemaSpace, projectProperties,
        projectInfo, projectLocation, reporter, ldproxyCfg);
    this.typeHandlerFactory = TypeTransformationHandler.createFactory(mappingContext);
    this.propertyHandlerFactory = PropertyTransformationHandler.createFactory(mappingContext);

    /* Calculate the total work units for the progress indicator (+1 for
     writing the file)*/
    int c = 1;
    for (final Cell typeCell : this.alignment.getActiveTypeCells()) {
      c += this.alignment.getPropertyCells(typeCell).size() + 1;
    }
    progress.begin("Translating hale alignment to XtraServer Mapping file", c);
    this.progress = progress;

    String inspireSchemaNameTmp = null;
    if (targetSchemaSpace.getSchemas() != null) {
      for (Schema xsd : targetSchemaSpace.getSchemas()) {
        if (xsd.getNamespace().contains("inspire.ec.europa.eu/schemas")) {
          String xsdLocation = xsd.getLocation().toString();
          if (StringUtils.isNotBlank(xsdLocation)) {
            try {
              String xsdFileName = xsdLocation.substring(xsdLocation.lastIndexOf("/") + 1,
                  xsdLocation.length() - 4);
              if (StringUtils.isNotBlank(xsdFileName)) {
                inspireSchemaNameTmp = xsdFileName;
                break;
              }
            } catch (IndexOutOfBoundsException e) {
              // ignore - xsd location malformed?
            }
          }
        }
      }
    }
    this.inspireSchemaName = inspireSchemaNameTmp;
  }

  /**
   * Generates the Mapping object
   *
   * @param reporter   status reporter
   * @param providerId
   * @throws UnsupportedTransformationException if the transformation of types or properties is not
   *                                            supported
   */
  public void generate(final IOReporter reporter, final OutputStream out, String providerId,
      boolean onlyProviderFile)
      throws UnsupportedTransformationException, IOException {

    for (final Cell typeCell : this.alignment.getActiveTypeCells()) {

      final String typeTransformationIdentifier = typeCell.getTransformationIdentifier();

            /* Create FeatureTypeMapping from the type cells. The Mapping tables
       are created and added by the Type Handlers*/
      this.progress.setCurrentTask("Transforming type");

      final TypeTransformationHandler typeHandler = typeHandlerFactory
          .create(typeTransformationIdentifier);

      if (typeHandler != null) {

        // TODO - FUTURE WORK - multiple mappings per feature type currently not supported
        QName featureTypeQName = XtraServerMappingUtils.getFeatureTypeName(typeCell);
        if (this.mappingContext.getFeatureTypeMappings().containsKey(featureTypeQName.toString())) {
          mappingContext.getReporter().warn(
              "Multiple mappings with the same target type are currently not supported. Only the first mapping for Feature Type {0} was created.",
              featureTypeQName.getLocalPart());
          this.progress.advance(this.alignment.getPropertyCells(typeCell).size());

        } else {

          // PROCESSING

          ImmutableFeatureSchema.Builder typeBuilder = typeHandler.handle(typeCell);
          this.progress.setCurrentTask(
              "Mapping values for Feature Type " + mappingContext.getFeatureTypeName());

          // Add MappingValues from the type cell's property cells
          for (final Cell propertyCell : this.alignment.getPropertyCells(typeCell)
              .stream()
              .sorted(Comparator.comparing(Cell::getPriority))
              .collect(Collectors.toList())) {

            final String propertyTransformationIdentifier = propertyCell
                .getTransformationIdentifier();
            final PropertyTransformationHandler propertyHandler = propertyHandlerFactory
                .create(propertyTransformationIdentifier);
            if (propertyHandler != null) {
              propertyHandler.handle(new CellParentWrapper(typeCell, propertyCell));
            }
            this.progress.advance(1);
          }

          // POSTPROCESSING

          if (typeBuilder != null) {
            EntityDefinition mainEntityDefinition = this.mappingContext.getMainEntityDefinition();
            TypeDefinition mainTypeDefinition = mainEntityDefinition.getType();
            String mainTableName = mainTypeDefinition.getName().getLocalPart();
            String sourcePath = "/" + mainTableName;

            // primary key is currently not used
//            String primaryKey = TypeTransformationHandler.getPrimaryKey(mainTypeDefinition);
//            if(StringUtils.isNotBlank(primaryKey)) {
//              sourcePath += "{primaryKey="+primaryKey+"}";
//            }
            if (this.mappingContext.getMainSortKeyField() != null) {
              sourcePath += "{sortKey=" + this.mappingContext.getMainSortKeyField() + "}";
            }
            if (mainEntityDefinition.getFilter() != null) {
              try {
                AbstractGeotoolsFilter filter = (AbstractGeotoolsFilter) mainEntityDefinition.getFilter();
                Filter qualifiedFilter = ECQL.toFilter(filter.getFilterTerm());
                sourcePath += "{filter=" + ECQL.toCQL(qualifiedFilter) + "}";
              } catch (ClassCastException | CQLException e) {
                // ignore
              }
            }

            typeBuilder.sourcePath(sourcePath);
          }
        }
      } else {
        this.progress.advance(this.alignment.getPropertyCells(typeCell).size());
      }
    }

    FeatureProviderDataV2 providerData = mappingContext.getProviderData(providerId);

    if (onlyProviderFile) {
      ldproxyCfg.writeEntity(providerData, out);
    } else {

      ldproxyCfg.addEntity(providerData);

      ImmutableOgcApiDataV2.Builder apiBuilder = ldproxyCfg.builder().entity().api();
      apiBuilder.id(providerData.getId()).entityStorageVersion(2).serviceType("OGC_API");
      apiBuilder.label("${" + providerId + ".service.label:-INSPIRE " + (
          StringUtils.isNotBlank(this.inspireSchemaName) ? this.inspireSchemaName : providerId)
          + "}");

      // Konfigurieren der Abflachung
      ImmutableGeoJsonConfiguration.Builder gjBuilder = ldproxyCfg.builder().ogcApiExtension().geoJson();
      List<PropertyTransformation> geoJsonApiTransformations = new ArrayList<>();
      ImmutablePropertyTransformation.Builder flattenTrfBuilder = new ImmutablePropertyTransformation.Builder();
      flattenTrfBuilder.flatten("_");
      geoJsonApiTransformations.add(flattenTrfBuilder.build());
      gjBuilder.putTransformations("*",geoJsonApiTransformations);
      apiBuilder.addExtensions(gjBuilder.build());


      // create service collections (with id, label and description per provider type)
      SortedMap<String, FeatureTypeConfigurationOgcApi> serviceCollDefsMap = new TreeMap<>();
      for (FeatureSchema providerType : providerData.getTypes().values()) {
        ImmutableFeatureTypeConfigurationOgcApi.Builder serviceCollDefBuilder = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
            .id(providerType.getName()).description(providerType.getDescription());
        if(providerType.getLabel().isPresent()) {
          serviceCollDefBuilder.label(providerType.getLabel().get());
        }
        serviceCollDefsMap.put(providerType.getName(),serviceCollDefBuilder.build());
      }
      apiBuilder.collections(serviceCollDefsMap);

      ldproxyCfg.addEntity(apiBuilder.build());

      // write codelist entities stored in the mapping context
      List<ImmutableCodelistData> codelists = mappingContext.getCodeLists();
      for (ImmutableCodelistData codelist : codelists) {
        ldproxyCfg.addEntity(codelist);
      }

      ldproxyCfg.writeZippedStore(out);
    }
  }

//  /**
//   * Return all property paths for which no association target could be found in the schema.
//   *
//   * @return list of properties with missing association targets
//   */
//  public Set<String> getMissingAssociationTargets() {
//    return this.mappingContext.getMissingAssociationTargets();
//  }

  private Path createTempDataDir() throws IOException {
    Path dataDir = Files.createTempDirectory("ldproxy-cfg");

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    java.nio.file.Files.walk(dataDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                  } catch (IOException e) {
                    // ignore
                  }
                }));

    return dataDir;
  }

}

