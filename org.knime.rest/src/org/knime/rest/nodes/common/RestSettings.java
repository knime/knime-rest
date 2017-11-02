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
 *   23. Apr. 2016. (Gabor Bakos): created
 */
package org.knime.rest.nodes.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.rest.generic.EnablableUserConfiguration;
import org.knime.rest.generic.UserConfiguration;
import org.knime.rest.internals.NoAuthentication;

/**
 * Common settings for the REST nodes.
 *
 * @author Gabor Bakos
 */
public class RestSettings {

    /** The logger. */
    protected static final NodeLogger LOGGER = NodeLogger.getLogger(RestSettings.class);

    static final String EXTENSION_ID = "org.knime.rest.authentication";

    private static final String USE_CONSTANT_URI = "Use constant URI";

    private static final boolean DEFAULT_USE_CONSTANT_URI = true;

    private static final String CONSTANT_URI = "Constant URI";

    private static final String DEFAULT_CONSTANT_URI = "https://www.google.com";

    private static final String URI_COLUMN = "URI column";

    private static final String DEFAULT_URI_COLUMN = null;

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

    private static final String FAIL_ON_CONNECTION_PROBLEMS = "Fail on connection problems";

    /** Default value for the fail on connection problems option. */
    protected static final boolean DEFAULT_FAIL_ON_CONNECTION_PROBLEMS = false;

    private static final String FAIL_ON_HTTP_ERRORS = "Fail on HTTP errors";

    /** Default value for the fail on http errors option. */
    protected static final boolean DEFAULT_FAIL_ON_HTTP_ERRORS = false;

    private static final String REQUEST_HEADER_KEYS = "Request header keys";

    private static final String REQUEST_HEADER_KEY_SELECTOR = "Request header key selector";

    private static final String REQUEST_HEADER_KEY_KIND = "Request header key kind";

    private static final List<RequestHeaderKeyItem> DEFAULT_REQUEST_HEADER_KEY_ITEMS =
        Collections.unmodifiableList(Arrays.asList());

    private static final String EXTRACT_ALL_RESPONSE_FIELDS = "Extract all response fields";

    private static final boolean DEFAULT_EXTRACT_ALL_RESPONSE_FIELDS = false;

    private static final String RESPONSE_HEADER_KEYS = "Response header keys";

    private static final String RESPONSE_HEADER_COLUMN_NAME = "Response header column name";

    private static final String RESPONSE_HEADER_COLUMN_TYPE = "Response header column type";

    private static final String STATUS = "Status";

    private static final List<ResponseHeaderItem> DEFAULT_RESPONSE_HEADER_ITEMS = Collections.unmodifiableList(Arrays
        .asList(new ResponseHeaderItem(STATUS, IntCell.TYPE), new ResponseHeaderItem("Content-Type", "Content type")));

    private static final String BODY_COLUMN_NAME = "Body column name";

    private static final String DEFAULT_BODY_COLUMN_NAME = "body";

    private static final String ENABLED_SUFFIX = "_enabled";

    private static final String FOLLOW_REDIRECTS = "follow redirects";

    private static final boolean DEFAULT_FOLLOW_REDIRECTS = true;

    private static final String TIMEOUT = "timeout";

    /** Default value for the timeout (in seconds) option. */
    protected static final int DEFAULT_TIMEOUT = 2;

    private boolean m_isUseConstantURI = DEFAULT_USE_CONSTANT_URI;

    private String m_constantURI = DEFAULT_CONSTANT_URI;

    private String m_uriColumn = DEFAULT_URI_COLUMN;

    private boolean m_useDelay = DEFAULT_USE_DELAY;

    private long m_delay = DEFAULT_DELAY;

    private int m_concurrency = DEFAULT_CONCURRENCY;

    private boolean m_sslIgnoreHostNameErrors = DEFAULT_IGNORE_HOSTNAME_ERRORS;

    private boolean m_sslTrustAll = DEFAULT_SSL_TRUST_ALL;

    private boolean m_failOnConnectionProblems = DEFAULT_FAIL_ON_CONNECTION_PROBLEMS;

