/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   24. Apr. 2016. (Gabor Bakos): created
 */
package org.knime.rest.nodes.common;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * {@link RestSettings} with options for invoking REST calls requiring entities in their body ({@code POST}, {@code PUT}).
 *
 * @author Gabor Bakos
 */
public class RestWithBodySettings extends RestSettings {

    private static final String USE_CONSTANT_REQUEST_BODY = "Use constant request body";

    private static final boolean DEFAULT_USE_CONSTANT_REQUEST_BODY = true;

    private static final String CONSTANT_REQUEST_BODY = "Constant request body";

    private static final String DEFAULT_CONSTANT_REQUEST_BODY = "";

    private static final String REQUEST_BODY_COLUMN = "Request body column";

    private static final String DEFAULT_REQUEST_BODY_COLUMN = null;

    private boolean m_useConstantRequestBody = DEFAULT_USE_CONSTANT_REQUEST_BODY;

    private String m_constantRequestBody = DEFAULT_CONSTANT_REQUEST_BODY;

    private String m_requestBodyColumn = DEFAULT_REQUEST_BODY_COLUMN;

    /**
     * Constructs the settings with defaults.
     */
    public RestWithBodySettings() {
        super();
    }

    /**
     * @return the useConstantRequestBody
     */
    protected boolean isUseConstantRequestBody() {
        return m_useConstantRequestBody;
    }

    /**
     * @param useConstantRequestBody the useConstantRequestBody to set
     */
    protected void setUseConstantRequestBody(final boolean useConstantRequestBody) {
        m_useConstantRequestBody = useConstantRequestBody;
    }

    /**
     * @return the constantRequestBody
     */
    protected String getConstantRequestBody() {
        return m_constantRequestBody;
    }

    /**
     * @param constantRequestBody the constantRequestBody to set
     */
    protected void setConstantRequestBody(final String constantRequestBody) {
        m_constantRequestBody = constantRequestBody;
    }

    /**
     * @return the requestBodyColumn
     */
    protected String getRequestBodyColumn() {
        return m_requestBodyColumn;
    }

    /**
     * @param requestBodyColumn the requestBodyColumn to set
     */
    protected void setRequestBodyColumn(final String requestBodyColumn) {
        m_requestBodyColumn = requestBodyColumn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO settings) {
        super.saveSettings(settings);
        settings.addBoolean(USE_CONSTANT_REQUEST_BODY, m_useConstantRequestBody);
        settings.addString(CONSTANT_REQUEST_BODY, m_constantRequestBody);
        settings.addString(REQUEST_BODY_COLUMN, m_requestBodyColumn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettingsFrom(settings);
        m_useConstantRequestBody = settings.getBoolean(USE_CONSTANT_REQUEST_BODY);
        m_constantRequestBody = settings.getString(CONSTANT_REQUEST_BODY);
        m_requestBodyColumn = settings.getString(REQUEST_BODY_COLUMN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final CredentialsProvider credentialNames,
        final DataTableSpec... specs) throws InvalidSettingsException {
        super.loadSettingsForDialog(settings, credentialNames, specs);
        m_useConstantRequestBody = settings.getBoolean(USE_CONSTANT_REQUEST_BODY, DEFAULT_USE_CONSTANT_REQUEST_BODY);
        m_constantRequestBody = settings.getString(CONSTANT_REQUEST_BODY, DEFAULT_CONSTANT_REQUEST_BODY);
        m_requestBodyColumn = settings.getString(REQUEST_BODY_COLUMN, DEFAULT_REQUEST_BODY_COLUMN);
    }

}
