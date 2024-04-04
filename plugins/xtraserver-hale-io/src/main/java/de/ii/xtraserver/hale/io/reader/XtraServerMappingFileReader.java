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

package de.ii.xtraserver.hale.io.reader;

import de.ii.xtraserver.hale.io.reader.handler.AlignmentGenerator;
import java.io.IOException;
import java.io.InputStream;

import de.interactive_instruments.xtraserver.config.api.XtraServerMapping;
import de.interactive_instruments.xtraserver.config.io.XtraServerMappingFile;
import de.interactive_instruments.xtraserver.config.transformer.XtraServerMappingTransformer;
import eu.esdihumboldt.hale.common.align.io.EntityResolver;
import eu.esdihumboldt.hale.common.align.io.impl.AbstractAlignmentReader;
import eu.esdihumboldt.hale.common.align.model.MutableAlignment;
import eu.esdihumboldt.hale.common.core.io.IOProviderConfigurationException;
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.core.io.report.impl.IOMessageImpl;
import eu.esdihumboldt.hale.common.schema.model.Schema;
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;
import eu.esdihumboldt.hale.common.schema.model.TypeIndex;
import eu.esdihumboldt.hale.util.nonosgi.contenttype.describer.XMLRootElementContentDescriber2;

/**
 * Reads an XtraServer Mapping file into an Alignment .
 * 
 * @author zahnen
 */
public class XtraServerMappingFileReader extends AbstractAlignmentReader {

	// an explicit usage of XMLRootElementContentDescriber2 is needed, otherwise the implicit import in plugin.xml does not work
	private static final XMLRootElementContentDescriber2 DUMMY_IMPORT = new XMLRootElementContentDescriber2();

	/**
	 * @see eu.esdihumboldt.hale.common.core.io.IOProvider#isCancelable()
	 */
	@Override
	public boolean isCancelable() {
		return false;
	}

	/**
	 * @see eu.esdihumboldt.hale.common.align.io.impl.AbstractAlignmentReader#loadAlignment(eu.esdihumboldt.hale.common.core.io.ProgressIndicator,
	 *      eu.esdihumboldt.hale.common.core.io.report.IOReporter)
	 */
	@Override
	protected MutableAlignment loadAlignment(ProgressIndicator progress, IOReporter reporter)
			throws IOProviderConfigurationException, IOException {

		progress.begin("Initializing", ProgressIndicator.UNKNOWN);

		final TypeIndex schemaspace = getTargetSchema();
		if (schemaspace == null) {
			reporter.error("Load the target schema first!");
			reporter.setSuccess(false);
			return null;
		}
		if (!(schemaspace instanceof SchemaSpace)) {
			throw new IllegalArgumentException(
					"Unknown target schema type: " + schemaspace.getClass());
		}

		EntityResolver entityResolver = null;
		if (getServiceProvider() != null) {
			entityResolver = getServiceProvider().getService(EntityResolver.class);
		}
		TypeIndex sourceTypes = getSourceSchema();
		TypeIndex targetTypes = getTargetSchema();

		final Schema schema = ((SchemaSpace) schemaspace).getSchemas().iterator().next();

		final MutableAlignment alignment;

		try (final InputStream in = getSource().getInput()) {

			progress.setCurrentTask("Loading XtraServer Mapping file");

			final XtraServerMapping xtraServerMapping = XtraServerMappingFile.read().fromStream(in);

			final XtraServerMapping flatXtraServerMapping = XtraServerMappingTransformer
					.forMapping(xtraServerMapping).applySchemaInfo(schema.getLocation())
					.flattenInheritance().transform();

			final AlignmentGenerator haleAlignmentGenerator = new AlignmentGenerator(
					sourceTypes, targetTypes, entityResolver, progress, reporter,
					flatXtraServerMapping);

			alignment = haleAlignmentGenerator.generate();

		} catch (Exception e) {
			reporter.error(new IOMessageImpl(e.getMessage(), e));
			reporter.setSuccess(false);
			return null;
		}

		progress.end();
		reporter.setSuccess(true);

		return alignment;
	}

}