    private boolean m_failOnHttpErrors = DEFAULT_FAIL_ON_HTTP_ERRORS;

    private boolean m_followRedirects = DEFAULT_FOLLOW_REDIRECTS;

    private int m_timeoutInSeconds = DEFAULT_TIMEOUT;

    //Request
    /**
     * The enum describing the possible options to refer to values.
     */
    enum ReferenceType {
            /** A constant value. */
            Constant,
            /** Value of a flow variable. */
            FlowVariable {
                @Override
                public String toString() {
                    return "Flow variable";
                }
            },
            /** Value of a column in the actual row. */
            Column,
            /** The referred credential's user name. */
            CredentialName {
                @Override
                public String toString() {
                    return "Credential name";
                }
            },
            /** The referred credential's password. */
            CredentialPassword {
                @Override
                public String toString() {
                    return "Credential password";
                }
            };
    }

    /**
     * Datastructure for the key, valuereference and reference type tuple (for the request headers). <br/>
     * It does not support custom conversion of the values.
     */
    static class RequestHeaderKeyItem {
        private final String m_key;

        private final String m_valueReference;

        private final ReferenceType m_kind;

        /**
         * Constructs the object.
         *
         * @param key The key to be used in the header.
         * @param valueReference Reference to the value.
         * @param kind The {@link ReferenceType} of the value.
         */
        RequestHeaderKeyItem(final String key, final String valueReference, final ReferenceType kind) {
            super();
            m_key = key;
            m_valueReference = valueReference;
            m_kind = kind;
        }

        /**
         * @return the key in the header.
         */
        public final String getKey() {
            return m_key;
        }

        /**
         * @return the valueReference
         */
        public final String getValueReference() {
            return m_valueReference;
        }

        /**
         * @return the kind ({@link ReferenceType} of the value)
         */
        public final ReferenceType getKind() {
            return m_kind;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("[key=%s, valueReference=%s, kind=%s]", m_key, m_valueReference, m_kind);
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
            return Objects.equals(m_key, other.m_key) && Objects.equals(m_kind, other.m_kind)
                && Objects.equals(m_valueReference, other.m_valueReference);
        }
    }

    private final List<RequestHeaderKeyItem> m_requestHeaders = new ArrayList<>();

    private boolean m_extractAllResponseFields;

    /**
     * Response header specification item. (A tuple of the header key, the data type ({@link IntCell} -only for status
     * code- or {@link StringCell}) of the value and the output column name.)
     */
    static class ResponseHeaderItem {
        private final String m_headerKey;

        private final DataType m_type;

        private final String m_outputColumnName;

        /**
         * Constructs the object.
         *
         * @param headerKey The key within the header.
         * @param type The {@link DataType} of the output column, should be: {@link IntCell} for status code and
         *            {@link StringCell} for everything else.
         * @param outputColumnName The output column's name.
         */
        ResponseHeaderItem(final String headerKey, final DataType type, final String outputColumnName) {
            super();
            m_headerKey = headerKey;
            m_type = type;
            m_outputColumnName = outputColumnName;
        }

        /**
         * Constructs the object with {@link StringCell} data type (except for the {@code Status} header key, which case
         * it is {@link StringCell}).
         *
         * @param headerKey The key within the header.
         * @param outputColumnName The output column's name.
         */
        ResponseHeaderItem(final String headerKey, final String outputColumnName) {
            this(headerKey, STATUS.equals(headerKey) ? IntCell.TYPE : StringCell.TYPE, outputColumnName);
        }

        /**
         * Constructs the object with the same output column name as the {@code headerKey}.
         *
         * @param headerKey The key within the header.
         * @param type The {@link DataType} of the output column, should be: {@link IntCell} for status code and
         *            {@link StringCell} for everything else.
         */
        ResponseHeaderItem(final String headerKey, final DataType type) {
            this(headerKey, type, headerKey);
        }

