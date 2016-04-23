/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *   23. Jan. 2016. (Gabor Bakos): created
 */
package org.knime.rest.nodes.post;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.rest.nodes.common.RestNodeModel;
import org.knime.rest.nodes.common.RestSettings.ReferenceType;
import org.knime.rest.nodes.common.RestSettings.RequestHeaderKeyItem;

/**
 *
 * @author Gabor Bakos
 */
class RestPostNodeModel extends RestNodeModel<RestPostSettings> {
    /**
     *
     */
    public RestPostNodeModel() {
        super();
    }

    /**
     * @param request
     * @param row
     * @return
     */
    @Override
    protected Invocation invocation(final Builder request, final DataRow row,
        final DataTableSpec spec) {
        final RestPostSettings settings = getSettings();
        final int bodyColumn = spec == null ? -1 : spec.findColumnIndex(settings.getRequestBodyColumn());
        final MediaType mediaType = MediaType.valueOf((String)computeHeaderValue(row, spec,
            settings.getRequestHeaders().stream().filter(v -> "Content-Type".equals(v.getKey())).findAny()
                .orElse(new RequestHeaderKeyItem("Content-Type", "application/json", ReferenceType.Constant))));
        Entity<?> entity = Entity
            .entity(settings.isUseConstantRequestBody() ? createObjectFromString(settings.getConstantRequestBody())
                : createObjectFromCell(row.getCell(bodyColumn)), new Variant(mediaType, (String)null, "UTF-8"));
        return request.buildPost(entity);
    }

    /**
     * @param row
     * @param spec
     * @param headerItem
     * @return
     */
    private Object computeHeaderValue(final DataRow row, final DataTableSpec spec,
        final RequestHeaderKeyItem headerItem) {
        Object value;
        switch (headerItem.getKind()) {
            case Constant:
                value = headerItem.getValueReference();
                break;
            case Column:
                value = row.getCell(spec.findColumnIndex(headerItem.getValueReference())).toString();
                break;
            case FlowVariable:
                value = getAvailableInputFlowVariables().get(headerItem.getKey()).getValueAsString();
                break;
            case CredentialName:
                value = getCredentialsProvider().get(headerItem.getKey()).getLogin();
                break;
            case CredentialPassword:
                value = getCredentialsProvider().get(headerItem.getKey()).getPassword();
                break;
            default:
                throw new UnsupportedOperationException("Unknown: " + headerItem.getKind() + " in: " + headerItem);
        }
        return value;
    }

    /**
     * @param cell
     * @return
     */
    private Object createObjectFromCell(final DataCell cell) {
        // TODO Auto-generated method stub
        return cell.toString();
    }

    /**
     * @param string
     * @return
     */
    private Object createObjectFromString(final String string) {
        return string;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RestPostSettings createSettings() {
        return new RestPostSettings();
    }
}
