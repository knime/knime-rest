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
 *   Nov 24, 2025 (magnus): created
 */
package org.knime.rest.nodes.common.webui;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ArrayPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ElementFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArrayElement;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;
import org.knime.rest.nodes.common.RestSettings.ReferenceType;

/**
 * {@link NodeParameters} representing the request headers.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class RequestHeaderItem implements NodeParameters {

    static final String CFG_HEADERS_KEY = "Request header keys";

    static final String CFG_HEADERS_KEY_SELECTOR = "Request header key selector";

    static final String CFG_HEADERS_KIND = "Request header key kind";

    static final String HIDE_CONTROL_IN_NODE_DESCRIPTION =
        "Multiple different header value parameter inputs couldn't be organized in an enum";

    @Widget(title = "Header key", description = """
            The HTTP header key, e.g. <tt>Accept</tt> or <tt>X-Custom-Key</tt>. Note that some
            header keys such as <tt>Origin</tt> are silently ignored by default for security reasons. You can configure
            KNIME AP to allow any header key by setting the <tt>sun.net.http.allowRestrictedHeaders</tt> system property
             in the <i>knime.ini</i> configuration file to <tt>true</tt>.
            """)
    @PersistArrayElement(RequestHeaderKeyPersistor.class)
    String m_key = "";

    @Widget(title = "Type", description = """
            The type of the value specified.
            """)
    @ValueReference(ReferenceTypeRef.class)
    @PersistArrayElement(RequestHeaderKindPersistor.class)
    ReferenceType m_kind = ReferenceType.Constant;

    @Widget(title = "Value", description = """
            The value for the header which can be a constant value or a reference to a flow
            variable, a column, a credential name, or a credential password (see the kind option).
            """)
    @Effect(predicate = IsConstantReference.class, type = EffectType.SHOW)
    @PersistArrayElement(RequestHeaderConstantValuePersistor.class)
    String m_constantValueReference;

    @Widget(title = "Value", description = """
            The flow variable header value.
            """)
    @WidgetInternal(hideControlInNodeDescription = HIDE_CONTROL_IN_NODE_DESCRIPTION)
    @Effect(predicate = IsFlowVariableReference.class, type = EffectType.SHOW)
    @ChoicesProvider(HeaderFlowVariablesProvider.class)
    @PersistArrayElement(RequestHeaderFlowVariableValuePersistor.class)
    String m_flowVariableValueReference;

    @Widget(title = "Value", description = """
            The column header value.
            """)
    @WidgetInternal(hideControlInNodeDescription = HIDE_CONTROL_IN_NODE_DESCRIPTION)
    @Effect(predicate = IsColumnReference.class, type = EffectType.SHOW)
    @ChoicesProvider(StringColumnsProvider.class)
    @PersistArrayElement(RequestHeaderColumnValuePersistor.class)
    String m_columnValueReference;

    @Widget(title = "Value", description = """
            The credential header value.
            """)
    @WidgetInternal(hideControlInNodeDescription = HIDE_CONTROL_IN_NODE_DESCRIPTION)
    @Effect(predicate = IsCredentialReference.class, type = EffectType.SHOW)
    @ChoicesProvider(CredentialFlowVariablesProvider.class)
    @PersistArrayElement(RequestHeaderCredentialValuePersistor.class)
    String m_credentialValueReference;

    static final class ReferenceTypeRef implements ParameterReference<ReferenceType> {
    }

    static final class IsConstantReference implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(RequestHeaderItem.ReferenceTypeRef.class).isOneOf(ReferenceType.Constant);
        }

    }

    static final class IsFlowVariableReference implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(RequestHeaderItem.ReferenceTypeRef.class).isOneOf(ReferenceType.FlowVariable);
        }

    }

    static final class IsColumnReference implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(RequestHeaderItem.ReferenceTypeRef.class).isOneOf(ReferenceType.Column);
        }

    }

    static final class IsCredentialReference implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(RequestHeaderItem.ReferenceTypeRef.class)
                    .isOneOf(ReferenceType.CredentialName, ReferenceType.CredentialPassword);
        }

    }

    static final class HeaderFlowVariablesProvider implements FlowVariableChoicesProvider {

        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
            return context.getAvailableInputFlowVariables(BooleanType.INSTANCE, DoubleType.INSTANCE,
                IntType.INSTANCE, LongType.INSTANCE, StringType.INSTANCE)
                    .values().stream().toList();
        }

    }

    static final class CredentialFlowVariablesProvider implements FlowVariableChoicesProvider {

        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
            return context.getAvailableInputFlowVariables(VariableType.CredentialsType.INSTANCE)
                    .values().stream().toList();
        }

    }

    static final class RequestHeaderKeyPersistor
        implements ElementFieldPersistor<String, Integer, RequestHeaderItem> {

        @Override
        public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            final var keys = nodeSettings.getStringArray(CFG_HEADERS_KEY, new String[0]);
            return loadContext < keys.length ? keys[loadContext] : "";
        }

        @Override
        public void save(final String param, final RequestHeaderItem saveDTO) {
            saveDTO.m_key = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_HEADERS_KEY}};
        }

    }

    static final class RequestHeaderKindPersistor
        implements ElementFieldPersistor<ReferenceType, Integer, RequestHeaderItem> {

        @Override
        public ReferenceType load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            final var kinds = nodeSettings.getStringArray(CFG_HEADERS_KIND, new String[0]);
            if (loadContext < kinds.length) {
                return ReferenceType.getFromValue(kinds[loadContext]);
            }
            return ReferenceType.Constant;
        }

        @Override
        public void save(final ReferenceType param, final RequestHeaderItem saveDTO) {
            saveDTO.m_kind = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_HEADERS_KIND}};
        }

    }

    static final class RequestHeaderConstantValuePersistor
        implements ElementFieldPersistor<String, Integer, RequestHeaderItem> {

        @Override
        public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            final var selectors = nodeSettings.getStringArray(CFG_HEADERS_KEY_SELECTOR, new String[0]);
            final var kinds = nodeSettings.getStringArray(CFG_HEADERS_KIND, new String[0]);
            if (loadContext < selectors.length && loadContext < kinds.length) {
                final var kind = ReferenceType.getFromValue(kinds[loadContext]);
                return kind == ReferenceType.Constant ? selectors[loadContext] : null;
            }
            return null;
        }

        @Override
        public void save(final String param, final RequestHeaderItem saveDTO) {
            saveDTO.m_constantValueReference = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_HEADERS_KEY_SELECTOR}};
        }

    }

    static final class RequestHeaderFlowVariableValuePersistor
        implements ElementFieldPersistor<String, Integer, RequestHeaderItem> {

        @Override
        public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            final var selectors = nodeSettings.getStringArray(CFG_HEADERS_KEY_SELECTOR, new String[0]);
            final var kinds = nodeSettings.getStringArray(CFG_HEADERS_KIND, new String[0]);
            if (loadContext < selectors.length && loadContext < kinds.length) {
                final var kind = ReferenceType.getFromValue(kinds[loadContext]);
                return kind == ReferenceType.FlowVariable ? selectors[loadContext] : null;
            }
            return null;
        }

        @Override
        public void save(final String param, final RequestHeaderItem saveDTO) {
            saveDTO.m_flowVariableValueReference = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_HEADERS_KEY_SELECTOR}};
        }

    }

    static final class RequestHeaderColumnValuePersistor
        implements ElementFieldPersistor<String, Integer, RequestHeaderItem> {

        @Override
        public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            final var selectors = nodeSettings.getStringArray(CFG_HEADERS_KEY_SELECTOR, new String[0]);
            final var kinds = nodeSettings.getStringArray(CFG_HEADERS_KIND, new String[0]);
            if (loadContext < selectors.length && loadContext < kinds.length) {
                final var kind = ReferenceType.getFromValue(kinds[loadContext]);
                return kind == ReferenceType.Column ? selectors[loadContext] : null;
            }
            return null;
        }

        @Override
        public void save(final String param, final RequestHeaderItem saveDTO) {
            saveDTO.m_columnValueReference = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_HEADERS_KEY_SELECTOR}};
        }

    }

    static final class RequestHeaderCredentialValuePersistor
        implements ElementFieldPersistor<String, Integer, RequestHeaderItem> {

        @Override
        public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            final var selectors = nodeSettings.getStringArray(CFG_HEADERS_KEY_SELECTOR, new String[0]);
            final var kinds = nodeSettings.getStringArray(CFG_HEADERS_KIND, new String[0]);
            if (loadContext < selectors.length && loadContext < kinds.length) {
                final var kind = ReferenceType.getFromValue(kinds[loadContext]);
                return (kind == ReferenceType.CredentialName || kind == ReferenceType.CredentialPassword) ?
                    selectors[loadContext] : null;
            }
            return null;
        }

        @Override
        public void save(final String param, final RequestHeaderItem saveDTO) {
            saveDTO.m_credentialValueReference = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_HEADERS_KEY_SELECTOR}};
        }

    }

    RequestHeaderItem() {
        this("", ReferenceType.Constant, "");
    }

    RequestHeaderItem(final String key, final ReferenceType kind, final String value) {
        m_key = key;
        m_kind = kind;
        m_constantValueReference = kind == ReferenceType.Constant ? value : null;
        m_flowVariableValueReference = kind == ReferenceType.FlowVariable ? value : null;
        m_columnValueReference = kind == ReferenceType.Column ? value : null;
        m_credentialValueReference = kind == ReferenceType.CredentialName ||
                kind == ReferenceType.CredentialPassword ? value : null;
    }

    static final class RequestHeadersArrayPersistor implements ArrayPersistor<Integer, RequestHeaderItem> {

        @Override
        public int getArrayLength(final NodeSettingsRO nodeSettings) {
            final var keys = nodeSettings.getStringArray(CFG_HEADERS_KEY, new String[0]);
            return keys.length;
        }

        @Override
        public Integer createElementLoadContext(final int index) {
            return index;
        }

        @Override
        public RequestHeaderItem createElementSaveDTO(final int index) {
            return new RequestHeaderItem();
        }

        @Override
        public void save(final List<RequestHeaderItem> savedElements, final NodeSettingsWO nodeSettings) {
            final var keys = new String[savedElements.size()];
            final var selectors = new String[savedElements.size()];
            final var kinds = new String[savedElements.size()];

            for (int i = 0; i < savedElements.size(); i++) {
                final var item = savedElements.get(i);
                keys[i] = item.m_key;
                kinds[i] = item.m_kind.name();
                switch (item.m_kind) {
                    case Constant:
                        selectors[i] = item.m_constantValueReference;
                        break;
                    case FlowVariable:
                        selectors[i] = item.m_flowVariableValueReference;
                        break;
                    case Column:
                        selectors[i] = item.m_columnValueReference;
                        break;
                    case CredentialName:
                        selectors[i] = item.m_credentialValueReference;
                        break;
                    case CredentialPassword:
                        selectors[i] = item.m_credentialValueReference;
                        break;
                }
            }

            nodeSettings.addStringArray(CFG_HEADERS_KEY, keys);
            nodeSettings.addStringArray(CFG_HEADERS_KEY_SELECTOR, selectors);
            nodeSettings.addStringArray(CFG_HEADERS_KIND, kinds);
        }

    }

}
