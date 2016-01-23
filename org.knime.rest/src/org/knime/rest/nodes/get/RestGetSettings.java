/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   23. Jan. 2016. (Gabor Bakos): created
 */
package org.knime.rest.nodes.get;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * Node settings for the GET REST node.
 * @author Gabor Bakos
 */
final class RestGetSettings {
    //The column or the actual constant URI
    private boolean m_isUseConstantURI = false;
    private String m_constantURI = "";
    private String m_uriColumn = "";

    private long m_delay = 0L;

    private int m_concurrency = 1;

    private boolean m_sslIgnoreHostNameErrors = false;
    private boolean m_sslTrustAll = false;

    //Request
    enum ReferenceType {
        Constant, FlowVariable, Column;
    }

    static class RequestHeaderKeyItem/*<InputType extends DataValue>*/ {
        private final String m_key;
        private final String m_valueReference;
        private final ReferenceType m_kind;
        //private final Function<InputType, String> m_conversionFunction;
        /**
         * @param key
         * @param valueReference
         * @param kind
         * @param conversionFunction
         */
        RequestHeaderKeyItem(final String key, final String valueReference, final ReferenceType kind/*,
            final Function<InputType, String> conversionFunction*/) {
            super();
            m_key = key;
            m_valueReference = valueReference;
            m_kind = kind;
//            m_conversionFunction = conversionFunction;
        }
        /**
         * @return the key
         */
        final String getKey() {
            return m_key;
        }
        /**
         * @return the valueReference
         */
        final String getValueReference() {
            return m_valueReference;
        }
        /**
         * @return the kind
         */
        final ReferenceType getKind() {
            return m_kind;
        }
//        /**
//         * @return the conversionFunction
//         */
//        final Function<InputType, String> getConversionFunction() {
//            return m_conversionFunction;
//        }
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format(
                "[key=%s, valueReference=%s, kind=%s]", m_key,
                m_valueReference, m_kind/*, m_conversionFunction*/);
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((m_key == null) ? 0 : m_key.hashCode());
            result = prime * result + ((m_kind == null) ? 0 : m_kind.hashCode());
            result = prime * result + ((m_valueReference == null) ? 0 : m_valueReference.hashCode());
            return result;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            RequestHeaderKeyItem other = (RequestHeaderKeyItem)obj;
            if (m_key == null) {
                if (other.m_key != null) {
                    return false;
                }
            } else if (!m_key.equals(other.m_key)) {
                return false;
            }
            if (m_kind != other.m_kind) {
                return false;
            }
            if (m_valueReference == null) {
                if (other.m_valueReference != null) {
                    return false;
                }
            } else if (!m_valueReference.equals(other.m_valueReference)) {
                return false;
            }
            return true;
        }
    }

    private final List<RequestHeaderKeyItem> m_requestHeaders = new ArrayList<>();

    //Response
    private boolean m_extractAllResponseFields;

    private final Map<String, DataType> m_extractFields = new LinkedHashMap<>();

    private String m_responseBodyColumn;

    /**
     *
     */
    public RestGetSettings() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @return the isUseConstantURI
     */
    boolean isUseConstantURI() {
        return m_isUseConstantURI;
    }

    /**
     * @param isUseConstantURI the isUseConstantURI to set
     */
    void setUseConstantURI(final boolean isUseConstantURI) {
        m_isUseConstantURI = isUseConstantURI;
    }

    /**
     * @return the constantURI
     */
    String getConstantURI() {
        return m_constantURI;
    }

    /**
     * @param constantURI the constantURI to set
     */
    void setConstantURI(final String constantURI) {
        m_constantURI = constantURI;
    }

    /**
     * @return the uriColumn
     */
    String getUriColumn() {
        return m_uriColumn;
    }

    /**
     * @param uriColumn the uriColumn to set
     */
    void setUriColumn(final String uriColumn) {
        m_uriColumn = uriColumn;
    }

    /**
     * @return the delay
     */
    long getDelay() {
        return m_delay;
    }

    /**
     * @param delay the delay to set
     */
    void setDelay(final long delay) {
        m_delay = delay;
    }

    /**
     * @return the concurrency
     */
    int getConcurrency() {
        return m_concurrency;
    }

    /**
     * @param concurrency the concurrency to set
     */
    void setConcurrency(final int concurrency) {
        m_concurrency = concurrency;
    }

    /**
     * @return the sslIgnoreHostNameErrors
     */
    boolean isSslIgnoreHostNameErrors() {
        return m_sslIgnoreHostNameErrors;
    }

    /**
     * @param sslIgnoreHostNameErrors the sslIgnoreHostNameErrors to set
     */
    void setSslIgnoreHostNameErrors(final boolean sslIgnoreHostNameErrors) {
        m_sslIgnoreHostNameErrors = sslIgnoreHostNameErrors;
    }

    /**
     * @return the sslTrustAll
     */
    boolean isSslTrustAll() {
        return m_sslTrustAll;
    }

    /**
     * @param sslTrustAll the sslTrustAll to set
     */
    void setSslTrustAll(final boolean sslTrustAll) {
        m_sslTrustAll = sslTrustAll;
    }

    /**
     * @return the extractAllResponseFields
     */
    boolean isExtractAllResponseFields() {
        return m_extractAllResponseFields;
    }

    /**
     * @param extractAllResponseFields the extractAllResponseFields to set
     */
    void setExtractAllResponseFields(final boolean extractAllResponseFields) {
        m_extractAllResponseFields = extractAllResponseFields;
    }

    /**
     * @return the requestHeaders
     */
    List<RequestHeaderKeyItem> getRequestHeaders() {
        return m_requestHeaders;
    }

    /**
     * @return the extractFields
     */
    Map<String, DataType> getExtractFields() {
        return m_extractFields;
    }

    /**
     * @return the responseBodyColumn
     */
    String getResponseBodyColumn() {
        return m_responseBodyColumn;
    }

    /**
     * @param responseBodyColumn the responseBodyColumn to set
     */
    void setResponseBodyColumn(final String responseBodyColumn) {
        m_responseBodyColumn = responseBodyColumn;
    }

    void saveSettings(final ConfigWO settings) {

    }

    void loadSettingsFrom(final ConfigRO settings) throws InvalidSettingsException {

    }

    void loadSettingsForDialog(final ConfigRO settings, final DataTableSpec spec) throws InvalidSettingsException {

    }
}