        /**
         * Constructs the object with the same output column name as the {@code headerKey} with {@link StringCell} data
         * type (except for the {@code Status} header key, which case it is {@link StringCell}).
         *
         * @param headerKey The key within the header.
         */
        ResponseHeaderItem(final String headerKey) {
            this(headerKey, headerKey);
        }

        /**
         * @return the headerKey (in the response)
         */
        String getHeaderKey() {
            return m_headerKey;
        }

        /**
         * @return the type (of the output column)
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
            return String.format("[headerKey=%s, type=%s, outputColumnName=%s]", m_headerKey, m_type,
                m_outputColumnName);
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
            return Objects.equals(m_headerKey, other.m_headerKey)
                && Objects.equals(m_outputColumnName, other.m_outputColumnName) && Objects.equals(m_type, other.m_type);
        }
    }

    private final List<ResponseHeaderItem> m_extractFields = new ArrayList<>();

    private String m_responseBodyColumn = DEFAULT_BODY_COLUMN_NAME;

    private List<EnablableUserConfiguration<UserConfiguration>> m_authorizationConfigurations = new ArrayList<>();

    {
        m_extractFields.add(new ResponseHeaderItem(STATUS, IntCell.TYPE, STATUS));
        m_extractFields.add(new ResponseHeaderItem("Content-Type", StringCell.TYPE, "Content type"));
    }

    /**
     * Constructs the default settings.
     */
    protected RestSettings() {
        super();
        IConfigurationElement[] configurationElements =
            Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
        final EnablableUserConfiguration<UserConfiguration> noAuth =
            new EnablableUserConfiguration<>(new NoAuthentication(), "None");
        noAuth.setEnabled(true);
        m_authorizationConfigurations.add(noAuth);
        for (final IConfigurationElement configurationElement : configurationElements) {
            try {
                final Object object = configurationElement.createExecutableExtension("class");
                if (object instanceof UserConfiguration) {
                    final UserConfiguration uc = (UserConfiguration)object;
                    m_authorizationConfigurations.add(new EnablableUserConfiguration<UserConfiguration>(uc,
                        configurationElement.getAttribute("name")));
                }
            } catch (CoreException e) {
                LOGGER.warn("Failed to load: " + configurationElement.getName(), e);
            }
        }
    }

    /**
     * @return the isUseConstantURI
     */
    protected boolean isUseConstantURI() {
        return m_isUseConstantURI;
    }

    /**
     * @param isUseConstantURI the isUseConstantURI to set
     */
    protected void setUseConstantURI(final boolean isUseConstantURI) {
        m_isUseConstantURI = isUseConstantURI;
    }

    /**
     * @return the constantURI
     */
    protected String getConstantURI() {
        return m_constantURI;
    }

    /**
     * @param constantURI the constantURI to set
     */
    protected void setConstantURI(final String constantURI) {
        m_constantURI = constantURI;
    }

    /**
     * @return the uriColumn
     */
    protected String getUriColumn() {
        return m_uriColumn;
    }

    /**
     * @param uriColumn the uriColumn to set
     */
    protected void setUriColumn(final String uriColumn) {
        m_uriColumn = uriColumn;
    }

    /**
     * @return the useDelay
     */
    protected boolean isUseDelay() {
        return m_useDelay;
    }

    /**
     * @param useDelay the useDelay to set
     */
    protected void setUseDelay(final boolean useDelay) {
        m_useDelay = useDelay;
    }

    /**
     * @return the delay (in milliseconds)
     */
    protected long getDelay() {
        return m_delay;
    }

    /**
     * @param delay the delay (in milliseconds) to set
     */
    protected void setDelay(final long delay) {
        m_delay = delay;
    }

    /**
     * @return the concurrency
     */
    protected int getConcurrency() {
        return m_concurrency;
    }

    /**
     * @param concurrency the concurrency to set
     */
    protected void setConcurrency(final int concurrency) {
        m_concurrency = concurrency;
    }

    /**
     * @return the sslIgnoreHostNameErrors
     */
    protected boolean isSslIgnoreHostNameErrors() {
        return m_sslIgnoreHostNameErrors;
    }

