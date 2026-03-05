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
 *   Mar 4, 2026 (magnus): created
 */
package org.knime.rest.nodes.common.webui;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.CredentialsWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.rest.nodes.common.webui.RestAuthenticationParameters.CredentialFlowVariablesProvider;
import org.knime.rest.nodes.common.webui.RestAuthenticationParameters.FlowVarNamePersistor;
import org.knime.rest.nodes.common.webui.RestNodeParameters.AuthenticationTypeDependentProvider;
import org.knime.rest.nodes.common.webui.RestNodeParameters.BearerAuth.BearerAuthCredentialsTypeRef;
import org.knime.rest.nodes.common.webui.RestNodeParameters.IsAuthViaInputPortEnabled;
import org.knime.rest.nodes.common.webui.RestNodeParameters.RestAuthenticationTypeRef;

/**
 * {@link NodeParameters} for the bearer authentication.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class BearerAuthenticationParameters implements NodeParameters {

    private static final String SECRET_KEY = "c-rH4Tkyk";

    @Persistor(FlowVarNamePersistor.class)
    @Widget(title = "Credentials variable", description = """
            The flow variable containing the credentials which are used for bearer authorization.
            """)
    @WidgetInternal(hideControlInNodeDescription = RestAuthenticationParameters.HIDE_CONTROL_IN_NODE_DESCRIPTION_REASON)
    @ChoicesProvider(CredentialFlowVariablesProvider.class)
    @Effect(predicate = RequiresFlowVariableCredential.class, type = EffectType.SHOW)
    String m_flowVarName;

    @Persistor(CredentialsPersistor.class)
    @Widget(title = "Token", description = "The bearer token used for authorization.")
    @CredentialsWidget
    @CredentialsWidgetInternal(hasUsernameProvider = RequiresUsernameProvider.class,
        hasPasswordProvider = RequiresPasswordProvider.class)
    @Effect(predicate = RequiresCredential.class, type = EffectType.SHOW)
    final Credentials m_credentials;

    static final class RequiresFlowVariableCredential implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(IsBearerAuth.class)
                    .and(not(i.getPredicate(IsManualBearerAuthCredentialInput.class)));
        }

    }

    static final class RequiresCredential implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(IsBearerAuth.class)
                    .and(i.getPredicate(IsManualBearerAuthCredentialInput.class));
        }

    }

    static final class IsManualBearerAuthCredentialInput implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(BearerAuthCredentialsTypeRef.class).isOneOf(CredentialsType.MANUAL);
        }

    }

    static final class IsBearerAuth implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(RestAuthenticationTypeRef.class).isOneOf(RestAuthenticationType.BEARER_TOKEN)
                    .and(not(i.getPredicate(IsAuthViaInputPortEnabled.class)));
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

    static final class CredentialsPersistor implements NodeParametersPersistor<Credentials> {

        @Override
        public Credentials load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var username = settings.getString(RestAuthenticationParameters.SETTINGS_MODEL_KEY_USERNAME, null);
            final var password = settings.getPassword(
                RestAuthenticationParameters.SETTINGS_MODEL_KEY_PASSWORD, SECRET_KEY, null);
            return new Credentials(username, password);
        }

        @Override
        public void save(final Credentials param, final NodeSettingsWO settings) {
            settings.addString(RestAuthenticationParameters.SETTINGS_MODEL_KEY_USERNAME, param.getUsername());
            settings.addPassword(
                RestAuthenticationParameters.SETTINGS_MODEL_KEY_PASSWORD, SECRET_KEY, param.getPassword());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{};
        }

    }

    BearerAuthenticationParameters() {
        this(null, new Credentials());
    }

    BearerAuthenticationParameters(final String flowVarName, final Credentials credential) {
        m_flowVarName = flowVarName;
        m_credentials = credential;
    }

}
