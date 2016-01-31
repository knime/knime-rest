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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;

/**
 * Node settings for the GET REST node.
 * @author Gabor Bakos
 */
final class RestGetSettings {
    /**
     *
     */
    enum ParameterKind {
            Header,
            Body,
            Path
    }

    private static final String USE_CONSTANT_URI = "Use constant URI";
    private static final boolean DEFAULT_USE_CONSTANT_URI = true;
    private static final String CONSTANT_URI = "Constant URI";
    private static final String DEFAULT_CONSTANT_URI = "https://www.google.com";
    private static final String URI_COLUMN = "URI column";
    private static final String DEFAULT_URI_COLUMN = "URI";
    private static final String USE_DELAY = "delay_enabled";
    private static final boolean DEFAULT_USE_DELAY = false;
    private static final String DELAY = "delay";
    private static final long DEFAULT_DELAY = 0L;
    private static final String CONCURRENCY = "concurrency";
    private static final int DEFAULT_CONCURRENCY = 1;
    private static final String SSL_IGNORE_HOSTNAME_ERRORS = "SSL ignore hostname errors";
    private static final boolean DEFAULT_IGNORE_HOSTNAME_ERRORS = false;
    private static final String SSL_TRUST_ALL = "SSL trust all";
    private static final boolean DEFAULT_SSL_TRUST_ALL = false;
    private static final String REQUEST_HEADER_KEYS = "Request header keys";
    private static final String REQUEST_HEADER_KEY_SELECTOR = "Request header key selector";
    private static final String REQUEST_HEADER_KEY_KIND = "Request header key kind";
    private static final List<RequestHeaderKeyItem> DEFAULT_REQUEST_HEADER_KEY_ITEMS = Collections.unmodifiableList(Arrays.asList());
    private static final String EXTRACT_ALL_RESPONSE_FIELDS = "Extract all response fields";
    private static final boolean DEFAULT_EXTRACT_ALL_RESPONSE_FIELDS = false;
    private static final String RESPONSE_HEADER_KEYS = "Response header keys";
    //private static final String RESPONSE_HEADER_VALUE_TYPE = "Response header value type";
    private static final String RESPONSE_HEADER_COLUMN_NAME = "Response header column name";
    private static final List<ResponseHeaderItem> DEFAULT_RESPONSE_HEADER_ITEMS = Collections.unmodifiableList(Arrays.asList(
        new ResponseHeaderItem("Status", IntCell.TYPE),
        new ResponseHeaderItem("Content-Type", "Content type")
        ));
    private static final String BODY_COLUMN_NAME = "Body column name";
    private static final String DEFAULT_BODY_COLUMN_NAME = "body";
    //The column or the actual constant URI
    private boolean m_isUseConstantURI = DEFAULT_USE_CONSTANT_URI;
    private String m_constantURI = DEFAULT_CONSTANT_URI;
    private String m_uriColumn = DEFAULT_URI_COLUMN;

    private boolean m_useDelay = DEFAULT_USE_DELAY;
    private long m_delay = DEFAULT_DELAY;

    private int m_concurrency = DEFAULT_CONCURRENCY;

    private boolean m_sslIgnoreHostNameErrors = DEFAULT_IGNORE_HOSTNAME_ERRORS;
    private boolean m_sslTrustAll = DEFAULT_SSL_TRUST_ALL;

    //Request
    enum ReferenceType {
        Constant, FlowVariable, Column;
    }

    static class RequestHeaderKeyItem/*<InputType extends DataValue>*/ {
        private final String m_key;
        private final String m_valueReference;
        private final ReferenceType m_kind;
        //private final Function<InputType, String> m_conversionFunction;
        private final ParameterKind m_parameterKind = ParameterKind.Header;
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
         * @return the parameterKind
         */
        ParameterKind getParameterKind() {
            return m_parameterKind;
        }
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

    static class ResponseHeaderItem {
        private final String m_headerKey;
        private final DataType m_type;
        private final String m_outputColumnName;
        /**
         * @param headerKey
         * @param type
         * @param outputColumnName
         */
        ResponseHeaderItem(final String headerKey, final DataType type, final String outputColumnName) {
            super();
            m_headerKey = headerKey;
            m_type = type;
            m_outputColumnName = outputColumnName;
        }
        /**
         * @param headerKey
         * @param outputColumnName
         */
        ResponseHeaderItem(final String headerKey, final String outputColumnName) {
            this(headerKey, "Status".equals(headerKey) ? IntCell.TYPE : StringCell.TYPE, outputColumnName);
        }
        /**
         * @param headerKey
         * @param type
         */
        ResponseHeaderItem(final String headerKey, final DataType type) {
            this(headerKey, type, headerKey);
        }
        /**
         * @param headerKey
         */
        ResponseHeaderItem(final String headerKey) {
            this(headerKey, headerKey);
        }
        /**
         * @return the headerKey
         */
        String getHeaderKey() {
            return m_headerKey;
        }
        /**
         * @return the type
         */
        DataType getType() {
            return m_type;
        }
        /**
         * @return the outputColumnName
         */
        String getOutputColumnName() {
            return m_outputColumnName;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("[headerKey=%s, type=%s, outputColumnName=%s]", m_headerKey,
                m_type, m_outputColumnName);
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((m_headerKey == null) ? 0 : m_headerKey.hashCode());
            result = prime * result + ((m_outputColumnName == null) ? 0 : m_outputColumnName.hashCode());
            result = prime * result + ((m_type == null) ? 0 : m_type.hashCode());
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
            ResponseHeaderItem other = (ResponseHeaderItem)obj;
            if (m_headerKey == null) {
                if (other.m_headerKey != null) {
                    return false;
                }
            } else if (!m_headerKey.equals(other.m_headerKey)) {
                return false;
            }
            if (m_outputColumnName == null) {
                if (other.m_outputColumnName != null) {
                    return false;
                }
            } else if (!m_outputColumnName.equals(other.m_outputColumnName)) {
                return false;
            }
            if (m_type == null) {
                if (other.m_type != null) {
                    return false;
                }
            } else if (!m_type.equals(other.m_type)) {
                return false;
            }
            return true;
        }
    }

