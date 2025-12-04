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
 *   Nov 25, 2025 (magnus): created
 */
package org.knime.rest.nodes.common.webui;

import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.rest.nodes.common.webui.RestNodeParameters.NoTableInputSummary;

/**
 * Node parameters for request body configuration.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class RequestBodyParameters implements NodeParameters {

    /**
     * Constructor.
     */
    public RequestBodyParameters() {
        // default constructor
    }

    @Widget(title = "Data / Body content", description = "The body of the request.")
    @Persistor(RequestBodyDataOrBodyContentTypePersistor.class)
    @ValueReference(DataOrBodyContentTypeRef.class)
    @ValueSwitchWidget
    @Migrate(loadDefaultIfAbsent=true)
    DataOrBodyContentType m_dataOrBodyContentType = DataOrBodyContentType.CUSTOM;

    @Widget(title = "Custom body content", description = "The custom body content used for the request body.")
    @TextAreaWidget(rows = 5)
    @Persist(configKey = "Constant request body")
    @Effect(predicate = IsConstantRequestBodyMode.class, type = EffectType.SHOW)
    @Migrate(loadDefaultIfAbsent=true)
    String m_constantRequestBody = "";

    @Widget(title = "Body column", description = "The column containing the body content for the request.")
    @Persist(configKey = "Request body column")
    @Effect(predicate = IsColumnRequestBodyMode.class, type = EffectType.SHOW)
    @ChoicesProvider(BodyColumnChoicesProvider.class)
    @ValueProvider(BodyColumnProvider.class)
    @ValueReference(BodyColumnRef.class)
    @Migrate(loadDefaultIfAbsent=true)
    String m_columnRequestBody;

    @TextMessage(NoTableInputSummary.class)
    @Effect(predicate = IsColumnRequestBodyMode.class, type = EffectType.SHOW)
    Void m_noColumnRequestBodySummary;

    static final class DataOrBodyContentTypeRef implements ParameterReference<DataOrBodyContentType>{
    }

    static final class BodyColumnRef implements ParameterReference<String> {
    }

    static final class BodyColumnChoicesProvider extends CompatibleColumnsProvider {

        protected BodyColumnChoicesProvider() {
            super(DataValue.class);
        }

    }

    static final class BodyColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected BodyColumnProvider() {
            super(BodyColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns =
                    ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, DataValue.class);
            return compatibleColumns.isEmpty() ?
                Optional.empty() : Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }

    }

    static final class IsConstantRequestBodyMode implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(DataOrBodyContentTypeRef.class).isOneOf(DataOrBodyContentType.CUSTOM);
        }

    }

    static final class IsColumnRequestBodyMode implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(DataOrBodyContentTypeRef.class).isOneOf(DataOrBodyContentType.COLUMN);
        }

    }

    static final class RequestBodyDataOrBodyContentTypePersistor extends EnumBooleanPersistor<DataOrBodyContentType> {

        protected RequestBodyDataOrBodyContentTypePersistor() {
            super("Use constant request body", DataOrBodyContentType.class, DataOrBodyContentType.CUSTOM);
        }

    }

    enum DataOrBodyContentType {

        @Label(value = "Custom", description = "Use constant body.")
        CUSTOM,
        @Label(value = "Column", description = "Use column's content as body.")
        COLUMN;

    }

}
