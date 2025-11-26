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
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
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
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.PasswordWidget;
import org.knime.node.parameters.widget.credentials.UsernameWidget;

/**
 * Node parameters class for rest authentication settings used in the rest request nodes.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class RestAuthenticationParameters implements NodeParameters {

    private final static String SECRET_KEY = "c-rH4Tkyk";

    static final String HIDE_CONTROL_IN_NODE_DESCRIPTION_REASON = """
                Hided because the different authentication settings would duplicate each other in the node description.
                """;

    abstract static class RestAuthenticationParametersModification implements Modification.Modifier {

        private Class<? extends EffectPredicateProvider> m_flowVarNameWidgetShowEffect;

        private Class<? extends EffectPredicateProvider> m_userNameWidgetShowEffect;

        private Class<? extends EffectPredicateProvider> m_passwordWidgetShowEffect;

        private String m_passwordTitle;

        private String m_passwordLabel;

        private String m_flowVarNameDescription;

        private String m_usernameDescription;

        private String m_passwordDescription;

        private boolean m_hideControlInNodeDescription;

        protected RestAuthenticationParametersModification(
            final Class<? extends EffectPredicateProvider> flowVarNameWidgetShowEffect,
            final Class<? extends EffectPredicateProvider> userNameWidgetShowEffect,
            final Class<? extends EffectPredicateProvider> passwordWidgetShowEffect,
            final String passwordTitle, final String passwordLabel,
            final String flowVarNameDescription, final String usernameDescription, final String passwordDescription,
            final boolean hideControlInNodeDescription) {
            m_flowVarNameWidgetShowEffect = flowVarNameWidgetShowEffect;
            m_userNameWidgetShowEffect = userNameWidgetShowEffect;
            m_passwordWidgetShowEffect = passwordWidgetShowEffect;
            m_passwordTitle = passwordTitle;
            m_passwordLabel = passwordLabel;
            m_flowVarNameDescription = flowVarNameDescription;
            m_usernameDescription = usernameDescription;
            m_passwordDescription = passwordDescription;
            m_hideControlInNodeDescription = hideControlInNodeDescription;
        }

        protected RestAuthenticationParametersModification(
            final Class<? extends EffectPredicateProvider> flowVarNameWidgetShowEffect,
            final Class<? extends EffectPredicateProvider> userNameWidgetShowEffect,
            final Class<? extends EffectPredicateProvider> passwordWidgetShowEffect,
            final boolean hideControlInNodeDescription) {
            this(flowVarNameWidgetShowEffect, userNameWidgetShowEffect, passwordWidgetShowEffect,
                null, null, null, null, null, hideControlInNodeDescription);
        }

        protected RestAuthenticationParametersModification(
            final Class<? extends EffectPredicateProvider> flowVarNameWidgetShowEffect,
            final Class<? extends EffectPredicateProvider> userNameWidgetShowEffect,
            final Class<? extends EffectPredicateProvider> passwordWidgetShowEffect,
            final String flowVarNameDescription, final String usernameDescription, final String passwordDescription) {
            this(flowVarNameWidgetShowEffect, userNameWidgetShowEffect, passwordWidgetShowEffect, null, null,
                flowVarNameDescription, usernameDescription, passwordDescription, false);
        }

        protected RestAuthenticationParametersModification(
            final Class<? extends EffectPredicateProvider> flowVarNameWidgetShowEffect,
            final Class<? extends EffectPredicateProvider> userNameWidgetShowEffect,
            final Class<? extends EffectPredicateProvider> passwordWidgetShowEffect) {
            this(flowVarNameWidgetShowEffect, userNameWidgetShowEffect, passwordWidgetShowEffect,
                null, null, null, null, null, false);
        }

        @Override
        public void modify(final WidgetGroupModifier group) {
            if (m_flowVarNameWidgetShowEffect != null) {
                group.find(FlowVariableCredentialsRef.class).addAnnotation(Effect.class)
                .withProperty("predicate", m_flowVarNameWidgetShowEffect)
                .withProperty("type", EffectType.SHOW)
                .modify();
            }

            CheckUtils.checkArgument(!(m_userNameWidgetShowEffect == null ^ m_passwordWidgetShowEffect == null), """
                If either a provider for the user name or password input fields is given
                the other one needs to be defined as well.
                """);
            if (m_userNameWidgetShowEffect != null && m_passwordWidgetShowEffect != null) {
                group.find(UsernameWidgetRef.class).addAnnotation(Effect.class)
                    .withProperty("predicate", m_userNameWidgetShowEffect)
                    .withProperty("type", EffectType.SHOW)
                    .modify();
                group.find(PasswordWidgetRef.class).addAnnotation(Effect.class)
                .withProperty("predicate", m_passwordWidgetShowEffect)
                .withProperty("type", EffectType.SHOW)
                .modify();
            }

            if (m_passwordTitle != null) {
                group.find(PasswordWidgetRef.class).modifyAnnotation(Widget.class)
                    .withProperty("title", m_passwordTitle).modify();
            }

            if (m_passwordLabel != null) {
                group.find(PasswordWidgetRef.class).modifyAnnotation(PasswordWidget.class)
                    .withProperty("passwordLabel", m_passwordLabel).modify();
            }
            if (m_flowVarNameDescription != null) {
                group.find(FlowVariableCredentialsRef.class).modifyAnnotation(Widget.class)
                    .withProperty("description", m_flowVarNameDescription).modify();
            }
            if (m_usernameDescription != null) {
                group.find(UsernameWidgetRef.class).modifyAnnotation(Widget.class)
                    .withProperty("description", m_usernameDescription).modify();
            }
            if (m_passwordDescription != null) {
                group.find(PasswordWidgetRef.class).modifyAnnotation(Widget.class)
                    .withProperty("description", m_passwordDescription).modify();
            }
            if (m_hideControlInNodeDescription) {
                group.find(FlowVariableCredentialsRef.class)
                    .addAnnotation(WidgetInternal.class)
                    .withProperty("hideControlInNodeDescription", HIDE_CONTROL_IN_NODE_DESCRIPTION_REASON)
                    .modify();
                group.find(UsernameWidgetRef.class)
                    .addAnnotation(WidgetInternal.class)
                    .withProperty("hideControlInNodeDescription", HIDE_CONTROL_IN_NODE_DESCRIPTION_REASON)
                    .modify();
                group.find(PasswordWidgetRef.class)
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

    @Persistor(UserNamePersistor.class)
    @Widget(title = "Username", description = "The user name for authentication.")
    @UsernameWidget
    @Modification.WidgetReference(UsernameWidgetRef.class)
    final Credentials m_username;

    @Persistor(PasswordPersistor.class)
    @Widget(title = "Password", description = "The password for authentication.")
    @PasswordWidget
    @Modification.WidgetReference(PasswordWidgetRef.class)
    final Credentials m_password;

    interface FlowVariableCredentialsRef extends ParameterReference<String>, Modification.Reference {
    }

    interface UsernameWidgetRef extends ParameterReference<String>, Modification.Reference {
    }

    interface PasswordWidgetRef extends ParameterReference<String>, Modification.Reference {
    }

    static final class CredentialFlowVariablesProvider implements FlowVariableChoicesProvider {

        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
            return context.getAvailableInputFlowVariables(VariableType.CredentialsType.INSTANCE)
                    .values().stream().toList();
        }

    }

    static final class UserNamePersistor implements NodeParametersPersistor<Credentials> {

        @Override
        public Credentials load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var username = settings.getString(SETTINGS_MODEL_KEY_USERNAME, null);
            final var password = settings.getPassword(SETTINGS_MODEL_KEY_PASSWORD, SECRET_KEY, null);
            if ((username == null || username.isEmpty()) && (password != null && !password.isEmpty())
                && !settings.getKey().equals("Bearer auth")) {
                throw new InvalidSettingsException("Username must not be empty.");
            }
            return new Credentials(username, null);
        }

        @Override
        public void save(final Credentials param, final NodeSettingsWO settings) {
            settings.addString(SETTINGS_MODEL_KEY_USERNAME, param.getUsername());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{SETTINGS_MODEL_KEY_USERNAME}};
        }

    }

    static final class PasswordPersistor implements NodeParametersPersistor<Credentials> {

        @Override
        public Credentials load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new Credentials(null, settings.getPassword(SETTINGS_MODEL_KEY_PASSWORD, SECRET_KEY, null));
        }

        @Override
        public void save(final Credentials param, final NodeSettingsWO settings) {
            settings.addPassword(SETTINGS_MODEL_KEY_PASSWORD, SECRET_KEY, param.getPassword());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{SETTINGS_MODEL_KEY_PASSWORD}};
        }

    }

    RestAuthenticationParameters() {
        this(null, new Credentials(), new Credentials());
    }

    RestAuthenticationParameters(final String flowVarName,
        final Credentials username, final Credentials password) {
        m_flowVarName = flowVarName;
        m_username = username;
        m_password = password;
    }

}
