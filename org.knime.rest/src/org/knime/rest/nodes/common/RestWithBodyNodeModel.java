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
import org.knime.core.node.port.PortObjectSpec;
import org.knime.rest.nodes.common.RestSettings.ReferenceType;
import org.knime.rest.nodes.common.RestSettings.RequestHeaderKeyItem;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Variant;

/**
 * Base class for the REST nodes which require body content in their call.
 *
 * @author Gabor Bakos
 * @param <S> Type for the settings.
 */
public abstract class RestWithBodyNodeModel<S extends RestWithBodySettings> extends RestNodeModel<S> {

    private static final RequestHeaderKeyItem CONTENT_TYPE_WILDCARD =
        new RequestHeaderKeyItem(HttpHeaders.CONTENT_TYPE, MediaType.WILDCARD, ReferenceType.Constant);

    /**
     * The default chunking threshold, i.e. the body size at which 'Transfer-Encoding: chunked' should be enabled.
     * Package scope for tests.
     */
    static int chunkingThreshold = 1 << 20;

    /**
     * @param cfg The node creation configuration.
     */
    protected RestWithBodyNodeModel(final NodeCreationConfiguration cfg) {
        super(cfg);
    }

    /**
     * Passing no-args constructor to subclasses.
     */
    protected RestWithBodyNodeModel() {
        super();
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final String requestBodyColumn = m_settings.getRequestBodyColumn();

        if (!m_settings.isUseConstantRequestBody()) {
            if (requestBodyColumn == null) {
                throw new InvalidSettingsException(
                    "The node is configured to use an empty request body column. Please change the configuration.");
            } else {
                final var dataTableSpec = (DataTableSpec)inSpecs[0];
                if (dataTableSpec != null && !dataTableSpec.containsName(requestBodyColumn)) {
                    throw new InvalidSettingsException("The configured request body column '" + requestBodyColumn
                        + "' is missing in the input table.");

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
        if (cell instanceof JSONValue jv) {
            if (cell instanceof StringValue sv) {
                return sv.getStringValue();
            }
            // CXF does not support the javax.json implementation, so we return
            // as String (currently all implementations are `StringValue`s)
            return ((StringValue)JSONCellFactory.create(jv.getJsonValue())).getStringValue();
        }
        if (cell instanceof PNGImageValue pngv) {
            return pngv.getImageContent().getByteArray();
        }
        if (cell instanceof StringValue sv) {
            return sv.getStringValue();
        }
        if (cell instanceof SvgValue svgv) {
            return svgv.getDocumentSupplier().compute(doc -> doc.cloneNode(true));
        }
        if (cell instanceof XMLValue<?> xmlv) {
            return xmlv.getDocumentSupplier().compute(doc -> doc.cloneNode(true));
        }
        if (cell instanceof BooleanValue bv) {
            return bv.getBooleanValue();
        }
        if (cell instanceof IntValue iv) {
            return iv.getIntValue();
        }
        if (cell instanceof LongValue lv) {
            return lv.getLongValue();
        }
        if (cell instanceof DoubleValue dv) {
            return dv.getDoubleValue();
        }
        if (cell instanceof BinaryObjectDataValue bodv) {
            try {
                return bodv.openInputStream();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        if (cell instanceof ByteVectorValue bvv) {
            if (bvv.length() > Integer.MAX_VALUE) {
                throw new IllegalStateException("Too large byte-vector: " + bvv.length());
            }
            final var ret = new byte[(int)bvv.length()];
            for (int i = ret.length; i > 0; i--) {
                int v = bvv.get(i);
                v &= 0xff;
                ret[i] = (byte)v;
            }
            return ret;
        }
        if (cell instanceof BitVectorValue) {
            throw new UnsupportedOperationException("Not supported datatype: bitvector");
        }
        return cell.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Invocation invocation(final Builder request, final DataRow row, final DataTableSpec spec) {
        final RestWithBodySettings settings = getSettings();
        final int bodyColumn = spec == null ? -1 : spec.findColumnIndex(settings.getRequestBodyColumn());
        final var mediaType = MediaType.valueOf(extractHeaderValue(row, spec, settings.getRequestHeaders().stream()
            .filter(v -> HttpHeaders.CONTENT_TYPE.equals(v.getKey())).findAny().orElse(CONTENT_TYPE_WILDCARD)));

        // Construct the body entity with a content type, based on constant or row-based content.
        final var variant = new Variant(mediaType, (String)null, null);
        Object o = settings.isUseConstantRequestBody() ? settings.getConstantRequestBody()
            : createObjectFromCell(row.getCell(bodyColumn));
        Entity<?> entity = Entity.entity(o, variant);

        HTTPClientPolicy clientPolicy = WebClient.getConfig(request).getHttpConduit().getClient();

        // We always have to allow chunking because otherwise the content length cannot be determined. If chunking
        // should be disabled we have to set the buffer large enough so that it can hold the complete body. Then the
        // Content-Length header will be set accordingly.
        clientPolicy.setAllowChunking(true);

        int bufferSize;
        if (m_settings.isAllowChunking().orElse(RestSettings.DEFAULT_ALLOW_CHUNKING)) {
            // chunk allowed => use a moderately sized buffer of 1 MB.
            bufferSize = chunkingThreshold;
        } else {
            // chunking should not happen, try to guess the size of the body
            if (o instanceof byte[] bo) {
                bufferSize = bo.length;
            } else if (o instanceof String so) {
                // we don't know the string's encoding, if it's UTF it can be larger than the string length
                bufferSize = so.length() * 2;
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
