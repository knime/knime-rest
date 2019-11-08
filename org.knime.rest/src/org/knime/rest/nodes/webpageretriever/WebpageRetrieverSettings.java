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
 *   Sep 25, 2019 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.rest.nodes.webpageretriever;

import java.util.Collections;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.rest.nodes.common.RestSettings;

/**
 * Node settings of the Webpage Retriever node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class WebpageRetrieverSettings extends RestSettings {

    private static final String COOKIE_OUTPUT_COLUMN_NAME = "Cookie output column name";

    private static final String EXTRACT_COOKIES = "Extract cookies";

    /**
     * Default cookie output column name.
     */
    static final String DEFAULT_COOKIE_OUTPUT_COLUMN_NAME = "Cookies";

    private static final boolean DEFAULT_EXTRACT_COOKIES = false;

    private static final String OUTPUT_COL_NAME = "Output column name";

    private static final String REPLACE_RELATIVE_URLS = "Replace relative URLS";

    private static final String OUTPUT_AS_XML = "Output as XML";

    private static final String DEFAULT_OUTPUT_COL_NAME = "Document";

    private static final boolean DEFAULT_REPLACE_RELATIVE_URLS = true;

    private static final boolean DEFAULT_OUTPUT_AS_XML = true;

    /** Use a different default for the fail on... settings */
    private static final boolean DEFAULT_FAIL_ON_PROBLEMS = true;

    private String m_outputColumnName = DEFAULT_OUTPUT_COL_NAME;

    private boolean m_replaceRelativeURLS = DEFAULT_REPLACE_RELATIVE_URLS;

    private boolean m_outputAsXML = DEFAULT_OUTPUT_AS_XML;

    private boolean m_extractCookies = DEFAULT_EXTRACT_COOKIES;

    private String m_cookieOutputColumnName = DEFAULT_COOKIE_OUTPUT_COLUMN_NAME;

    /** */
    WebpageRetrieverSettings() {
        super(Collections.emptyList());
        setFailOnConnectionProblems(DEFAULT_FAIL_ON_PROBLEMS);
        setFailOnHttpErrors(DEFAULT_FAIL_ON_PROBLEMS);
    }

    /**
     * @return the extractCookies
     */
    boolean isExtractCookies() {
        return m_extractCookies;
    }

    /**
     * @param extractCookies the extractCookies to set
     */
    void setExtractCookies(final boolean extractCookies) {
        m_extractCookies = extractCookies;
    }

    /**
     * @return the cookieColumnName
     */
    String getCookieOutputColumnName() {
        return m_cookieOutputColumnName;
    }

    /**
     * @param cookieColumnName the cookieColumnName to set
     */
    void setCookieOutputColumnName(final String cookieColumnName) {
        m_cookieOutputColumnName = cookieColumnName;
    }

    /**
     * @return the replaceRelativeURLS
     */
    public boolean isReplaceRelativeURLS() {
        return m_replaceRelativeURLS;
    }

    /**
     * @param replaceRelativeURLS the replaceRelativeURLS to set
     */
    public void setReplaceRelativeURLS(final boolean replaceRelativeURLS) {
        m_replaceRelativeURLS = replaceRelativeURLS;
    }

    /**
     * @return the outputAsXML
     */
    public boolean isOutputAsXML() {
        return m_outputAsXML;
    }

    /**
     * @param outputAsXML the outputAsXML to set
     */
    public void setOutputAsXML(final boolean outputAsXML) {
        m_outputAsXML = outputAsXML;
    }

    /**
     * @return the outputColumnName
     */
    public String getOutputColumnName() {
        return m_outputColumnName;
    }

    /**
     * @param outputColumnName the outputColumnName to set
     */
    public void setOutputColumnName(final String outputColumnName) {
        m_outputColumnName = outputColumnName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO settings) {
        super.saveSettings(settings);
        settings.addString(OUTPUT_COL_NAME, m_outputColumnName);
        settings.addBoolean(REPLACE_RELATIVE_URLS, m_replaceRelativeURLS);
        settings.addBoolean(OUTPUT_AS_XML, m_outputAsXML);
        settings.addBoolean(EXTRACT_COOKIES, m_extractCookies);
        settings.addString(COOKIE_OUTPUT_COLUMN_NAME, m_cookieOutputColumnName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final CredentialsProvider credentialNames,
        final DataTableSpec... specs) throws InvalidSettingsException {
        super.loadSettingsForDialog(settings, credentialNames, specs);
        m_outputColumnName = settings.getString(OUTPUT_COL_NAME, DEFAULT_OUTPUT_COL_NAME);
        m_replaceRelativeURLS = settings.getBoolean(REPLACE_RELATIVE_URLS, DEFAULT_REPLACE_RELATIVE_URLS);
        m_outputAsXML = settings.getBoolean(OUTPUT_AS_XML, DEFAULT_OUTPUT_AS_XML);
        m_extractCookies = settings.getBoolean(EXTRACT_COOKIES, DEFAULT_EXTRACT_COOKIES);
        m_cookieOutputColumnName = settings.getString(COOKIE_OUTPUT_COLUMN_NAME, DEFAULT_COOKIE_OUTPUT_COLUMN_NAME);
        setFailOnConnectionProblems(settings.getBoolean(FAIL_ON_CONNECTION_PROBLEMS, DEFAULT_FAIL_ON_PROBLEMS));
        setFailOnHttpErrors(settings.getBoolean(FAIL_ON_HTTP_ERRORS, DEFAULT_FAIL_ON_PROBLEMS));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettingsFrom(settings);
        m_outputColumnName = settings.getString(OUTPUT_COL_NAME);
        m_replaceRelativeURLS = settings.getBoolean(REPLACE_RELATIVE_URLS);
        m_outputAsXML = settings.getBoolean(OUTPUT_AS_XML);
        m_extractCookies = settings.getBoolean(EXTRACT_COOKIES);
        m_cookieOutputColumnName = settings.getString(COOKIE_OUTPUT_COLUMN_NAME);
    }
}
