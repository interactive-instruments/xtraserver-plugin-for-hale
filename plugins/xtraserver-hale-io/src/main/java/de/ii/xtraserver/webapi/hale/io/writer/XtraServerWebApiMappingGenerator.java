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
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
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
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.io.project.ProjectInfo;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

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
    // Calculate the total work units for the progress indicator (+1 for
    // writing the
    // file)
    int c = 1;
    for (final Cell typeCell : this.alignment.getActiveTypeCells()) {
      c += this.alignment.getPropertyCells(typeCell).size() + 1;
    }
    progress.begin("Translating hale alignment to XtraServer Mapping file", c);
    this.progress = progress;
  }

  /**
   * Generates the Mapping object
   *
   * @param reporter   status reporter
   * @param providerId
   * @return the generated XtraServer Mapping
   * @throws UnsupportedTransformationException if the transformation of types or properties is not
   *                                            supported
   */
  public void generate(final IOReporter reporter, final OutputStream out, String providerId)
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

          typeHandler.handle(typeCell);
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
        }
      } else {
        this.progress.advance(this.alignment.getPropertyCells(typeCell).size());
      }
    }

    ldproxyCfg.writeEntity(mappingContext.getProviderData(providerId), out);
  }

  /**
   * Return all property paths for which no association target could be found in the schema.
   *
   * @return list of properties with missing association targets
   */
  public Set<String> getMissingAssociationTargets() {
    return this.mappingContext.getMissingAssociationTargets();
  }

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

