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

import com.google.common.io.Files;
import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.features.sql.domain.FeatureProviderSqlData;
import de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData.Builder;
import eu.esdihumboldt.hale.common.align.io.impl.AbstractAlignmentWriter;
import eu.esdihumboldt.hale.common.core.io.IOProviderConfigurationException;
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.ValueProperties;
import eu.esdihumboldt.hale.common.core.io.project.ComplexConfigurationService;
import eu.esdihumboldt.hale.common.core.io.project.ProjectIO;
import eu.esdihumboldt.hale.common.core.io.project.model.Project;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/** Writes an Alignment to a XtraServer Web API Provider file. */
public class XtraServerWebApiMappingFileWriter extends AbstractAlignmentWriter {

  private static final String WRITER_TYPE_NAME = "XtraServer Web API Provider Exporter";

  static final String CONTENT_TYPE_MAPPING = "de.ii.xtraserver.webapi.hale.io.mapping.yaml";

  static final String CONTENT_TYPE_ARCHIVE = "de.ii.xtraserver.webapi.hale.io.mapping.archive";

  /**
   * @see eu.esdihumboldt.hale.common.core.io.IOProvider#isCancelable()
   */
  @Override
  public boolean isCancelable() {
    return false;
  }

  /**
   * @see eu.esdihumboldt.hale.common.core.io.impl.AbstractIOProvider#getDefaultTypeName()
   */
  @Override
  protected String getDefaultTypeName() {
    return WRITER_TYPE_NAME;
  }

  /**
   * @see eu.esdihumboldt.hale.common.core.io.impl.AbstractIOProvider#execute(ProgressIndicator,
   *     IOReporter)
   */
  @Override
  protected IOReport execute(final ProgressIndicator progress, final IOReporter reporter)
      throws IOProviderConfigurationException, IOException {

    ValueProperties projectProperties = null;
    if (getProjectInfo() instanceof Project) {
      final ComplexConfigurationService service =
          ProjectIO.createProjectConfigService((Project) getProjectInfo());
      projectProperties = service.getProperty("variables").as(ValueProperties.class);
    }
    if (projectProperties == null) {
      projectProperties = new ValueProperties();
    }

    progress.begin("Initialising", ProgressIndicator.UNKNOWN);
    if (getAlignment() == null) {
      throw new IOProviderConfigurationException("No alignment was provided.");
    }
    if (getTargetSchema() == null) {
      throw new IOProviderConfigurationException("No target schema was provided.");
    }
    if (getTarget() == null) {
      throw new IOProviderConfigurationException("No target was provided.");
    }

    Path dataDir = java.nio.file.Files.createTempDirectory("ldproxy-cfg");

    try (final OutputStream out = getTarget().getOutput()) {
      if (getContentType().getId().equals(CONTENT_TYPE_MAPPING)) {
        progress.setCurrentTask("Writing XtraServer Web API Provider File");
      } else {
        throw new IOProviderConfigurationException(
            "Content type not supported: " + getContentType().getName());
      }
      // TODO: validate
      String providerId =
          Files.getNameWithoutExtension(
              Paths.get(getTarget().getLocation()).getFileName().toString());

      LdproxyCfg ldproxyCfg = new LdproxyCfg(dataDir);
      Builder builder = ldproxyCfg.builder().entity().provider();
      builder.id(providerId);
      FeatureProviderSqlData provider = builder.build();
      ldproxyCfg.writeEntity(provider, out);

      progress.advance(1);
    } finally {
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
    }

    progress.end();
    reporter.setSuccess(true);

    return reporter;
  }
}