    private final List<ResponseHeaderItem> m_extractFields = new ArrayList<>();
    {
        m_extractFields.add(new ResponseHeaderItem("Status", IntCell.TYPE, "Status"));
        m_extractFields.add(new ResponseHeaderItem("Content-Type", StringCell.TYPE, "Content type"));
    }

    private String m_responseBodyColumn = DEFAULT_BODY_COLUMN_NAME;

    /**
     *
     */
    RestGetSettings() {
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
     * @return the useDelay
     */
    boolean isUseDelay() {
        return m_useDelay;
    }

    /**
     * @param useDelay the useDelay to set
     */
    void setUseDelay(final boolean useDelay) {
        m_useDelay = useDelay;
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
    List<ResponseHeaderItem> getExtractFields() {
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
        settings.addBoolean(USE_CONSTANT_URI, m_isUseConstantURI);
        settings.addString(CONSTANT_URI, m_constantURI);
        settings.addString(URI_COLUMN, m_uriColumn);
        settings.addBoolean(USE_DELAY, m_useDelay);
        settings.addLong(DELAY, m_delay);
        settings.addInt(CONCURRENCY, m_concurrency);
        settings.addBoolean(SSL_IGNORE_HOSTNAME_ERRORS, m_sslIgnoreHostNameErrors);
        settings.addBoolean(SSL_TRUST_ALL, m_sslTrustAll);
        settings.addStringArray(REQUEST_HEADER_KEYS, m_requestHeaders.stream().map(rh -> rh.getKey()).toArray(n -> new String[n]));
        settings.addStringArray(REQUEST_HEADER_KEY_SELECTOR, m_requestHeaders.stream().map(rh -> rh.getValueReference()).toArray(n -> new String[n]));
        settings.addStringArray(REQUEST_HEADER_KEY_KIND, m_requestHeaders.stream().map(rh -> rh.getKind().name()).toArray(n -> new String[n]));
        settings.addBoolean(EXTRACT_ALL_RESPONSE_FIELDS, m_extractAllResponseFields);
        settings.addStringArray(RESPONSE_HEADER_KEYS, m_extractFields.stream().map(rh -> rh.getHeaderKey()).toArray(n -> new String[n]));
        //settings.addStringArray(RESPONSE_HEADER_VALUE_TYPE, m_extractFields.stream().map(rh -> rh.getType().getName()).toArray(n -> new String[n]));
        settings.addStringArray(RESPONSE_HEADER_COLUMN_NAME, m_extractFields.stream().map(rh -> rh.getOutputColumnName()).toArray(n -> new String[n]));
        settings.addString(BODY_COLUMN_NAME, m_responseBodyColumn);
    }

    void loadSettingsFrom(final ConfigRO settings) throws InvalidSettingsException {
        m_isUseConstantURI = settings.getBoolean(USE_CONSTANT_URI);
        m_constantURI = settings.getString(CONSTANT_URI);
        m_uriColumn = settings.getString(URI_COLUMN);
        m_useDelay = settings.getBoolean(USE_DELAY);
        m_delay = settings.getLong(DELAY);
        m_concurrency = settings.getInt(CONCURRENCY);
        m_sslIgnoreHostNameErrors = settings.getBoolean(SSL_IGNORE_HOSTNAME_ERRORS);
        m_sslTrustAll = settings.getBoolean(SSL_TRUST_ALL);
        m_requestHeaders.clear();
        String[] requestKeys = settings.getStringArray(REQUEST_HEADER_KEYS);
        String[] requestKeySelectors = settings.getStringArray(REQUEST_HEADER_KEY_SELECTOR);
        String[] requestKeyKinds = settings.getStringArray(REQUEST_HEADER_KEY_KIND);
        CheckUtils.checkSetting(requestKeyKinds.length == requestKeys.length, "Request keys and request key kinds have different lengths: " + requestKeys.length + " <> " + requestKeyKinds.length);
        CheckUtils.checkSetting(requestKeys.length == requestKeySelectors.length, "Request keys and request key selectors have different lengths: " + requestKeys.length + " <> " + requestKeySelectors.length);
        for (int i = 0; i < requestKeys.length; ++i) {
            m_requestHeaders.add(new RequestHeaderKeyItem(requestKeys[i], requestKeySelectors[i], ReferenceType.valueOf(requestKeyKinds[i])));
        }

        m_extractAllResponseFields = settings.getBoolean(EXTRACT_ALL_RESPONSE_FIELDS);
        m_extractFields.clear();
        String[] responseKeys = settings.getStringArray(RESPONSE_HEADER_KEYS);
        String[] responseColumns = settings.getStringArray(RESPONSE_HEADER_COLUMN_NAME);
        CheckUtils.checkSetting(responseKeys.length == responseColumns.length, "Response header keys and output columns for them have different lengths: " + responseKeys.length + " <> " + responseColumns.length);
        for (int i = 0; i < responseKeys.length; ++i) {
            m_extractFields.add(new ResponseHeaderItem(responseKeys[i], responseColumns[i]));
        }
        m_responseBodyColumn = settings.getString(BODY_COLUMN_NAME);
    }

    void loadSettingsForDialog(final ConfigRO settings, final DataTableSpec... specs) throws InvalidSettingsException {
        m_isUseConstantURI = settings.getBoolean(USE_CONSTANT_URI, DEFAULT_USE_CONSTANT_URI);
        m_constantURI = settings.getString(CONSTANT_URI, DEFAULT_CONSTANT_URI);
        m_uriColumn = settings.getString(URI_COLUMN, DEFAULT_URI_COLUMN);
        m_useDelay = settings.getBoolean(USE_DELAY, DEFAULT_USE_DELAY);
        m_delay = settings.getLong(DELAY, DEFAULT_DELAY);
        m_concurrency = settings.getInt(CONCURRENCY, DEFAULT_CONCURRENCY);
        m_sslIgnoreHostNameErrors = settings.getBoolean(SSL_IGNORE_HOSTNAME_ERRORS, DEFAULT_IGNORE_HOSTNAME_ERRORS);
        m_sslTrustAll = settings.getBoolean(SSL_TRUST_ALL, DEFAULT_SSL_TRUST_ALL);
        m_requestHeaders.clear();
        String[] requestKeys = settings.getStringArray(REQUEST_HEADER_KEYS, DEFAULT_REQUEST_HEADER_KEY_ITEMS.stream().map(RequestHeaderKeyItem::getKey).toArray(n->new String[n]));
        String[] requestKeySelectors = settings.getStringArray(REQUEST_HEADER_KEY_SELECTOR, DEFAULT_REQUEST_HEADER_KEY_ITEMS.stream().map(RequestHeaderKeyItem::getValueReference).toArray(n -> new String[n]));
        String[] requestKeyKinds = settings.getStringArray(REQUEST_HEADER_KEY_KIND, DEFAULT_REQUEST_HEADER_KEY_ITEMS.stream().map(RequestHeaderKeyItem::getKind).map(ReferenceType::name).toArray(n->new String[n]));
        CheckUtils.checkSetting(requestKeyKinds.length == requestKeys.length, "Request keys and request key kinds have different lengths: " + requestKeys.length + " <> " + requestKeyKinds.length);
        CheckUtils.checkSetting(requestKeys.length == requestKeySelectors.length, "Request keys and request key selectors have different lengths: " + requestKeys.length + " <> " + requestKeySelectors.length);
        for (int i = 0; i < requestKeys.length; ++i) {
            m_requestHeaders.add(new RequestHeaderKeyItem(requestKeys[i], requestKeySelectors[i], ReferenceType.valueOf(requestKeyKinds[i])));
        }

        m_extractAllResponseFields = settings.getBoolean(EXTRACT_ALL_RESPONSE_FIELDS, DEFAULT_EXTRACT_ALL_RESPONSE_FIELDS);
        m_extractFields.clear();
        String[] responseKeys = settings.getStringArray(RESPONSE_HEADER_KEYS, DEFAULT_RESPONSE_HEADER_ITEMS.stream().map(ResponseHeaderItem::getHeaderKey).toArray(n -> new String[n]));
        String[] responseColumns = settings.getStringArray(RESPONSE_HEADER_COLUMN_NAME, DEFAULT_RESPONSE_HEADER_ITEMS.stream().map(ResponseHeaderItem::getOutputColumnName).toArray(n -> new String[n]));
        CheckUtils.checkSetting(responseKeys.length == responseColumns.length, "Response header keys and output columns for them have different lengths: " + responseKeys.length + " <> " + responseColumns.length);
        for (int i = 0; i < responseKeys.length; ++i) {
            m_extractFields.add(new ResponseHeaderItem(responseKeys[i], responseColumns[i]));
        }
        m_responseBodyColumn = settings.getString(BODY_COLUMN_NAME, DEFAULT_BODY_COLUMN_NAME);
    }
}