    /**
     * @param sslIgnoreHostNameErrors the sslIgnoreHostNameErrors to set
     */
    protected void setSslIgnoreHostNameErrors(final boolean sslIgnoreHostNameErrors) {
        m_sslIgnoreHostNameErrors = sslIgnoreHostNameErrors;
    }

    /**
     * @return the sslTrustAll
     */
    protected boolean isSslTrustAll() {
        return m_sslTrustAll;
    }

    /**
     * @param sslTrustAll the sslTrustAll to set
     */
    protected void setSslTrustAll(final boolean sslTrustAll) {
        m_sslTrustAll = sslTrustAll;
    }

    /**
     * @return the failOnConnectionProblems
     */
    protected boolean isFailOnConnectionProblems() {
        return m_failOnConnectionProblems;
    }

    /**
     * @param failOnConnectionProblems the failOnConnectionProblems to set
     */
    protected void setFailOnConnectionProblems(final boolean failOnConnectionProblems) {
        m_failOnConnectionProblems = failOnConnectionProblems;
    }

    /**
     * @return the failOnHttpErrors
     */
    protected boolean isFailOnHttpErrors() {
        return m_failOnHttpErrors;
    }

    /**
     * @param failOnHttpErrors the failOnHttpErrors to set
     */
    protected void setFailOnHttpErrors(final boolean failOnHttpErrors) {
        m_failOnHttpErrors = failOnHttpErrors;
    }

    /**
     * @return the extractAllResponseFields
     */
    protected boolean isExtractAllResponseFields() {
        return m_extractAllResponseFields;
    }

    /**
     * @param extractAllResponseFields the extractAllResponseFields to set
     */
    protected void setExtractAllResponseFields(final boolean extractAllResponseFields) {
        m_extractAllResponseFields = extractAllResponseFields;
    }

    /**
     * @return the requestHeaders
     */
    protected List<RequestHeaderKeyItem> getRequestHeaders() {
        return m_requestHeaders;
    }

    /**
     * @return the extractFields (mutable, be careful!)
     */
    protected List<ResponseHeaderItem> getExtractFields() {
        return m_extractFields;
    }

    /**
     * @return the responseBodyColumn
     */
    protected String getResponseBodyColumn() {
        return m_responseBodyColumn;
    }

    /**
     * @param responseBodyColumn the responseBodyColumn to set
     */
    protected void setResponseBodyColumn(final String responseBodyColumn) {
        m_responseBodyColumn = responseBodyColumn;
    }

    /**
     * @return the followRedirects
     */
    protected boolean isFollowRedirects() {
        return m_followRedirects;
    }

    /**
     * @param followRedirects the followRedirects to set
     */
    protected void setFollowRedirects(final boolean followRedirects) {
        m_followRedirects = followRedirects;
    }

    /**
     * @return the timeoutInSeconds
     */
    protected int getTimeoutInSeconds() {
        return m_timeoutInSeconds;
    }

    /**
     * @param timeoutInSeconds the timeoutInSeconds to set
     */
    protected void setTimeoutInSeconds(final int timeoutInSeconds) {
        m_timeoutInSeconds = timeoutInSeconds;
    }

    /**
     * @return the authorizationConfigurations (not modifiable!)
     */
    protected List<EnablableUserConfiguration<UserConfiguration>> getAuthorizationConfigurations() {
        return Collections.unmodifiableList(m_authorizationConfigurations);
    }

