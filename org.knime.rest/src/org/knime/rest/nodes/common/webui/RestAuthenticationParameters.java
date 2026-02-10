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
 *   Feb 26, 2024 (Paul Bärnreuther): created
 */
package org.knime.rest.nodes.common.webui;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.CredentialsWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;

/**
 * Node parameters class for rest authentication settings used in the rest request nodes.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class RestAuthenticationParameters implements NodeParameters {

    private static final String SECRET_KEY = "c-rH4Tkyk";

    static final String HIDE_CONTROL_IN_NODE_DESCRIPTION_REASON = """
                Hided because the different authentication settings would duplicate each other in the node description.
                """;

    abstract static class RestAuthenticationParametersModification implements Modification.Modifier {

        private Class<? extends EffectPredicateProvider> m_flowVarNameWidgetShowEffect;

        private Class<? extends EffectPredicateProvider> m_credentialWidgetShowEffect;

        private Class<? extends StateProvider<Boolean>> m_hasUserNameProvider;

        private Class<? extends StateProvider<Boolean>> m_hasPasswordProvider;

        private String m_flowVarNameDescription;

        private String m_passwordLabel;

        private String m_credentialDescription;

        private boolean m_hideControlInNodeDescription;

        protected RestAuthenticationParametersModification(
            final Class<? extends EffectPredicateProvider> flowVarNameWidgetShowEffect,
            final Class<? extends EffectPredicateProvider> credentialWidgetShowEffect,
            final Class<? extends StateProvider<Boolean>> userNameWidgetShowEffect,
            final Class<? extends StateProvider<Boolean>> passwordWidgetShowEffect,
            final String flowVarNameDescription, final String passwordLabel, final String credentialDescription,
            final boolean hideControlInNodeDescription) {
            m_flowVarNameWidgetShowEffect = flowVarNameWidgetShowEffect;
            m_credentialWidgetShowEffect = credentialWidgetShowEffect;
            m_hasUserNameProvider = userNameWidgetShowEffect;
            m_hasPasswordProvider = passwordWidgetShowEffect;
            m_credentialDescription = credentialDescription;
            m_passwordLabel = passwordLabel;
            m_flowVarNameDescription = flowVarNameDescription;
            m_hideControlInNodeDescription = hideControlInNodeDescription;
        }

        protected RestAuthenticationParametersModification(
            final Class<? extends EffectPredicateProvider> flowVarNameWidgetShowEffect,
            final Class<? extends EffectPredicateProvider> credentialWidgetShowEffect,
            final Class<? extends StateProvider<Boolean>> userNameWidgetShowEffect,
            final Class<? extends StateProvider<Boolean>> passwordWidgetShowEffect,
            final boolean hideControlInNodeDescription) {
            this(flowVarNameWidgetShowEffect, credentialWidgetShowEffect, userNameWidgetShowEffect,
                passwordWidgetShowEffect, null, null, null, hideControlInNodeDescription);
        }

        protected RestAuthenticationParametersModification(
            final Class<? extends EffectPredicateProvider> flowVarNameWidgetShowEffect,
            final Class<? extends EffectPredicateProvider> credentialWidgetShowEffect,
            final Class<? extends StateProvider<Boolean>> userNameWidgetShowEffect,
            final Class<? extends StateProvider<Boolean>> passwordWidgetShowEffect,
            final String flowVarNameDescription, final String credentialDescription) {
            this(flowVarNameWidgetShowEffect, credentialWidgetShowEffect, userNameWidgetShowEffect,
                passwordWidgetShowEffect, flowVarNameDescription, null, credentialDescription, false);
        }

        protected RestAuthenticationParametersModification(
            final Class<? extends EffectPredicateProvider> flowVarNameWidgetShowEffect,
            final Class<? extends EffectPredicateProvider> credentialWidgetShowEffect,
            final Class<? extends StateProvider<Boolean>> userNameWidgetShowEffect,
            final Class<? extends StateProvider<Boolean>> passwordWidgetShowEffect) {
            this(flowVarNameWidgetShowEffect, credentialWidgetShowEffect, userNameWidgetShowEffect,
                passwordWidgetShowEffect, null, null, null, false);
        }

        @Override
        public void modify(final WidgetGroupModifier group) {
            if (m_flowVarNameWidgetShowEffect != null) {
                group.find(FlowVariableCredentialsRef.class).addAnnotation(Effect.class)
                .withProperty("predicate", m_flowVarNameWidgetShowEffect)
                .withProperty("type", EffectType.SHOW)
                .modify();
            }

            if (m_credentialWidgetShowEffect != null) {
                group.find(CredentialWidgetRef.class).addAnnotation(Effect.class)
                .withProperty("predicate", m_credentialWidgetShowEffect)
                .withProperty("type", EffectType.SHOW)
                .modify();
            }

            CheckUtils.checkArgument(!(m_hasUserNameProvider == null ^ m_hasPasswordProvider == null), """
                If either a provider for the user name or password input fields is given
                the other one needs to be defined as well.
                """);
            if (m_hasUserNameProvider != null && m_hasPasswordProvider != null) {
                group.find(CredentialWidgetRef.class).addAnnotation(CredentialsWidgetInternal.class)
                    .withProperty("hasUsernameProvider", m_hasUserNameProvider)
                    .withProperty("hasPasswordProvider", m_hasPasswordProvider)
                    .modify();
            }

            if (m_flowVarNameDescription != null) {
                group.find(FlowVariableCredentialsRef.class).modifyAnnotation(Widget.class)
                .withProperty("description", m_flowVarNameDescription).modify();
            }
            if (m_passwordLabel != null) {
                group.find(CredentialWidgetRef.class).modifyAnnotation(CredentialsWidget.class)
                    .withProperty("passwordLabel", m_passwordLabel).modify();
            }
            if (m_credentialDescription != null) {
                group.find(CredentialWidgetRef.class).modifyAnnotation(Widget.class)
                    .withProperty("description", m_credentialDescription).modify();
            }
            if (m_hideControlInNodeDescription) {
                group.find(FlowVariableCredentialsRef.class)
                    .addAnnotation(WidgetInternal.class)
                    .withProperty("hideControlInNodeDescription", HIDE_CONTROL_IN_NODE_DESCRIPTION_REASON)
                    .modify();
                group.find(CredentialWidgetRef.class)
                    .addAnnotation(WidgetInternal.class)
                    .withProperty("hideControlInNodeDescription", HIDE_CONTROL_IN_NODE_DESCRIPTION_REASON)
                    .modify();
            }
        }

    }

    static final String SETTINGS_MODEL_KEY_CREDENTIAL = "credentials";

    static final String SETTINGS_MODEL_KEY_PASSWORD = "password";

    static final String SETTINGS_MODEL_KEY_USERNAME = "username";

    @Persist(configKey = SETTINGS_MODEL_KEY_CREDENTIAL)
    @Widget(title = "Credentials variable", description = """
            The flow variable containing the credentials which are used for authentication.
            """)
    @ChoicesProvider(CredentialFlowVariablesProvider.class)
    @Modification.WidgetReference(FlowVariableCredentialsRef.class)
    String m_flowVarName;

    @Persistor(CredentialsPersistor.class)
    @Widget(title = "Credentials", description = "The credentials used for the authentication.")
    @CredentialsWidget
    @Modification.WidgetReference(CredentialWidgetRef.class)
    final Credentials m_credentials;

    interface FlowVariableCredentialsRef extends ParameterReference<String>, Modification.Reference {
    }

    interface CredentialWidgetRef extends ParameterReference<Credentials>, Modification.Reference {
    }

    static final class CredentialFlowVariablesProvider implements FlowVariableChoicesProvider {

        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
            // can't just use 'context.getAvailableInputFlowVariables(VariableType.CredentialsType.INSTANCE)'
            // as this will not provide workflow variables (deprecated concept but still supported in migrations)
            if (context instanceof NodeParametersInputImpl impl) {
                return impl.getCredentialsProvider() //
                    .map(CredentialsProvider::listVariables) //
                    .map(List::copyOf) //
                    .orElse(List.of());
            }
            return context.getAvailableInputFlowVariables(VariableType.CredentialsType.INSTANCE)
                    .values().stream().toList();
        }

    }

    static final class CredentialsPersistor implements NodeParametersPersistor<Credentials> {

        @Override
        public Credentials load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var username = settings.getString(SETTINGS_MODEL_KEY_USERNAME, null);
            final var password = settings.getPassword(SETTINGS_MODEL_KEY_PASSWORD, SECRET_KEY, null);
            return new Credentials(username, password);
        }

        @Override
        public void save(final Credentials param, final NodeSettingsWO settings) {
            settings.addString(SETTINGS_MODEL_KEY_USERNAME, param.getUsername());
            settings.addPassword(SETTINGS_MODEL_KEY_PASSWORD, SECRET_KEY, param.getPassword());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{SETTINGS_MODEL_KEY_USERNAME}}; // AP-14067: Only possible to overwrite user name
        }

    }

    RestAuthenticationParameters() {
        this(null, new Credentials());
    }

    RestAuthenticationParameters(final String flowVarName, final Credentials credential) {
        m_flowVarName = flowVarName;
        m_credentials = credential;
    }

}
