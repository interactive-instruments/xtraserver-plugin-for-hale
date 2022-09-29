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
import de.ii.xtraserver.hale.io.writer.handler.UnsupportedTransformationException;
import eu.esdihumboldt.hale.common.align.io.impl.AbstractAlignmentWriter;
import eu.esdihumboldt.hale.common.core.io.IOProviderConfigurationException;
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.ValueProperties;
import eu.esdihumboldt.hale.common.core.io.project.ComplexConfigurationService;
import eu.esdihumboldt.hale.common.core.io.project.ProjectIO;
import eu.esdihumboldt.hale.common.core.io.project.model.Project;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Collections;

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

    try (final OutputStream out = getTarget().getOutput()) {
      if (getContentType().getId().equals(CONTENT_TYPE_MAPPING)) {
        progress.setCurrentTask("Writing XtraServer Web API Provider File");
      } else if (getContentType().getId().equals(CONTENT_TYPE_ARCHIVE)) {
        progress.setCurrentTask("Writing XtraServer Web API Configuration Archive");
      } else {
        throw new IOProviderConfigurationException(
            "Content type not supported: " + getContentType().getName());
      }

      String providerId =
          Files.getNameWithoutExtension(
              Paths.get(getTarget().getLocation()).getFileName().toString());

      if (!providerId.matches("[\\w-_]+")) {
        reporter.error("The chosen filename '" + providerId
                + "'  is not a valid provider id. It may only contain letters, numbers, hyphens and underscores.");
        reporter.setSuccess(false);
        return reporter;
      }

      final XtraServerWebApiMappingGenerator generator = new XtraServerWebApiMappingGenerator(
          getAlignment(), getTargetSchema(), progress,
          Collections.unmodifiableMap(projectProperties), getProjectInfo(),
          getProjectLocation(), reporter);

      generator.generate(reporter, out, providerId, getContentType().getId().equals(CONTENT_TYPE_MAPPING));

      progress.advance(1);
    } catch (final UnsupportedTransformationException e) {
      reporter.error("The transformation of the type '" + e.getTransformationIdentifier()
          + "'  is not supported. Make sure that the XtraServer compatibility mode is enabled.");
      reporter.setSuccess(false);
      return reporter;
    }

    progress.end();
    reporter.setSuccess(true);

    return reporter;
  }
}