    /**
     * Saves the internal state to {@code settings}.
     *
     * @param settings A writable {@link NodeSettingsWO}.
     */
    protected void saveSettings(final NodeSettingsWO settings) {
        settings.addBoolean(USE_CONSTANT_URI, m_isUseConstantURI);
        settings.addString(CONSTANT_URI, m_constantURI);
        settings.addString(URI_COLUMN, m_uriColumn);
        settings.addBoolean(USE_DELAY, m_useDelay);
        settings.addLong(DELAY, m_delay);
        settings.addInt(CONCURRENCY, m_concurrency);
        settings.addBoolean(SSL_IGNORE_HOSTNAME_ERRORS, m_sslIgnoreHostNameErrors);
        settings.addBoolean(SSL_TRUST_ALL, m_sslTrustAll);
        settings.addBoolean(FAIL_ON_CONNECTION_PROBLEMS, m_failOnConnectionProblems);
        settings.addBoolean(FAIL_ON_HTTP_ERRORS, m_failOnHttpErrors);
        settings.addStringArray(REQUEST_HEADER_KEYS,
            m_requestHeaders.stream().map(rh -> rh.getKey()).toArray(n -> new String[n]));
        settings.addStringArray(REQUEST_HEADER_KEY_SELECTOR,
            m_requestHeaders.stream().map(rh -> rh.getValueReference()).toArray(n -> new String[n]));
        settings.addStringArray(REQUEST_HEADER_KEY_KIND,
            m_requestHeaders.stream().map(rh -> rh.getKind().name()).toArray(n -> new String[n]));
        settings.addBoolean(EXTRACT_ALL_RESPONSE_FIELDS, m_extractAllResponseFields);
        settings.addStringArray(RESPONSE_HEADER_KEYS,
            m_extractFields.stream().map(rh -> rh.getHeaderKey()).toArray(n -> new String[n]));
        settings.addStringArray(RESPONSE_HEADER_COLUMN_NAME,
            m_extractFields.stream().map(rh -> rh.getOutputColumnName()).toArray(n -> new String[n]));
        settings.addDataTypeArray(RESPONSE_HEADER_COLUMN_TYPE,
            m_extractFields.stream().map(rh -> rh.getType()).toArray(n -> new DataType[n]));
        settings.addString(BODY_COLUMN_NAME, m_responseBodyColumn);
        for (final EnablableUserConfiguration<UserConfiguration> euc : m_authorizationConfigurations) {
            final UserConfiguration uc = euc.getUserConfiguration();
            final NodeSettingsWO configBase = settings.addNodeSettings(euc.getName());
            uc.saveUserConfiguration(configBase);
            settings.addBoolean(euc.getName() + ENABLED_SUFFIX, euc.isEnabled());
        }
        settings.addBoolean(FOLLOW_REDIRECTS, m_followRedirects);
        settings.addInt(TIMEOUT, m_timeoutInSeconds);
    }

