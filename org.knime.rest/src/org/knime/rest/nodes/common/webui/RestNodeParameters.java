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
 * ------------------------------------------------------------------------
 */

package org.knime.rest.nodes.common.webui;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArray;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.array.ArrayWidget.ElementLayout;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.ColumnNameValidation;
import org.knime.rest.nodes.common.proxy.ProxyMode;
import org.knime.rest.nodes.common.webui.CredentialsType.CredentialsTypePersistor;
import org.knime.rest.nodes.common.webui.RestAuthenticationParameters.RestAuthenticationParametersModification;
import org.knime.rest.util.InvalidURLPolicy;

/**
 * Node parameters for request nodes.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
public class RestNodeParameters implements NodeParameters {

    static final String CFG_LEGACY_TIMEOUT = "timeout";

    /**
     * Constructor.
     */
    public RestNodeParameters() {
        // intentionally left blank
    }

    /**
     * Modification for making the response body column name text input widget invisible.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public static class RemoveResponseBodyColumnNameModification implements Modification.Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            group.find(ResponseBodyColumnModRef.class).removeAnnotation(Widget.class);
        }

    }

    @Section(title = "API Connection")
    interface APIConnectionSection {
    }

    @Section(title = "Authentication")
    @After(APIConnectionSection.class)
    interface AuthenticationSection {
    }

    @Advanced
    @Section(title = "Proxy")
    @After(AdvancedConnectionOptionsSection.class)
    //@After(AuthenticationSection.class)
    // TODO: In the new design the proxy section is after Authentication and before Performance and Rate Control
    // But since the Proxy section is advanced, it will be for now shown at the end of the dialog until decided how
    // to handle advanced settings in such cases.
    interface ProxySection {

        /**
         * Proxy section until proxy authentication.
         *
         * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
         */
        interface ProxySectionUnitlProxyAuth {
        }

        /**
         * Proxy section from proxy authentication.
         *
         * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
         */
        @After(ProxySectionUnitlProxyAuth.class)
        interface ProxySectionFromProxyAuth {
        }

    }

    @Section(title = "Performance and Rate Control")
    // @After(ProxySection.class)
    @After(AuthenticationSection.class)
    interface PerformanceAndRateControlSection {
    }

    @Advanced
    @Section(title = "Security and Certificates")
    //@After(PerformanceAndRateControlSection.class)
    // TODO: Similar to the proxy section.
    @After(ProxySection.class)
    interface SecurityAndCertificateSection {
    }

    @Section(title = "Request Headers")
    //@After(SecurityAndCertificateSection.class)
    @After(PerformanceAndRateControlSection.class)
    interface RequestHeaderSection {
    }

    /**
     * Request body section used in PUT, POST, PATCH nodes.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    @Section(title = "Request Body")
    @After(RequestHeaderSection.class)
    protected interface RequestBodySection {
    }

    @Section(title = "Timing, Retries and Errors")
    @After(RequestBodySection.class)
    interface TimingRetriesAndErrorsSection {

        /**
         * Layout for retry on server errors settings.
         *
         * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
         */
        @HorizontalLayout
        interface RetryOnServerErrorsLayout {
        }

    }

    /**
     * Output section of the request nodes.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    @Section(title = "Output")
    @After(TimingRetriesAndErrorsSection.class)
    public interface OutputSection {
    }

    @Advanced
    @Section(title = "Advanced Connection Options")
    @After(OutputSection.class)
    interface AdvancedConnectionOptionsSection {
    }

    @Layout(APIConnectionSection.class)
    @Widget(title = "Request URL", description = """
            Select a constant URL or a column from the input table that contains the URLs that you want to request.
            """)
    @Persistor(URLModePersistor.class)
    @ValueSwitchWidget
    @ValueReference(URLModeRef.class)
    URLMode m_urlMode = URLMode.CONSTANT;

    @Layout(APIConnectionSection.class)
    @Widget(title = "URL", description = "The URL for the GET request.")
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @Effect(predicate = IsConstantURLMode.class, type = EffectType.SHOW)
    @Persist(configKey = "Constant URI")
    String m_constantURL = "https://www.google.com";

    @Layout(APIConnectionSection.class)
    @Widget(title = "URL column", description = "Column containing the URLs for the requests.")
    @ChoicesProvider(URLColumnsProvider.class)
    @Effect(predicate = IsColumnURLMode.class, type = EffectType.SHOW)
    @Persist(configKey = "URI column")
    @ValueProvider(URLColumnProvider.class)
    @ValueReference(URLColumnRef.class)
    String m_urlColumn;

    @Layout(APIConnectionSection.class)
    @TextMessage(UrlColumnInputSummary.class)
    @Effect(predicate = IsColumnURLMode.class, type = EffectType.SHOW)
    Void m_urlColumnInputSummary;

    // TODO: The advanced settings which are not entire sections and belong to visible section are for now listed at the
    // end of the dialog until it is decided how to integrate advanced settings into their respective sections.
    @Layout(AdvancedConnectionOptionsSection.class)
    //@Layout(APIConnectionSection.class)
    @Widget(title = "Follow redirects", description = """
            If checked, the node will follow redirects (HTTP status code 3xx).
            """)
    @Persist(configKey = "follow redirects")
    boolean m_followRedirects = true;

    @Layout(AuthenticationSection.class)
    @TextMessage(AuthenticationInputPortSummary.class)
    Void m_authenticationInputPortSummary;

    @Layout(AuthenticationSection.class)
    @Widget(title = "Authentication type", description = "The type of the used authentication.")
    @Persistor(RestAuthenticationTypePersistor.class)
    @ValueReference(RestAuthenticationTypeRef.class)
    @Effect(predicate = IsAuthViaInputPortEnabled.class, type = EffectType.HIDE)
    RestAuthenticationType m_authType;

    @Layout(AuthenticationSection.class)
    @Persistor(NoneAuthPersistor.class)
    Void m_noneAuthParameters;

    @Layout(AuthenticationSection.class)
    @PersistWithin({"Basic"})
    @Persist(configKey = "BASIC auth")
    BasicAuth m_basicAuth = new BasicAuth();

    static final class BasicAuth implements NodeParameters {

        @Persistor(BasicAuthCredentialsTypePersistor.class)
        @Widget(title = "Credentials type", description = "The type of credentials used for authentication.")
        @ValueSwitchWidget
        @ValueReference(BasicAuthCredentialsTypeRef.class)
        @Effect(predicate = IsBasicAuth.class, type = EffectType.SHOW)
        CredentialsType m_basicAuthcredentialsType = CredentialsType.MANUAL;

        @PersistWithin.PersistEmbedded
        @Modification(BasicAuthParametersModification.class)
        RestAuthenticationParameters m_basicAuthParameters = new RestAuthenticationParameters();

        static final class BasicAuthParametersModification extends RestAuthenticationParametersModification {

            BasicAuthParametersModification() {
                super(RequiresFlowVariableCredential.class,
                    RequiresCredential.class,
                    RequiresUsernameProvider.class,
                    RequiresPasswordProvider.class);
            }

        }

        static final class BasicAuthCredentialsTypeRef implements ParameterReference<CredentialsType>{
        }

        static final class IsManualBasicAuthCredentialInput implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(BasicAuthCredentialsTypeRef.class).isOneOf(CredentialsType.MANUAL);
            }

        }

        static final class IsBasicAuth implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(RestAuthenticationTypeRef.class).isOneOf(RestAuthenticationType.BASIC_AUTH)
                        .and(not(i.getPredicate(IsAuthViaInputPortEnabled.class)));
            }

        }

        static final class RequiresCredential implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getPredicate(IsBasicAuth.class)
                        .and(i.getPredicate(IsManualBasicAuthCredentialInput.class));
            }

        }

        static final class RequiresUsernameProvider extends AuthenticationTypeDependentProvider {

            @Override
            public Boolean computeState(final NodeParametersInput context) {
                return m_typeSupplier.get() == RestAuthenticationType.BASIC_AUTH;
            }

        }

        static final class RequiresPasswordProvider extends AuthenticationTypeDependentProvider {

            @Override
            public Boolean computeState(final NodeParametersInput context) {
                return m_typeSupplier.get() == RestAuthenticationType.BASIC_AUTH;
            }

        }

        static final class RequiresFlowVariableCredential implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getPredicate(IsBasicAuth.class)
                        .and(not(i.getPredicate(IsManualBasicAuthCredentialInput.class)));
            }

        }

        static final class BasicAuthCredentialsTypePersistor extends CredentialsTypePersistor {

            protected BasicAuthCredentialsTypePersistor() {
                super(AuthenticationType.USER_PWD);
            }

        }

        BasicAuth() {
        }

    }

    @Layout(AuthenticationSection.class)
    @PersistWithin({"Bearer"})
    @Persist(configKey = "Bearer auth")
    BearerAuth m_bearerAuth = new BearerAuth();

    static final class BearerAuth implements NodeParameters {

        @Persistor(BearerAuthCredentialsTypePersistor.class)
        @Widget(title = "Credentials type", description = "The type of credentials used for authentication.")
        @ValueSwitchWidget
        @WidgetInternal(hideControlInNodeDescription =
            RestAuthenticationParameters.HIDE_CONTROL_IN_NODE_DESCRIPTION_REASON)
        @ValueReference(DigestAuthCredentialsTypeRef.class)
        @Effect(predicate = IsBearerAuth.class, type = EffectType.SHOW)
        CredentialsType m_bearerAuthcredentialsType = CredentialsType.MANUAL;

        @PersistWithin.PersistEmbedded
        @Modification(BearerAuthParametersModification.class)
        RestAuthenticationParameters m_bearerAuthParameters = new RestAuthenticationParameters();

        static final class BearerAuthParametersModification extends RestAuthenticationParametersModification {

            BearerAuthParametersModification() {
                super(RequiresFlowVariableCredential.class,
                    RequiresCredential.class,
                    RequiresUsernameProvider.class,
                    RequiresPasswordProvider.class,
                    "The flow variable containing the credentials which are used for bearer authorization.",
                    "Token",
                    "The bearer token used for authorization.",
                    false);
            }

        }

        static final class DigestAuthCredentialsTypeRef implements ParameterReference<CredentialsType>{
        }

        static final class IsManualBearerAuthCredentialInput implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(DigestAuthCredentialsTypeRef.class).isOneOf(CredentialsType.MANUAL);
            }

        }

        static final class IsBearerAuth implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(RestAuthenticationTypeRef.class).isOneOf(RestAuthenticationType.BEARER_TOKEN)
                        .and(not(i.getPredicate(IsAuthViaInputPortEnabled.class)));
            }

        }

        static final class RequiresCredential implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getPredicate(IsBearerAuth.class)
                        .and(i.getPredicate(IsManualBearerAuthCredentialInput.class));
            }

        }

        static final class RequiresUsernameProvider extends AuthenticationTypeDependentProvider {

            @Override
            public Boolean computeState(final NodeParametersInput context) {
                return false;
            }

        }

        static final class RequiresPasswordProvider extends AuthenticationTypeDependentProvider {

            @Override
            public Boolean computeState(final NodeParametersInput context) {
                return m_typeSupplier.get() == RestAuthenticationType.BEARER_TOKEN;
            }

        }

        static final class RequiresFlowVariableCredential implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getPredicate(IsBearerAuth.class)
                        .and(not(i.getPredicate(IsManualBearerAuthCredentialInput.class)));
            }

        }

        static final class BearerAuthCredentialsTypePersistor extends CredentialsTypePersistor {

            protected BearerAuthCredentialsTypePersistor() {
                super(AuthenticationType.PWD);
            }

        }

        BearerAuth() {
        }

    }

    @Layout(AuthenticationSection.class)
    @PersistWithin({"Digest"})
    @Persist(configKey = "Digest auth")
    DigestAuth m_digestAuth = new DigestAuth();

    static final class DigestAuth implements NodeParameters {

        @Persistor(DigestAuthCredentialsTypePersistor.class)
        @Widget(title = "Credentials type", description = "The type of credentials used for authentication.")
        @WidgetInternal(hideControlInNodeDescription =
        RestAuthenticationParameters.HIDE_CONTROL_IN_NODE_DESCRIPTION_REASON)
        @ValueSwitchWidget
        @ValueReference(DigestAuthCredentialsTypeRef.class)
        @Effect(predicate = IsDigestAuth.class, type = EffectType.SHOW)
        CredentialsType m_digestAuthcredentialsType = CredentialsType.MANUAL;

        @PersistWithin.PersistEmbedded
        @Modification(DigestAuthParametersModification.class)
        RestAuthenticationParameters m_digestAuthParameters = new RestAuthenticationParameters();

        static final class DigestAuthParametersModification extends RestAuthenticationParametersModification {

            DigestAuthParametersModification() {
                super(RequiresFlowVariableCredential.class,
                    RequiresCredential.class,
                    RequiresUsernameProvider.class,
                    RequiresPasswordProvider.class,
                    true);
            }

        }

        static final class DigestAuthCredentialsTypeRef implements ParameterReference<CredentialsType>{
        }

        static final class IsManualDigestAuthCredentialInput implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(DigestAuthCredentialsTypeRef.class).isOneOf(CredentialsType.MANUAL);
            }

        }

        static final class IsDigestAuth implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(RestAuthenticationTypeRef.class).isOneOf(RestAuthenticationType.DIGEST_AUTH)
                        .and(not(i.getPredicate(IsAuthViaInputPortEnabled.class)));
            }

        }

        static final class RequiresCredential implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getPredicate(IsDigestAuth.class)
                        .and(i.getPredicate(IsManualDigestAuthCredentialInput.class));
            }

        }

        static final class RequiresUsernameProvider extends AuthenticationTypeDependentProvider {

            @Override
            public Boolean computeState(final NodeParametersInput context) {
                return m_typeSupplier.get() == RestAuthenticationType.DIGEST_AUTH;
            }

        }

        static final class RequiresPasswordProvider extends AuthenticationTypeDependentProvider {

            @Override
            public Boolean computeState(final NodeParametersInput context) {
                return m_typeSupplier.get() == RestAuthenticationType.DIGEST_AUTH;
            }

        }

        static final class RequiresFlowVariableCredential implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getPredicate(IsDigestAuth.class)
                        .and(not(i.getPredicate(IsManualDigestAuthCredentialInput.class)));
            }

        }

        static final class DigestAuthCredentialsTypePersistor extends CredentialsTypePersistor {

            protected DigestAuthCredentialsTypePersistor() {
                super(AuthenticationType.USER_PWD);
            }

        }

        DigestAuth() {
        }

    }

    @Layout(AuthenticationSection.class)
    @PersistWithin({"NTLM (Labs)"})
    @Persist(configKey = "NTLM-auth")
    NTLMAuth m_ntlmAuth = new NTLMAuth();

    static final class NTLMAuth implements NodeParameters {

        @Persistor(NTLMAuthCredentialsTypePersistor.class)
        @Widget(title = "Credentials type", description = "The type of credentials used for authentication.")
        @WidgetInternal(hideControlInNodeDescription =
            RestAuthenticationParameters.HIDE_CONTROL_IN_NODE_DESCRIPTION_REASON)
        @ValueSwitchWidget
        @ValueReference(NTLMAuthCredentialsTypeRef.class)
        @Effect(predicate = IsNTLMAuth.class, type = EffectType.SHOW)
        CredentialsType m_ntlmAuthcredentialsType = CredentialsType.MANUAL;

        @PersistWithin.PersistEmbedded
        @Modification(NTLMAuthParametersModification.class)
        RestAuthenticationParameters m_ntlmAuthParameters = new RestAuthenticationParameters();

        static final class NTLMAuthParametersModification extends RestAuthenticationParametersModification {

            NTLMAuthParametersModification() {
                super(RequiresFlowVariableCredential.class,
                    RequiresCredential.class,
                    RequiresUsernameProvider.class,
                    RequiresPasswordProvider.class,
                    true);
            }

        }

        static final class NTLMAuthCredentialsTypeRef implements ParameterReference<CredentialsType>{
        }

        static final class IsManualNTLMAuthCredentialInput implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(NTLMAuthCredentialsTypeRef.class).isOneOf(CredentialsType.MANUAL);
            }

        }

        static final class RequiresCredential implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getPredicate(IsNTLMAuth.class)
                        .and(i.getPredicate(IsManualNTLMAuthCredentialInput.class));
            }

        }

        static final class RequiresUsernameProvider extends AuthenticationTypeDependentProvider {

            @Override
            public Boolean computeState(final NodeParametersInput context) {
                return m_typeSupplier.get() == RestAuthenticationType.NTLM_AUTH;
            }

        }

        static final class RequiresPasswordProvider extends AuthenticationTypeDependentProvider {

            @Override
            public Boolean computeState(final NodeParametersInput context) {
                return m_typeSupplier.get() == RestAuthenticationType.NTLM_AUTH;
            }

        }

        static final class RequiresFlowVariableCredential implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getPredicate(IsNTLMAuth.class)
                        .and(not(i.getPredicate(IsManualNTLMAuthCredentialInput.class)));
            }

        }

        static final class NTLMAuthCredentialsTypePersistor extends CredentialsTypePersistor {

            protected NTLMAuthCredentialsTypePersistor() {
                super(AuthenticationType.USER_PWD);
            }

        }

        NTLMAuth() {
        }

    }

    @Layout(AuthenticationSection.class)
    @PersistWithin({"NTLM (Labs)"})
    @Persist(configKey = "org.knime.rest.auth.ntlm.domain")
    @Widget(title = "Domain", description = "The Windows domain used for NTLM authentication.")
    @Effect(predicate = IsNTLMAuth.class, type = EffectType.SHOW)
    String m_domain = "";

    @Layout(AuthenticationSection.class)
    @Persistor(KerberosAuthPersistor.class)
    Void m_kerberosAuthParameters;

    @Layout(ProxySection.ProxySectionUnitlProxyAuth.class)
    @Widget(title = "Proxy mode", description = "Specifies how the proxy should be configured.")
    @ValueReference(ProxyModeRef.class)
    @Persistor(ProxyModePersistor.class)
    ProxyMode m_proxyMode = ProxyMode.GLOBAL;

    @Persistor(NodeSpecificProxyPersistor.class)
    @ValueProvider(NodeSpecificProxyValueProvider.class)
    @ValueReference(NodeSpecificProxyRef.class)
    NodeSpecificProxyParameters m_nodeSpecificProxyParameters = new NodeSpecificProxyParameters();

    static final class NodeSpecificProxyParameters implements NodeParameters {

        // This parameter is only used to copy the value of the proxy mode from the outer parameters class
        // which we need in the persistor to determine whether to persist the proxy settings or not
        ProxyMode m_proxyModeCopy;

        @Layout(ProxySection.ProxySectionUnitlProxyAuth.class)
        @Widget(title = "Proxy protocol", description = """
                This option describes the proxy protocol to use. HTTP, HTTPS and SOCKS can be selected.
                """)
        @ValueSwitchWidget
        @Effect(predicate = IsLocalProxyMode.class, type = EffectType.SHOW)
        ProxyProtocol m_proxyProtocol = ProxyProtocol.HTTP;

        @Layout(ProxySection.ProxySectionUnitlProxyAuth.class)
        @Widget(title = "Proxy host", description = "Specifies the proxy host address.")
        @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
        @Effect(predicate = IsLocalProxyMode.class, type = EffectType.SHOW)
        String m_proxyHost;

        @Layout(ProxySection.ProxySectionUnitlProxyAuth.class)
        @Widget(title = "Proxy port", description = "Specifies the port that should be used at the proxy host.")
        @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
        @Effect(predicate = IsLocalProxyMode.class, type = EffectType.SHOW)
        int m_proxyPort = 8080;

        @Layout(ProxySection.ProxySectionUnitlProxyAuth.class)
        @Widget(title = "Use proxy authentication", description = "Enable if the proxy requires authentication.")
        @ValueReference(UseProxyAuthRef.class)
        @Effect(predicate = IsLocalProxyMode.class, type = EffectType.SHOW)
        boolean m_useProxyAuth;

        @Layout(ProxySection.ProxySectionFromProxyAuth.class)
        @Widget(title = "Exclude hosts from proxy", description = """
                Enable to specify hosts that will be ignored by the proxy. Requests to excluded hosts will use a
                direct connection.
                """)
        @ValueReference(UseProxyExcludeHostsRef.class)
        @Effect(predicate = IsLocalProxyMode.class, type = EffectType.SHOW)
        boolean m_useProxyExcludeHosts;

        @Layout(ProxySection.ProxySectionFromProxyAuth.class)
        @Widget(title = "Selected hosts", description = "List of hosts that should be ignored by the proxy.")
        @ArrayWidget(elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE, addButtonText = "Add host",
            elementDefaultValueProvider = ProxyExcludedHostsDefaultProvider.class)
        @ValueReference(ProxyExcludedHostsRef.class)
        @ValueProvider(ProxyExcludedHostsProvider.class)
        @Effect(predicate = IsProxyExcludeHostsRequired.class, type = EffectType.SHOW)
        ProxyExcludedHosts[] m_proxyExcludedHosts = new ProxyExcludedHosts[0];

        NodeSpecificProxyParameters() {
        }

        NodeSpecificProxyParameters(final ProxyMode proxyMode, final ProxyProtocol protocol, final String host,
            final int port, final boolean useAuth, final boolean useProxyExcludeHosts,
            final ProxyExcludedHosts[] excludedHosts) {
            m_proxyModeCopy = proxyMode;
            m_proxyProtocol = protocol;
            m_proxyHost = host;
            m_proxyPort = port;
            m_useProxyAuth = useAuth;
            m_useProxyExcludeHosts = useProxyExcludeHosts;
            m_proxyExcludedHosts = excludedHosts;
        }

    }

    @Layout(ProxySection.ProxySectionUnitlProxyAuth.class)
    @Persist(configKey = "Proxy auth")
    ProxyAuth m_proxyAuth = new ProxyAuth();

    static final class ProxyAuth implements NodeParameters {

        @Persistor(ProxyAuthCredentialsTypePersistor.class)
        @Widget(title = "Credentials type", description = "The type of credentials used for proxy authentication.")
        @ValueSwitchWidget
        @ValueReference(ProxyAuthCredentialsTypeRef.class)
        @Effect(predicate = IsProxyAuthRequired.class, type = EffectType.SHOW)
        CredentialsType m_proxyAuthcredentialsType = CredentialsType.MANUAL;

        @PersistWithin.PersistEmbedded
        @Modification(ProxyAuthParametersModification.class)
        RestAuthenticationParameters m_proxyAuthParameters = new RestAuthenticationParameters();

        static final class ProxyAuthParametersModification extends RestAuthenticationParametersModification {

            ProxyAuthParametersModification() {
                super(IsProxyAuthRequiredAndVariableInput.class,
                    RequiresCredential.class,
                    RequiresUsernameProvider.class,
                    RequiresPasswordProvider.class,
                    "The flow variable containing the credentials which are used for proxy authentication.",
                    "The user name and password for proxy authentication.");
            }

        }

        static final class ProxyAuthCredentialsTypeRef implements ParameterReference<CredentialsType>{
        }

        static final class IsManualProxyAuthCredentialInput implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(ProxyAuthCredentialsTypeRef.class).isOneOf(CredentialsType.MANUAL);
            }

        }

        static final class IsProxyAuthRequired implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getPredicate(IsLocalProxyMode.class)
                    .and(i.getBoolean(UseProxyAuthRef.class).isTrue());
            }

        }

        static final class RequiresCredential implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getPredicate(IsProxyAuthRequired.class)
                        .and(i.getPredicate(IsManualProxyAuthCredentialInput.class));
            }

        }

        static final class RequiresUsernameProvider extends ProxyAuthDependentProvider {

            @Override
            public Boolean computeState(final NodeParametersInput context) {
                return m_isProxyAuthEnabled.get();
            }

        }

        static final class RequiresPasswordProvider extends ProxyAuthDependentProvider {

            @Override
            public Boolean computeState(final NodeParametersInput context) {
                return m_isProxyAuthEnabled.get();
            }

        }

        static final class IsProxyAuthRequiredAndManualInput implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getPredicate(IsProxyAuthRequired.class)
                    .and(i.getPredicate(IsManualProxyAuthCredentialInput.class));
            }

        }

        static final class IsProxyAuthRequiredAndVariableInput implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getPredicate(IsProxyAuthRequired.class)
                    .and(not(i.getPredicate(IsManualProxyAuthCredentialInput.class)));
            }

        }

        static final class ProxyAuthCredentialsTypePersistor extends CredentialsType.CredentialsTypePersistor {

            protected ProxyAuthCredentialsTypePersistor() {
                super(AuthenticationType.USER_PWD);
            }

        }

        ProxyAuth() {
        }

    }

    @Layout(PerformanceAndRateControlSection.class)
    @Widget(title = "Concurrency", description = "Number of concurrent requests.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Persist(configKey = "concurrency")
    int m_concurrency = 1;

    @Layout(PerformanceAndRateControlSection.class)
    @Widget(title = "Enable delay",
        description = "Enable delay between consecutive requests to avoid overloading the web service.")
    @Persist(configKey = "delay_enabled")
    @ValueReference(UseDelayRef.class)
    boolean m_useDelay;

    @Layout(PerformanceAndRateControlSection.class)
    @Widget(title = "Delay (ms)", description = "Delay between two consecutive requests in milliseconds.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Persist(configKey = "delay")
    @Effect(predicate = UseDelayPredicate.class, type = EffectType.SHOW)
    long m_delay;

    @Layout(AdvancedConnectionOptionsSection.class)
    //@Layout(PerformanceAndRateControlSection.class)
    @Widget(title = "Connect timeout (s)", description = """
            The connection timeout is the timeout in making the initial connection. In case of HTTPS, this
            includes completing the SSL handshake. This timeout is set in seconds.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Persist(configKey = "connectTimeout")
    @Migration(ConnectionTimeoutMigration.class)
    int m_connectTimeoutInSeconds = 5;

    static final class ConnectionTimeoutMigration implements NodeParametersMigration<Integer> {

        static Integer loadTimeout(final NodeSettingsRO settings) {
            return settings.getInt(CFG_LEGACY_TIMEOUT, 5);
        }

        @Override
        public List<ConfigMigration<Integer>> getConfigMigrations() {
            return List.of(
                ConfigMigration.builder(settings -> 5).withMatcher(settings->
                    !settings.containsKey(CFG_LEGACY_TIMEOUT) && !settings.containsKey("readTimeout")).build(),
                ConfigMigration.builder(ConnectionTimeoutMigration::loadTimeout)
                .withDeprecatedConfigPath(CFG_LEGACY_TIMEOUT).build());
        }

    }

    @Layout(AdvancedConnectionOptionsSection.class)
    //@Layout(PerformanceAndRateControlSection.class)
    @Widget(title = "Read timeout (s)", description = """
            The read timeout is the time to wait until the first byte of data is read. Increasing this timeout
            makes sense if you have a slow connection or you expect the server will take a long time to prepare
            your response. This timeout is set in seconds.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Persist(configKey = "readTimeout")
    @Migration(ReadTimeoutMigration.class)
    int m_readTimeoutInSeconds = 120;

    static final class ReadTimeoutMigration implements NodeParametersMigration<Integer> {

        static Integer loadTimeout(final NodeSettingsRO settings) {
            return settings.getInt(CFG_LEGACY_TIMEOUT, 120);
        }

        @Override
        public List<ConfigMigration<Integer>> getConfigMigrations() {
            return List.of(
                ConfigMigration.builder(settings -> 120).withMatcher(settings->
                    !settings.containsKey(CFG_LEGACY_TIMEOUT) && !settings.containsKey("readTimeout")).build(),
                ConfigMigration.builder(ConnectionTimeoutMigration::loadTimeout)
                .withDeprecatedConfigPath(CFG_LEGACY_TIMEOUT).build());
        }

    }

    @Layout(AdvancedConnectionOptionsSection.class)
    //@Layout(PerformanceAndRateControlSection.class)
    @Widget(title = "Send large data in chunks", description = """
            Specifies whether HTTP Chunked Transfer Encoding is allowed to be used by the node. If enabled,
            messages with a large body size are being sent to the server in a series of chunks.
            """)
    @Persist(configKey = "allowChunking")
    boolean m_allowChunking = true;

    @Layout(SecurityAndCertificateSection.class)
    @Widget(title = "Ignore hostname mismatches", description = """
            If checked, the node trusts the server's SSL certificate even if it was generated for a different host.
            """)
    @Persist(configKey = "SSL ignore hostname errors")
    boolean m_sslIgnoreHostNameErrors;

    @Layout(SecurityAndCertificateSection.class)
    @Widget(title = "Trust all certificates", description = """
            If checked, the node trusts all certificates regardless of their origin or expiration date.
            """)
    @Persist(configKey = "SSL trust all")
    boolean m_sslTrustAll;

    @Layout(RequestHeaderSection.class)
    @Widget(title = "Request headers", description = """
            Define custom request headers. Each header consists of a key, value, and kind specification.
            """)
    @ArrayWidget(elementLayout = ElementLayout.VERTICAL_CARD, addButtonText = "Add header parameter")
    @PersistArray(RequestHeaderItem.RequestHeadersArrayPersistor.class)
    @ValueReference(RequestHeaderItemRef.class)
    RequestHeaderItem[] m_requestHeaders = new RequestHeaderItem[0];

    @Layout(RequestHeaderSection.class)
    @Widget(title = "Missing header value handling", description = """
            How to handle missing header input values.
            """)
    @Persistor(MissingHeaderValuePersistor.class)
    @ValueSwitchWidget
    @Migration(MissingHeaderValueMigration.class)
    MissingHeaderValue m_failOnMissingHeaders = MissingHeaderValue.FAIL;

    static final class MissingHeaderValueMigration implements DefaultProvider<MissingHeaderValue> {

        @Override
        public MissingHeaderValue getDefault() {
            return MissingHeaderValue.SKIP;
        }

    }

    @Layout(TimingRetriesAndErrorsSection.class)
    @Widget(title = "Handling of invalid URLs", description = """
            Specifies how invalid URLs are handled. For REST client nodes, all URLs conforming to
            <a href=\"https://www.rfc-editor.org/rfc/rfc1738\">RFC 1738</a> and using the HTTP or HTTPS protocol are
            considered valid.
            """)
    @ValueSwitchWidget
    @Persistor(InvalidURLPolicyPersistor.class)
    InvalidURLPolicy m_invalidURLPolicy = InvalidURLPolicy.MISSING;

    @Layout(TimingRetriesAndErrorsSection.class)
    @Widget(title = "Fail on connection problems (e.g. timeout, certificate errors, …)", description = """
            This option describes what should happen if there was a problem establishing the connection to the
            server. The node either fails in execution or outputs a missing value in the row of the output table.
            """)
    @ValueSwitchWidget
    @Persistor(FailOnConnectionProblemsPersistor.class)
    ErrorHandlingPolicy m_failOnConnectionProblems = ErrorHandlingPolicy.INSERT_MISSING_VALUE;

    // TODO Missing option for fail on 429 responses

    @Layout(TimingRetriesAndErrorsSection.class)
    @Widget(title = "Client-side errors (HTTP 4XX)", description = """
            This option describes what should happen if a response with a 4XX status code is received. These
            status codes usually describe client-side errors such as malformed requests.
            """)
    @ValueSwitchWidget
    @Persistor(FailOnClientErrorsPersistor.class)
    @Migration(FailOnServerOrClientErrorsMigration.class)
    ErrorHandlingPolicy m_failOnClientErrors = ErrorHandlingPolicy.INSERT_MISSING_VALUE;

    @Layout(TimingRetriesAndErrorsSection.class)
    @Widget(title = "Rate-limiting error handling (HTTP 429)", description = """
            Decide how to proceed when receiving rate limiting errors (HTTP 429).
            """)
    @ValueSwitchWidget
    @PersistWithin({"delayPolicy"})
    @Persistor(PauseOnRateLimitPersistor.class)
    @ValueReference(PauseOnRateLimitRef.class)
    RateLimitingRetryPolicy m_pauseOnRateLimit = RateLimitingRetryPolicy.FALSE;

    @Layout(TimingRetriesAndErrorsSection.class)
    @Widget(title = "Rate limiting cooldown (s)", description = """
            The cooldown delay in seconds to wait when receiving rate limiting errors (HTTP 429).
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @PersistWithin({"delayPolicy"})
    @Persist(configKey = "delayRateLimitCooldown")
    @Effect(predicate = IsPauseOnRateLimitEnabled.class, type = EffectType.SHOW)
    long m_rateLimitCooldownSeconds = 60L;

    // TODO Missing "Auto" option for rate limit responses

    // TODO Missing maximum wait time setting for for rate limit responses

    @Layout(TimingRetriesAndErrorsSection.class)
    @Widget(title = "Server-side errors (HTTP 5XX)", description = """
            This option describes what should happen if a response with a 5XX status code is received. These
            status codes usually describe errors on the server side.
            """)
    @ValueSwitchWidget
    @Persistor(FailOnServerErrorsPersistor.class)
    @Migration(FailOnServerOrClientErrorsMigration.class)
    ErrorHandlingPolicy m_failOnServerErrors = ErrorHandlingPolicy.INSERT_MISSING_VALUE;

    static final class FailOnServerOrClientErrorsMigration implements NodeParametersMigration<ErrorHandlingPolicy> {

        static ErrorHandlingPolicy loadPolicy(final NodeSettingsRO settings) {
            final var legacyFailOnMissingHeaders = settings.getBoolean("Fail on HTTP errors", false);
            if (legacyFailOnMissingHeaders) {
                return ErrorHandlingPolicy.FAIL_NODE;
            } else {
                return ErrorHandlingPolicy.INSERT_MISSING_VALUE;
            }
        }

        @Override
        public List<ConfigMigration<ErrorHandlingPolicy>> getConfigMigrations() {
            return List.of(
                ConfigMigration.builder(settings -> ErrorHandlingPolicy.INSERT_MISSING_VALUE).withMatcher(settings->
                    !settings.containsKey("Fail on HTTP errors") &&
                    (!settings.containsKey("failOnServerError") || !settings.containsKey("failOnClientError"))).build(),
                ConfigMigration.builder(FailOnServerOrClientErrorsMigration::loadPolicy)
                .withDeprecatedConfigPath("Fail on HTTP errors").build());
        }

    }

    @Layout(TimingRetriesAndErrorsSection.class)
    @Widget(title = "Retry on server errors",
        description = "Enable automatic retries for server-side errors (HTTP 5XX).")
    @PersistWithin({"delayPolicy"})
    @Persist(configKey = "delayRetriesEnabled")
    @ValueReference(RetryOnServerErrorsRef.class)
    boolean m_retryOnServerErrors;

    @Layout(TimingRetriesAndErrorsSection.RetryOnServerErrorsLayout.class)
    @Widget(title = "Number of retries", description = """
            Number of additional attempts that will be made after the initial request has failed.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @PersistWithin({"delayPolicy"})
    @Persist(configKey = "delayMaxRetries")
    @Effect(predicate = IsRetryEnabled.class, type = EffectType.SHOW)
    int m_maxRetries = 3;

    @Layout(TimingRetriesAndErrorsSection.RetryOnServerErrorsLayout.class)
    @Widget(title = "Retry delay (s)", description = """
            The base delay to be applied between retry attempts in seconds. The actual delay increases exponentially
            with each retry attempt.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @PersistWithin({"delayPolicy"})
    @Persist(configKey = "delayRetryBase")
    @Effect(predicate = IsRetryEnabled.class, type = EffectType.SHOW)
    long m_retryDelaySeconds = 1L;

    @Layout(OutputSection.class)
    @Widget(title = "Extracted request headers", description = """
            Extracts the defined headers in the first response into columns.
            """)
    @ValueSwitchWidget
    @Persistor(ExtractAllResponseFieldsPersistor.class)
    @ValueReference(ExtractAllResponseFieldsRef.class)
    @Modification.WidgetReference(ResponseHeaderPolicyModRef.class)
    ResponseHeaderPolicy m_extractAllResponseFields = ResponseHeaderPolicy.CUSTOM;

    /**
     * Reference to the response header policy widget for use in modifications.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public interface ResponseHeaderPolicyModRef extends
        ParameterReference<ResponseHeaderPolicy>, Modification.Reference {
    }

    @Layout(OutputSection.class)
    @Widget(title = "Response headers", description = """
            Define which response headers to extract into output columns.
            """)
    @ArrayWidget(elementLayout = ElementLayout.VERTICAL_CARD, addButtonText = "Add header output column",
        elementDefaultValueProvider = ResponseHeaderItemDefaultProvider.class)
    @PersistArray(ResponseHeaderItem.ResponseHeadersArrayPersistor.class)
    @ValueReference(ResponseHeaderItemRef.class)
    @Modification.WidgetReference(ResponseHeadersModRef.class)
    @Effect(predicate = IsExtractAllResponseHeadersPredicate.class, type = EffectType.HIDE)
    ResponseHeaderItem[] m_responseHeaders = new ResponseHeaderItem[0];

    /**
     * Reference to the response header widget for use in modifications.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public interface ResponseHeadersModRef extends ParameterReference<String>, Modification.Reference {
    }

    @Layout(OutputSection.class)
    @Widget(title = "Body column name", description = "Name of the response body column in the output table.")
    @TextInputWidget(patternValidation = ColumnNameValidation.class)
    @Persist(configKey = "Body column name")
    @Modification.WidgetReference(ResponseBodyColumnModRef.class)
    String m_responseBodyColumn = "body";

    /**
     * Reference to the response body column name widget for use in modifications.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public interface ResponseBodyColumnModRef extends ParameterReference<String>, Modification.Reference {
    }

    @Layout(OutputSection.class)
    @Widget(title = "Output additional column with error cause", description = """
            If enabled, each output row corresponding to a request will contain an additional cell that, in
            case the request has failed, will provide a description of the error cause.
            """)
    @Persist(configKey = "outputErrorCause")
    boolean m_outputErrorCause;

    // TODO Missing option to rename error cause column

    static final class URLModeRef implements ParameterReference<URLMode> {
    }

    static final class URLColumnRef implements ParameterReference<String> {
    }

    static final class RestAuthenticationTypeRef implements ParameterReference<RestAuthenticationType> {
    }

    static final class ProxyModeRef implements ParameterReference<ProxyMode> {
    }

    static final class NodeSpecificProxyRef implements ParameterReference<NodeSpecificProxyParameters> {
    }

    static final class UseProxyAuthRef implements ParameterReference<Boolean> {
    }

    static final class UseProxyExcludeHostsRef implements ParameterReference<Boolean> {
    }

    static final class ProxyExcludedHostsRef implements ParameterReference<ProxyExcludedHosts[]> {
    }

    static final class UseDelayRef implements ParameterReference<Boolean> {
    }

    static final class RequestHeaderItemRef implements ParameterReference<RequestHeaderItem[]> {
    }

    static final class PauseOnRateLimitRef implements ParameterReference<RateLimitingRetryPolicy> {
    }

    static final class RetryOnServerErrorsRef implements ParameterReference<Boolean> {
    }

    static final class ExtractAllResponseFieldsRef implements ParameterReference<ResponseHeaderPolicy> {
    }

    static final class ResponseHeaderItemRef implements ParameterReference<ResponseHeaderItem[]> {
    }

    static final class IsConstantURLMode implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(URLModeRef.class).isOneOf(URLMode.CONSTANT);
        }

    }

    static final class IsColumnURLMode implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(URLModeRef.class).isOneOf(URLMode.COLUMN);
        }

    }

    static final class IsAuthViaInputPortEnabled implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getConstant(parametersInput -> parametersInput.getInPortObjects().length != 1);
        }

    }

    static final class IsNTLMAuth implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(RestAuthenticationTypeRef.class).isOneOf(RestAuthenticationType.NTLM_AUTH)
                    .and(not(i.getPredicate(IsAuthViaInputPortEnabled.class)));
        }

    }

    static final class IsLocalProxyMode implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ProxyModeRef.class).isOneOf(ProxyMode.LOCAL);
        }

    }

    static final class IsProxyExcludeHostsRequired implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(IsLocalProxyMode.class).and(i.getBoolean(UseProxyExcludeHostsRef.class).isTrue());
        }

    }

    static final class UseDelayPredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(UseDelayRef.class).isTrue();
        }

    }

    static final class IsPauseOnRateLimitEnabled implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(PauseOnRateLimitRef.class).isOneOf(RateLimitingRetryPolicy.FIXED_DELAY);
        }

    }

    static final class IsRetryEnabled implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(RetryOnServerErrorsRef.class).isTrue();
        }

    }

    static final class IsExtractAllResponseHeadersPredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ExtractAllResponseFieldsRef.class).isOneOf(ResponseHeaderPolicy.ALL);
        }

    }

    static final class URLColumnsProvider implements ColumnChoicesProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            return ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(
                context, StringValue.class, URIDataValue.class);
        }

    }

    static final class URLColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected URLColumnProvider() {
            super(URLColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            return ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(
                parametersInput, StringValue.class, URIDataValue.class);
        }

    }

    abstract static class AuthenticationTypeDependentProvider implements StateProvider<Boolean> {

        protected Supplier<RestAuthenticationType> m_typeSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_typeSupplier = initializer.computeFromValueSupplier(RestAuthenticationTypeRef.class);
        }

    }

    static final class NodeSpecificProxyValueProvider implements StateProvider<NodeSpecificProxyParameters> {

        Supplier<ProxyMode> m_proxyModeSupplier;

        Supplier<NodeSpecificProxyParameters> m_nodeSpecificProxySupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_proxyModeSupplier = initializer.computeFromValueSupplier(ProxyModeRef.class);
            m_nodeSpecificProxySupplier =
                initializer.getValueSupplier(NodeSpecificProxyRef.class);
        }

        @Override
        public NodeSpecificProxyParameters computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (m_proxyModeSupplier.get() != ProxyMode.LOCAL) {
                return null;
            }

            final var nodeSpecificProxy = m_nodeSpecificProxySupplier.get();
            if (nodeSpecificProxy != null && nodeSpecificProxy.m_proxyHost != null) {
                return nodeSpecificProxy;
            }

            return new NodeSpecificProxyParameters(ProxyMode.LOCAL, ProxyProtocol.HTTP, "localhost", 8080, false,
                false, new ProxyExcludedHosts[] {new ProxyExcludedHosts("localhost|127.0.0.1")});
        }

    }

    abstract static class ProxyAuthDependentProvider implements StateProvider<Boolean> {

        protected Supplier<Boolean> m_isProxyAuthEnabled;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_isProxyAuthEnabled = initializer.computeFromValueSupplier(UseProxyAuthRef.class);
        }

    }

    static final class ProxyExcludedHostsDefaultProvider implements StateProvider<ProxyExcludedHosts> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public ProxyExcludedHosts computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return new ProxyExcludedHosts("");
        }

    }

    static final class ProxyExcludedHostsProvider implements StateProvider<ProxyExcludedHosts[]> {

        Supplier<ProxyExcludedHosts[]> m_excludedHosts;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_excludedHosts = initializer.getValueSupplier(ProxyExcludedHostsRef.class);
        }

        @Override
        public ProxyExcludedHosts[] computeState(final NodeParametersInput context)
            throws StateComputationFailureException {
            final var excludedHosts = m_excludedHosts.get();
            if (excludedHosts == null || excludedHosts.length > 0) {
                return excludedHosts;
            } else {
                return new ProxyExcludedHosts[] {new ProxyExcludedHosts()};
            }
        }

    }

    static final class RequestHeaderItemsDefaultProvider implements StateProvider<RequestHeaderItem> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public RequestHeaderItem computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return new RequestHeaderItem();
        }

    }

    static final class ResponseHeaderItemDefaultProvider implements StateProvider<ResponseHeaderItem> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public ResponseHeaderItem computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return new ResponseHeaderItem();
        }

    }

    static final class URLModePersistor extends EnumBooleanPersistor<URLMode> {

        protected URLModePersistor() {
            super("Use constant URI", URLMode.class, URLMode.CONSTANT);
        }

    }

    static final class RestAuthenticationTypePersistor implements NodeParametersPersistor<RestAuthenticationType> {

        @Override
        public RestAuthenticationType load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getBoolean("None_enabled", false)) {
                return RestAuthenticationType.NONE;
            }
            if (settings.getBoolean("Basic_enabled", false)) {
                return RestAuthenticationType.BASIC_AUTH;
            }
            if (settings.getBoolean("Bearer_enabled", false)) {
                return RestAuthenticationType.BEARER_TOKEN;
            }
            if (settings.getBoolean("Digest_enabled", false)) {
                return RestAuthenticationType.DIGEST_AUTH;
            }
            if (settings.getBoolean("NTLM (Labs)_enabled", false)) {
                return RestAuthenticationType.NTLM_AUTH;
            }
            if (settings.getBoolean("Kerberos_enabled", false)) {
                return RestAuthenticationType.KERBEROS;
            }
            return RestAuthenticationType.NONE;
        }

        @Override
        public void save(final RestAuthenticationType param, final NodeSettingsWO settings) {
            saveRestAuthenticationType(settings, param);
        }

        private static void saveRestAuthenticationType(
            final NodeSettingsWO settings, final RestAuthenticationType authType) {
            settings.addBoolean("None_enabled", authType == RestAuthenticationType.NONE);
            settings.addBoolean("Basic_enabled", authType == RestAuthenticationType.BASIC_AUTH);
            settings.addBoolean("Bearer_enabled", authType == RestAuthenticationType.BEARER_TOKEN);
            settings.addBoolean("Digest_enabled", authType == RestAuthenticationType.DIGEST_AUTH);
            settings.addBoolean("NTLM (Labs)_enabled", authType == RestAuthenticationType.NTLM_AUTH);
            settings.addBoolean("Kerberos_enabled", authType == RestAuthenticationType.KERBEROS);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"None_enabled"},
                {"Basic_enabled"},
                {"Bearer_enabled"},
                {"Digest_enabled"},
                {"NTLM (Labs)_enabled"},
                {"Kerberos_enabled"}};
        }

    }

    static final class NoneAuthPersistor implements NodeParametersPersistor<Void> {

        @Override
        public Void load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return null;
        }

        @Override
        public void save(final Void param, final NodeSettingsWO settings) {
            settings.addNodeSettings("None");
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{};
        }

    }

    static final class KerberosAuthPersistor implements NodeParametersPersistor<Void> {

        @Override
        public Void load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return null;
        }

        @Override
        public void save(final Void param, final NodeSettingsWO settings) {
            settings.addNodeSettings("Kerberos");
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{};
        }

    }

    static final class ProxyModePersistor implements NodeParametersPersistor<ProxyMode> {

        @Override
        public ProxyMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return ProxyMode.fromSettings(settings);
        }

        @Override
        public void save(final ProxyMode value, final NodeSettingsWO settings) {
            switch (value) {
                case GLOBAL -> settings.addBoolean("Proxy_enabled", true);
                case LOCAL -> settings.addBoolean("Proxy_enabled", true);
                case NONE -> settings.addBoolean("Proxy_enabled", false);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"Proxy_enabled"}};
        }

    }

    static final class NodeSpecificProxyPersistor implements NodeParametersPersistor<NodeSpecificProxyParameters> {

        static final String CFG_KEY_NODE_SPECIFIC_PROXY = "Node-specific Proxy";

        @Override
        public NodeSpecificProxyParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            try {
                final var proxyMode = ProxyMode.fromSettings(settings);
                if (proxyMode != ProxyMode.LOCAL) {
                    return null;
                }
                final var nodeSpecificSettings = settings.getNodeSettings(CFG_KEY_NODE_SPECIFIC_PROXY);
                final var excludedHostsString = nodeSpecificSettings.getString("Proxy-excluded hosts", "");
                String[] excludedHosts;
                if (excludedHostsString == null || excludedHostsString.trim().isEmpty()) {
                    excludedHosts = new String[]{""};
                } else {
                    excludedHosts = excludedHostsString.split(";");
                }
                ProxyExcludedHosts[] proxyExcludedHosts = new ProxyExcludedHosts[excludedHosts.length];
                if (excludedHostsString == null || excludedHostsString.trim().isEmpty()) {
                    proxyExcludedHosts = new ProxyExcludedHosts[]{new ProxyExcludedHosts("localhost|127.0.0.1")};
                } else {
                    excludedHosts = excludedHostsString.split(";");
                    for (int i = 0; i < excludedHosts.length; i++) {
                        proxyExcludedHosts[i] = new ProxyExcludedHosts(excludedHosts[i].trim());
                    }
                }
                return new NodeSpecificProxyParameters(proxyMode, ProxyProtocol.getFromValue(
                    nodeSpecificSettings.getString("Proxy protocol", ProxyProtocol.HTTP.name())),
                    nodeSpecificSettings.getString("Proxy host", "localhost"),
                    nodeSpecificSettings.getInt("Proxy port", 8080),
                    nodeSpecificSettings.getBoolean("Use proxy authentication", false),
                    nodeSpecificSettings.getBoolean("Exclude hosts from proxy", false),
                    proxyExcludedHosts);
            } catch (InvalidSettingsException e) {
                return null;
            }
        }

        @Override
        public void save(final NodeSpecificProxyParameters param, final NodeSettingsWO settings) {
            if (param != null && param.m_proxyModeCopy == ProxyMode.LOCAL) {
                final var nodeSpecificSettings = settings.addConfig(CFG_KEY_NODE_SPECIFIC_PROXY);
                nodeSpecificSettings.addString("Proxy protocol", param.m_proxyProtocol.name());
                nodeSpecificSettings.addString("Proxy host", param.m_proxyHost);
                nodeSpecificSettings.addInt("Proxy port", param.m_proxyPort);
                nodeSpecificSettings.addBoolean("Use proxy authentication", param.m_useProxyAuth);
                nodeSpecificSettings.addBoolean("Exclude hosts from proxy", param.m_useProxyExcludeHosts);
                nodeSpecificSettings.addString("Proxy-excluded hosts",
                    param.m_proxyExcludedHosts != null ? String.join(";", Arrays.stream(param.m_proxyExcludedHosts)
                        .map(excludedHosts -> excludedHosts.m_host).toArray(String[]::new)) : "");
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{
                {CFG_KEY_NODE_SPECIFIC_PROXY, "Proxy protocol"},
                {CFG_KEY_NODE_SPECIFIC_PROXY, "Proxy host"},
                {CFG_KEY_NODE_SPECIFIC_PROXY, "Proxy port"},
                {CFG_KEY_NODE_SPECIFIC_PROXY, "Use proxy authentication"},
                {CFG_KEY_NODE_SPECIFIC_PROXY, "Exclude hosts from proxy"},
                {CFG_KEY_NODE_SPECIFIC_PROXY, "Proxy-excluded hosts"}};
        }

    }

    static final class MissingHeaderValuePersistor extends EnumBooleanPersistor<MissingHeaderValue> {

        protected MissingHeaderValuePersistor() {
            super("failOnMissingHeaders", MissingHeaderValue.class, MissingHeaderValue.FAIL);
        }

    }

    static final class PauseOnRateLimitPersistor extends EnumBooleanPersistor<RateLimitingRetryPolicy> {

        protected PauseOnRateLimitPersistor() {
            super("delayCooldownEnabled", RateLimitingRetryPolicy.class, RateLimitingRetryPolicy.FIXED_DELAY);
        }

    }

    static final class InvalidURLPolicyPersistor implements NodeParametersPersistor<InvalidURLPolicy> {

        @Override
        public InvalidURLPolicy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return InvalidURLPolicy.getFromValue(
                settings.getString("Invalid URL handling", InvalidURLPolicy.MISSING.name()));
        }

        @Override
        public void save(final InvalidURLPolicy value, final NodeSettingsWO settings) {
            settings.addString("Invalid URL handling", value.name());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"Invalid URL handling"}};
        }

    }

    static final class FailOnConnectionProblemsPersistor extends EnumBooleanPersistor<ErrorHandlingPolicy> {

        protected FailOnConnectionProblemsPersistor() {
            super("Fail on connection problems", ErrorHandlingPolicy.class, ErrorHandlingPolicy.FAIL_NODE);
        }

    }

    static final class FailOnClientErrorsPersistor extends EnumBooleanPersistor<ErrorHandlingPolicy> {

        protected FailOnClientErrorsPersistor() {
            super("failOnClientError", ErrorHandlingPolicy.class, ErrorHandlingPolicy.FAIL_NODE);
        }

    }

    static final class FailOnServerErrorsPersistor extends EnumBooleanPersistor<ErrorHandlingPolicy> {

        protected FailOnServerErrorsPersistor() {
            super("failOnServerError", ErrorHandlingPolicy.class, ErrorHandlingPolicy.FAIL_NODE);
        }

    }

    static final class ExtractAllResponseFieldsPersistor extends EnumBooleanPersistor<ResponseHeaderPolicy> {

        protected ExtractAllResponseFieldsPersistor() {
            super("Extract all response fields", ResponseHeaderPolicy.class, ResponseHeaderPolicy.ALL);
        }

    }

    static final class UrlColumnInputSummary implements StateProvider<Optional<TextMessage.Message>> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Optional<TextMessage.Message> computeState(final NodeParametersInput context)
            throws StateComputationFailureException {
            final var compatibleUrlColumns = ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(
                context, StringValue.class, URIDataValue.class);

            if (!compatibleUrlColumns.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new TextMessage.Message("No input table connected or missing URL column.",
                "Connect an input table which contains a URL column", TextMessage.MessageType.INFO));
        }

    }

    static final class AuthenticationInputPortSummary implements StateProvider<Optional<TextMessage.Message>> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Optional<TextMessage.Message> computeState(final NodeParametersInput context)
            throws StateComputationFailureException {

            if (context.getInPortObjects().length == 1) {
                return Optional.empty();
            }

            return Optional.of(new TextMessage.Message("Authentication managed by Credential Input Port.",
                "Remove the Credential Input Port to use other authentication methods", TextMessage.MessageType.INFO));
        }

    }

    static final class NoTableInputSummary implements StateProvider<Optional<TextMessage.Message>> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Optional<TextMessage.Message> computeState(final NodeParametersInput context)
            throws StateComputationFailureException {
            final var compatibleColumns = ColumnSelectionUtil.getAllColumnsOfFirstPort(context);

            if (!compatibleColumns.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new TextMessage.Message("No input table connected.",
                "Connect an input table first.", TextMessage.MessageType.INFO));
        }

    }

    static final class ProxyExcludedHosts implements NodeParameters {

        @Widget(title = "Excluded host", description = "Host that should be ignored by the proxy.")
        String m_host;

        public ProxyExcludedHosts() {
        }

        public ProxyExcludedHosts(final String host) {
            m_host = host;
        }
    }

    enum URLMode {

        @Label("Constant URL")
        CONSTANT, //
        @Label("URL from column")
        COLUMN;

    }

    enum ProxyProtocol {

        @Label("HTTP")
        HTTP, //
        @Label("HTTPS")
        HTTPS, //
        @Label("SOCKS")
        SOCKS;

        static ProxyProtocol getFromValue(final String value) throws InvalidSettingsException {
            for (final ProxyProtocol condition : values()) {
                if (condition.name().equals(value)) {
                    return condition;
                }
            }
            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
        }

        private static String createInvalidSettingsExceptionMessage(final String name) {
            var values = List.of(HTTP.name(), HTTPS.name(), SOCKS.name()).stream().collect(
                Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", name, values);
        }

    }

    enum MissingHeaderValue {

        @Label(value = "Fail", description = """
                The node fails if any of the specified headers are missing in the response.
                """)
        FAIL, //
        @Label(value = "Skip", description = """
                The node skips the row if any of the specified headers are missing in the response.
                """)
        SKIP;

    }

    enum RateLimitingRetryPolicy {

        @Label(value = "Fail node", description = "The node execution fails.")
        FALSE, //
        @Label(value = "Fixed delay", description = """
            Pauses execution for a fixed delay before retrying the request.
            """)
        FIXED_DELAY;

    }

    enum ErrorHandlingPolicy {

        @Label(value = "Insert missing value", description = """
            The node inserts a missing value in the output table for the corresponding request.
            """)
        INSERT_MISSING_VALUE, //
        @Label(value = "Fail node", description = "The node execution fails.")
        FAIL_NODE;

    }

    enum ResponseHeaderPolicy {

        @Label(value = "Custom", description = "Extract only the custom defined headers into output columns.")
        CUSTOM, //
        @Label(value = "All", description = "Extract all response headers into output columns.")
        ALL;

    }

}
