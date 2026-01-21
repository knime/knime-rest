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

package org.knime.rest.nodes.webpageretriever;

import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.ColumnNameValidation;
import org.knime.rest.nodes.common.webui.RestNodeParameters;
import org.knime.rest.nodes.webpageretriever.WebpageRetrieverNodeParameters.WebpageRetrieverNodeParametersModification;

/**
 * Node parameters for Webpage Retriever.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
@Modification(WebpageRetrieverNodeParametersModification.class)
final class WebpageRetrieverNodeParameters extends RestNodeParameters {

    static class WebpageRetrieverNodeParametersModification implements Modification.Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            group.find(ResponseBodyColumnModRef.class).removeAnnotation(Widget.class);
            group.find(ResponseHeaderPolicyModRef.class).removeAnnotation(Widget.class);
            group.find(ResponseHeadersModRef.class).removeAnnotation(Widget.class);
        }

    }

    @Layout(OutputSection.class)
    @Persist(configKey = "Output column name")
    @Widget(title = "Output column name", description = "The name of the created output column.")
    @TextInputWidget(patternValidation = ColumnNameValidation.class)
    String m_outputColumnName = "Document";

    @Layout(OutputSection.class)
    @Persist(configKey = "Output as XML")
    @Widget(title = "Output as XML", description = """
            If checked, the output will be an XML column containing the parsed HTML converted into XHTML.
            Otherwise, the output will be a String column containing the parsed HTML.
            """)
    boolean m_outputAsXML = true;

    @Layout(OutputSection.class)
    @Persist(configKey = "Replace relative URLS")
    @Widget(title = "Replace relative URLs with absolute URLs", description = """
            If checked, relative URLs in the HTML will be replaced by the absolute ones.
            This may simplify further processing.
            """)
    boolean m_replaceRelativeURLS = true;

    @Layout(OutputSection.class)
    @Persist(configKey = "Extract cookies")
    @Widget(title = "Extract cookies", description = """
            If checked, the cookies sent by the server are extracted from the response and appended as a list column.
            A missing value is appended if the server doesn't send cookies.
            """)
    @ValueReference(IsExtractCookiesEnabled.class)
    boolean m_extractCookies;

    static final class IsExtractCookiesEnabled implements BooleanReference {
    }

    @Layout(OutputSection.class)
    @Persist(configKey = "Cookie output column name")
    @Widget(title = "Cookie column name", description = """
            The name of the column containing a list of cookies in the output table.
            """)
    @TextInputWidget(patternValidation = ColumnNameValidation.class)
    @Effect(predicate = IsExtractCookiesEnabled.class, type = EffectType.SHOW)
    String m_cookieOutputColumnName = "Cookies";

}
