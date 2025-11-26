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
import java.util.stream.Collectors;

import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ArrayPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ElementFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArrayElement;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.widget.choices.Label;

/**
 * {@link NodeParameters} representing the response headers.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class ResponseHeaderItem implements NodeParameters {

    @Widget(title = "Header key", description = "The name of the header to extract.")
    @PersistArrayElement(ResponseHeaderKeyPersistor.class)
    String m_headerKey = "";

    @HorizontalLayout
    interface ColumnNameAndTypeLayout {
    }

    @Layout(ColumnNameAndTypeLayout.class)
    @Widget(title = "Column name", description = "The name of the output column.")
    @PersistArrayElement(ResponseHeaderColumnNamePersistor.class)
    String m_outputColumnName = "";

    @Layout(ColumnNameAndTypeLayout.class)
    @Widget(title = "Header type", description = "The KNIME data type the header should be converted into.")
    @PersistArrayElement(ResponseHeaderTypePersistor.class)
    DataTypeOptions m_type = DataTypeOptions.STRING;

    static final class ResponseHeaderKeyPersistor
        implements ElementFieldPersistor<String, Integer, ResponseHeaderItem> {

        @Override
        public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            final var keys = nodeSettings.getStringArray("Response header keys",
                ResponseHeadersArrayPersistor.DEFAULT_RESPONSE_HEADERS);
            return loadContext < keys.length ? keys[loadContext] : "";
        }

        @Override
        public void save(final String param, final ResponseHeaderItem saveDTO) {
            saveDTO.m_headerKey = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"Response header keys"}};
        }

    }

    static final class ResponseHeaderColumnNamePersistor
        implements ElementFieldPersistor<String, Integer, ResponseHeaderItem> {

        @Override
        public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            final var columnNames = nodeSettings.getStringArray("Response header column name",
                ResponseHeadersArrayPersistor.DEFAULT_RESPONSE_HEADER_COLUMN_NAMES);
            return loadContext < columnNames.length ? columnNames[loadContext] : "";
        }

        @Override
        public void save(final String param, final ResponseHeaderItem saveDTO) {
            saveDTO.m_outputColumnName = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"Response header column name"}};
        }

    }

    static final class ResponseHeaderTypePersistor
        implements ElementFieldPersistor<DataTypeOptions, Integer, ResponseHeaderItem> {

        @Override
        public DataTypeOptions load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            final var types = nodeSettings.getDataTypeArray("Response header column type",
                ResponseHeadersArrayPersistor.DEFAULT_RESPONSE_HEADER_COLUMN_TYPES);
            if (loadContext < types.length) {
                return types[loadContext].equals(IntCell.TYPE) ? DataTypeOptions.INT : DataTypeOptions.STRING;
            }
            return DataTypeOptions.STRING;
        }

        @Override
        public void save(final DataTypeOptions param, final ResponseHeaderItem saveDTO) {
            saveDTO.m_type = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"Response header column type"}};
        }

    }

    ResponseHeaderItem() {
    }

    ResponseHeaderItem(final String headerKey, final String outputColumnName, final DataTypeOptions type) {
        m_headerKey = headerKey;
        m_outputColumnName = outputColumnName;
        m_type = type;
    }

    static final class ResponseHeadersArrayPersistor implements ArrayPersistor<Integer, ResponseHeaderItem> {

        static final String[] DEFAULT_RESPONSE_HEADERS = new String[]{"Status", "Content-Type"};

        static final String[] DEFAULT_RESPONSE_HEADER_COLUMN_NAMES = new String[]{"Status", "Content type"};

        static final DataType[] DEFAULT_RESPONSE_HEADER_COLUMN_TYPES =
                new DataType[]{IntCell.TYPE, StringCell.TYPE};

        @Override
        public int getArrayLength(final NodeSettingsRO nodeSettings) {
            final var keys = nodeSettings.getStringArray("Response header keys", DEFAULT_RESPONSE_HEADERS);
            return keys.length;
        }

        @Override
        public Integer createElementLoadContext(final int index) {
            return index;
        }

        @Override
        public ResponseHeaderItem createElementSaveDTO(final int index) {
            return new ResponseHeaderItem();
        }

        @Override
        public void save(final List<ResponseHeaderItem> savedElements, final NodeSettingsWO nodeSettings) {
            final var keys = new String[savedElements.size()];
            final var columnNames = new String[savedElements.size()];
            final var types = new DataType[savedElements.size()];

            for (int i = 0; i < savedElements.size(); i++) {
                final var item = savedElements.get(i);
                keys[i] = item.m_headerKey;
                columnNames[i] = item.m_outputColumnName;
                types[i] = item.m_type == DataTypeOptions.INT ? IntCell.TYPE : StringCell.TYPE;
            }

            nodeSettings.addStringArray("Response header keys", keys);
            nodeSettings.addStringArray("Response header column name", columnNames);
            nodeSettings.addDataTypeArray("Response header column type", types);
        }
    }

    enum DataTypeOptions {

        @Label("String")
        STRING(StringCell.TYPE.getName()),

        @Label("Number (Integer)")
        INT(IntCell.TYPE.getName());

        private final String m_value;

        DataTypeOptions(final String value) {
            m_value = value;
        }

        String getValue() {
            return m_value;
        }

        static DataTypeOptions getFromValue(final String value) throws InvalidSettingsException {
            for (final DataTypeOptions condition : values()) {
                if (condition.getValue().equals(value)) {
                    return condition;
                }
            }
            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
        }

        private static String createInvalidSettingsExceptionMessage(final String name) {
            var values =
                List.of(IntCell.TYPE.getName(), StringCell.TYPE.getName()).stream().collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", name, values);
        }

    }

}
