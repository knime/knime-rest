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
 *   2016. �pr. 24. (Gabor Bakos): created
 */
package org.knime.rest.nodes.common;

import java.io.IOException;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.knime.base.data.xml.SvgValue;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.image.png.PNGImageValue;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.rest.nodes.common.RestSettings.ReferenceType;
import org.knime.rest.nodes.common.RestSettings.RequestHeaderKeyItem;
import org.w3c.dom.Document;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Variant;

/**
 * Base class for the REST nodes which require body content in their call.
 *
 * @author Gabor Bakos
 * @param <S> Type for the settings.
 */
public abstract class RestWithBodyNodeModel<S extends RestWithBodySettings> extends RestNodeModel<S> {

    /**
     * @param cfg The node creation configuration.
     */
    protected RestWithBodyNodeModel(final NodeCreationConfiguration cfg) {
        super(cfg);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final String requestBodyColumn = m_settings.getRequestBodyColumn();

        if (!m_settings.isUseConstantRequestBody()) {
            if (requestBodyColumn == null) {
                throw new InvalidSettingsException(
                    "The node is configured to use an empty request body column. Please change the configuration.");
            } else {
                final DataTableSpec dataTableSpec = inSpecs[0];
                if (dataTableSpec != null) {
                    if (!dataTableSpec.containsName(requestBodyColumn)) {
                        throw new InvalidSettingsException("The configured request body column '" + requestBodyColumn
                            + "' is missing in the input table.");
                    }
                } else {
                    throw new InvalidSettingsException(
                        "Input table required to execute. The node is configured to use a request body from the column "
                            + "'" + requestBodyColumn + "' of the input table.");
                }
            }
        }

        return super.configure(inSpecs);
    }

    /**
     * Converts a {@link DataCell} to an object consumable by the {@link Entity} constructor.
     *
     * @param cell A non-missing {@link DataCell}.
     * @return The converted object.
     */
    protected Object createObjectFromCell(final DataCell cell) {
        if (cell instanceof JSONValue) {
            JSONValue jv = (JSONValue)cell;
            if (cell instanceof StringValue) {
                StringValue sv = (StringValue)cell;
                return sv.getStringValue();
            }
            //CXF does not support the javax.json implementation, so we return as String (but currently all implementations are StringValues):
            return ((StringValue)JSONCellFactory.create(jv.getJsonValue())).getStringValue();
        }
        if (cell instanceof PNGImageValue) {
            PNGImageValue pngv = (PNGImageValue)cell;
            return pngv.getImageContent().getByteArray();
        }
        if (cell instanceof StringValue) {
            StringValue sv = (StringValue)cell;
            return sv.getStringValue();
        }
        if (cell instanceof SvgValue) {
            return ((SvgValue) cell).getDocumentSupplier().compute(doc -> doc.cloneNode(true));
        }
        if (cell instanceof XMLValue) {
            return ((XMLValue<Document>) cell).getDocumentSupplier().compute(doc -> doc.cloneNode(true));
        }
        if (cell instanceof BooleanValue) {
            BooleanValue bv = (BooleanValue)cell;
            return bv.getBooleanValue();
        }
        if (cell instanceof IntValue) {
            IntValue iv = (IntValue)cell;
            return iv.getIntValue();
        }
        if (cell instanceof LongValue) {
            LongValue lv = (LongValue)cell;
            return lv.getLongValue();
        }
        if (cell instanceof DoubleValue) {
            DoubleValue dv = (DoubleValue)cell;
            return dv.getDoubleValue();
        }
        if (cell instanceof BinaryObjectDataValue) {
            BinaryObjectDataValue bodv = (BinaryObjectDataValue)cell;
            try {
                return bodv.openInputStream();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        if (cell instanceof ByteVectorValue) {
            ByteVectorValue bvv = (ByteVectorValue)cell;
            if (bvv.length() > Integer.MAX_VALUE) {
                throw new IllegalStateException("Too large byte-vector: " + bvv.length());
            }
            byte[] ret = new byte[(int)bvv.length()];
            for (int i = ret.length; i-- > 0;) {
                int v = bvv.get(i);
                v &= 0xff;
                ret[i] = (byte)v;
            }
            return ret;
        }
        //TODO how to represent?
        if (cell instanceof BitVectorValue) {
            throw new UnsupportedOperationException("Not supported datatype: bitvector");
//            BitVectorValue bvv = (BitVectorValue)cell;
//            return bvv.toString();
        }
        //TODO should we fail instead?
        return cell.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Invocation invocation(final Builder request, final DataRow row, final DataTableSpec spec) {
        final RestWithBodySettings settings = getSettings();
        final int bodyColumn = spec == null ? -1 : spec.findColumnIndex(settings.getRequestBodyColumn());
        final MediaType mediaType = MediaType.valueOf(extractHeaderValue(row, spec,
            settings.getRequestHeaders().stream().filter(v -> "Content-Type".equals(v.getKey())).findAny()
                .orElse(new RequestHeaderKeyItem("Content-Type", "application/json", ReferenceType.Constant))));

        Variant variant = new Variant(mediaType, (String)null, null);
        Object o = settings.isUseConstantRequestBody()
                ? settings.getConstantRequestBody() : createObjectFromCell(row.getCell(bodyColumn));
        Entity<?> entity = Entity.entity(o, variant);

        HTTPClientPolicy clientPolicy = WebClient.getConfig(request).getHttpConduit().getClient();

        // We always have to allow chunking because otherwise the content length cannot be determined. If chunking
        // should be disabled we have to set the buffer large enough so that it can hold the complete body. Then the
        // Content-Length header will be set accordingly.
        clientPolicy.setAllowChunking(true);

        int bufferSize;
        if (m_settings.isAllowChunking().orElse(RestSettings.DEFAULT_ALLOW_CHUNKING)) {
            // chunk allowed => use a moderately sized buffer of 1 MB.
            bufferSize = 1 << 20;
        } else {
            // chunking should not happen, try to guess the size of the body
            if (o instanceof byte[]) {
                bufferSize = ((byte[]) o).length;
            } else if (o instanceof String) {
                // we don't know the string's encoding, if it's UTF it can be larger than the string length
                bufferSize = ((String) o).length() * 2;
            } else {
                // use 16 MB in all other cases
                bufferSize = 16 << 20;
            }
        }
        clientPolicy.setChunkingThreshold(bufferSize);

        return invocationWithEntity(request, entity);
    }

    /**
     * Creates {@link Invocation} with an {@link Entity}.
     *
     * @param request A {@link Builder}.
     * @param entity The {@link Entity} to be used as the invocation body.
     * @return The create {@link Invocation}.
     */
    protected abstract Invocation invocationWithEntity(final Builder request, Entity<?> entity);

}