    /**
     * Loads the internal state from {@code settings} for model load.
     *
     * @param settings The read only {@link NodeSettingsRO}.
     * @throws InvalidSettingsException When the state is inconsistent.
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_isUseConstantURI = settings.getBoolean(USE_CONSTANT_URI);
        m_constantURI = settings.getString(CONSTANT_URI);
        m_uriColumn = settings.getString(URI_COLUMN);
        m_useDelay = settings.getBoolean(USE_DELAY);
        m_delay = settings.getLong(DELAY);
        m_concurrency = settings.getInt(CONCURRENCY);
        m_sslIgnoreHostNameErrors = settings.getBoolean(SSL_IGNORE_HOSTNAME_ERRORS);
        m_sslTrustAll = settings.getBoolean(SSL_TRUST_ALL);
        m_failOnConnectionProblems = settings.getBoolean(FAIL_ON_CONNECTION_PROBLEMS);
        m_failOnHttpErrors = settings.getBoolean(FAIL_ON_HTTP_ERRORS);
        m_requestHeaders.clear();
        String[] requestKeys = settings.getStringArray(REQUEST_HEADER_KEYS);
        String[] requestKeySelectors = settings.getStringArray(REQUEST_HEADER_KEY_SELECTOR);
        String[] requestKeyKinds = settings.getStringArray(REQUEST_HEADER_KEY_KIND);
        CheckUtils.checkSetting(requestKeyKinds.length == requestKeys.length,
            "Request keys and request key kinds have different lengths: " + requestKeys.length + " <> "
                + requestKeyKinds.length);
        CheckUtils.checkSetting(requestKeys.length == requestKeySelectors.length,
            "Request keys and request key selectors have different lengths: " + requestKeys.length + " <> "
                + requestKeySelectors.length);
        for (int i = 0; i < requestKeys.length; ++i) {
            m_requestHeaders.add(new RequestHeaderKeyItem(requestKeys[i], requestKeySelectors[i],
                ReferenceType.valueOf(requestKeyKinds[i])));
        }

        m_extractAllResponseFields = settings.getBoolean(EXTRACT_ALL_RESPONSE_FIELDS);
        m_extractFields.clear();
        String[] responseKeys = settings.getStringArray(RESPONSE_HEADER_KEYS);
        String[] responseColumns = settings.getStringArray(RESPONSE_HEADER_COLUMN_NAME);
        final DataType[] responseTypes = settings.getDataTypeArray(RESPONSE_HEADER_COLUMN_TYPE);
        CheckUtils.checkSetting(responseKeys.length == responseColumns.length,
            "Response header keys and output columns: they have different lengths: " + responseKeys.length + " <> "
                + responseColumns.length);
        CheckUtils.checkSetting(responseKeys.length == responseTypes.length,
                "Response header keys and output column types: they have different lengths: " + responseKeys.length + " <> "
                        + responseTypes.length);
        for (int i = 0; i < responseKeys.length; ++i) {
            m_extractFields.add(new ResponseHeaderItem(responseKeys[i], responseTypes[i], responseColumns[i]));
        }
        m_responseBodyColumn = settings.getString(BODY_COLUMN_NAME);
        for (final EnablableUserConfiguration<UserConfiguration> euc : m_authorizationConfigurations) {
            final UserConfiguration uc = euc.getUserConfiguration();
            euc.setEnabled(settings.getBoolean(euc.getName() + ENABLED_SUFFIX, false));
            try {
                final NodeSettingsRO base = settings.getNodeSettings(euc.getName());
                uc.loadUserConfiguration(base);
            } catch (InvalidSettingsException e) {
                if (euc.isEnabled()) {
                    throw e;
                } else {
                    LOGGER.debug(e.getMessage(), e);
                }
            }
        }
        m_followRedirects = settings.getBoolean(FOLLOW_REDIRECTS);
        m_timeoutInSeconds = settings.getInt(TIMEOUT);
    }

    /**
     * Loads the settings for the dialog.
     *
     * @param settings The read only {@link NodeSettingsRO}.
     * @param credentialNames The credential names.
     * @param specs The input specs.
     * @throws InvalidSettingsException When the state is inconsistent.
     */
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final CredentialsProvider credentialNames,
        final DataTableSpec... specs) throws InvalidSettingsException {
        m_isUseConstantURI = settings.getBoolean(USE_CONSTANT_URI, DEFAULT_USE_CONSTANT_URI);
        m_constantURI = settings.getString(CONSTANT_URI, DEFAULT_CONSTANT_URI);
        m_uriColumn = settings.getString(URI_COLUMN, DEFAULT_URI_COLUMN);
        m_useDelay = settings.getBoolean(USE_DELAY, DEFAULT_USE_DELAY);
        m_delay = settings.getLong(DELAY, DEFAULT_DELAY);
        m_concurrency = settings.getInt(CONCURRENCY, DEFAULT_CONCURRENCY);
        m_sslIgnoreHostNameErrors = settings.getBoolean(SSL_IGNORE_HOSTNAME_ERRORS, DEFAULT_IGNORE_HOSTNAME_ERRORS);
        m_sslTrustAll = settings.getBoolean(SSL_TRUST_ALL, DEFAULT_SSL_TRUST_ALL);
        m_failOnConnectionProblems =
            settings.getBoolean(FAIL_ON_CONNECTION_PROBLEMS, DEFAULT_FAIL_ON_CONNECTION_PROBLEMS);
        m_failOnHttpErrors = settings.getBoolean(FAIL_ON_HTTP_ERRORS, DEFAULT_FAIL_ON_HTTP_ERRORS);
        m_requestHeaders.clear();
        String[] requestKeys = settings.getStringArray(REQUEST_HEADER_KEYS,
            DEFAULT_REQUEST_HEADER_KEY_ITEMS.stream().map(RequestHeaderKeyItem::getKey).toArray(n -> new String[n]));
        String[] requestKeySelectors =
            settings.getStringArray(REQUEST_HEADER_KEY_SELECTOR, DEFAULT_REQUEST_HEADER_KEY_ITEMS.stream()
                .map(RequestHeaderKeyItem::getValueReference).toArray(n -> new String[n]));
        String[] requestKeyKinds = settings.getStringArray(REQUEST_HEADER_KEY_KIND, DEFAULT_REQUEST_HEADER_KEY_ITEMS
            .stream().map(RequestHeaderKeyItem::getKind).map(ReferenceType::name).toArray(n -> new String[n]));
        CheckUtils.checkSetting(requestKeyKinds.length == requestKeys.length,
            "Request keys and request key kinds have different lengths: " + requestKeys.length + " <> "
                + requestKeyKinds.length);
        CheckUtils.checkSetting(requestKeys.length == requestKeySelectors.length,
            "Request keys and request key selectors have different lengths: " + requestKeys.length + " <> "
                + requestKeySelectors.length);
        for (int i = 0; i < requestKeys.length; ++i) {
            m_requestHeaders.add(new RequestHeaderKeyItem(requestKeys[i], requestKeySelectors[i],
                ReferenceType.valueOf(requestKeyKinds[i])));
        }

        m_extractAllResponseFields =
            settings.getBoolean(EXTRACT_ALL_RESPONSE_FIELDS, DEFAULT_EXTRACT_ALL_RESPONSE_FIELDS);
        m_extractFields.clear();
        String[] responseKeys = settings.getStringArray(RESPONSE_HEADER_KEYS,
            DEFAULT_RESPONSE_HEADER_ITEMS.stream().map(ResponseHeaderItem::getHeaderKey).toArray(n -> new String[n]));
        String[] responseColumns = settings.getStringArray(RESPONSE_HEADER_COLUMN_NAME, DEFAULT_RESPONSE_HEADER_ITEMS
            .stream().map(ResponseHeaderItem::getOutputColumnName).toArray(n -> new String[n]));
        final DataType[] responseTypes = settings.getDataTypeArray(RESPONSE_HEADER_COLUMN_TYPE,
            DEFAULT_RESPONSE_HEADER_ITEMS.stream().map(ResponseHeaderItem::getType).toArray(n -> new DataType[n]));
        CheckUtils.checkSetting(responseKeys.length == responseColumns.length,
            "Response header keys and output columns for them have different lengths: " + responseKeys.length + " <> "
                + responseColumns.length);
        CheckUtils.checkSetting(responseKeys.length == responseTypes.length,
                "Response header keys and output column types: they have different lengths: " + responseKeys.length + " <> "
                        + responseTypes.length);
        for (int i = 0; i < responseKeys.length; ++i) {
            m_extractFields.add(new ResponseHeaderItem(responseKeys[i], responseTypes[i], responseColumns[i]));
        }
        m_responseBodyColumn = settings.getString(BODY_COLUMN_NAME, DEFAULT_BODY_COLUMN_NAME);
        for (final EnablableUserConfiguration<UserConfiguration> euc : m_authorizationConfigurations) {
            final UserConfiguration uc = euc.getUserConfiguration();
            euc.setEnabled(settings.getBoolean(euc.getName() + ENABLED_SUFFIX, uc instanceof NoAuthentication));
            try {
                try {
                    final NodeSettingsRO base = settings.getNodeSettings(euc.getName());
                    uc.loadUserConfigurationForDialog(base, specs, credentialNames);
                } catch (InvalidSettingsException e) {
                    uc.loadUserConfigurationForDialog(null, specs, credentialNames);
                }
            } catch (NotConfigurableException e1) {
                throw new IllegalStateException(e1);
            }
        }
        m_followRedirects = settings.getBoolean(FOLLOW_REDIRECTS, DEFAULT_FOLLOW_REDIRECTS);
        m_timeoutInSeconds = settings.getInt(TIMEOUT, DEFAULT_TIMEOUT);
    }
}
