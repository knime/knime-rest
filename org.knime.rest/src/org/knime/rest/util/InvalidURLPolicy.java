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
 *   Feb 1, 2024 (lw): created
 */
package org.knime.rest.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.base.data.filter.row.FilterRowGenerator;
import org.knime.core.data.DataRow;
import org.knime.core.data.MissingCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.rest.nodes.common.RestNodeModel;

/**
 * Describes all options for invalid URL handling in REST client nodes.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
public enum InvalidURLPolicy implements ButtonGroupEnumInterface {
        /** Inserts a {@link MissingCell} */
        @Label(value = "Insert missing value", description = """
                The node inserts a missing value in the output table for the corresponding request.
                """)
        MISSING {
            @Override
            public String getText() {
                return "Insert missing value";
            }
        },
        /** Fails on encountering at least one invalid URL. */
        @Label(value = "Fail node", description = "The node execution fails.")
        FAIL {
            @Override
            public String getText() {
                return "Fail node execution immediately";
            }
        },
        /** Excludes this row in the output table. */
        @Label(value = "Remove row", description = """
                The node removes the row for the corresponding request.
                """)
        SKIP {
            @Override
            public String getText() {
                return "Omit row from output";
            }
        };

    /**
     * The error which indicates that a REST invocation could not be created
     * due to an invalid, supplied {@link URL}.
     */
    public static final String INVALID_URL_ERROR = "Invalid URL";

    /**
     * The default value of the invalid URL policy.
     */
    public static final InvalidURLPolicy DEFAULT_POLICY = InvalidURLPolicy.MISSING;

    private static final String SETTINGS_KEY = "Invalid URL handling";

    /**
     * Creates a settings model, representing this type.
     *
     * @return settings model, stores enum as string
     */
    public static SettingsModelString createSettingsModel() {
        return new SettingsModelString(SETTINGS_KEY, DEFAULT_POLICY.name());
    }

    /**
     * Identifies the URL policy from the settings.
     *
     * @param settings NodeSettings
     * @return InvalidURLPolicy
     */
    public static InvalidURLPolicy loadSettingsFrom(final NodeSettingsRO settings) {
        try {
            final var model = createSettingsModel();
            model.loadSettingsFrom(settings);
            return InvalidURLPolicy.valueOf(model.getStringValue());
        } catch (InvalidSettingsException | IllegalArgumentException ignored) { // NOSONAR
            /* Backwards compatibility (introduced with 5.3): if not present, invalid URLs insert missing. */
            return DEFAULT_POLICY;
        }
    }

    /**
     * Saves the enum value to the settings.
     *
     * @param settings NodeSettings
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        final var model = createSettingsModel();
        model.setStringValue(name());
        model.saveSettingsTo(settings);
    }

    @Override
    public String getActionCommand() {
        return name();
    }

    @Override
    public String getToolTip() {
        return null;
    }

    @Override
    public boolean isDefault() {
        return this == DEFAULT_POLICY;
    }

    /**
     * Returns the policy-corresponding {@link FilterRowGenerator} for data tables.
     *
     * @param columnIndex URL column index
     * @return row filter, filtering rows where an invalid URL was detected
     */
    public FilterRowGenerator createRowFilter(final int columnIndex) {
        if (this != SKIP) {
            return row -> true;
        }
        return row -> isValidURLPresent(row, columnIndex);
    }

    /**
     * Checks whether a valid HTTP(S) URL is present is the specified data cell by checking if retrieving
     * the {@link URL} object throws a {@link URLSyntaxException}.
     *
     * @param row data row
     * @param columnIndex index of the URL column
     * @return is a valid URL present?
     */
    private static boolean isValidURLPresent(final DataRow row, final int columnIndex) {
        try {
            RestNodeModel.getURLFromRow(row, columnIndex);
            return true;
        } catch (MalformedURLException ignored) { // NOSONAR presence of exception is indicator
            return false;
        }
    }

    /**
     * Retrieves the {@link InvalidURLPolicy} for the given string value.
     *
     * @param value the string value representation of the InvalidURLPolicy
     * @return {@link InvalidURLPolicy}
     * @throws InvalidSettingsException if the stringValue does not correspond to any InvalidURLPolicy
     *
     * @since 5.10
     */
    public static InvalidURLPolicy getFromValue(final String value) throws InvalidSettingsException {
        for (final InvalidURLPolicy condition : values()) {
            if (condition.name().equals(value)) {
                return condition;
            }
        }
        throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
    }

    private static String createInvalidSettingsExceptionMessage(final String name) {
        var values = List.of(MISSING.name(), FAIL.name(), SKIP.name()).stream().collect(Collectors.joining(", "));
        return String.format("Invalid value '%s'. Possible values: %s", name, values);
    }
}
