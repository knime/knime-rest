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
 *   30. Jan. 2016. (Gabor Bakos): created
 */
package org.knime.rest.generic;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.ParseException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory;
import org.knime.core.data.DataCellFactory.FromInputStream;
import org.knime.core.data.DataCellFactory.FromReader;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.node.ExecutionContext;
import org.knime.rest.util.JavaUtil;

/**
 *
 * @author Gabor Bakos
 */
public interface ResponseBodyParser {
    public MediaType supportedMediaType();

    public DataType producedDataType();

    public DataCell create(Response response);

    /**
     * @return Human readable description of the expected data (can be a single word, like {@code png}).
     */
    public String valueDescriptor();

    public static class Default implements ResponseBodyParser {
        private DataType m_produced;

        private MediaType m_mediaType;

        private ExecutionContext m_exec;

        public Default(final String mimeType, final DataType produced) {
            this(mimeType, produced, null);
        }

        public Default(final String mimeType, final DataType produced, final ExecutionContext exec) {
            this(MediaType.valueOf(mimeType), produced, exec);
        }

        public Default(final MediaType mediaType, final DataType produced) {
            this(mediaType, produced, null);
        }

        public Default(final MediaType mediaType, final DataType produced, final ExecutionContext exec) {
            m_mediaType = mediaType;
            m_produced = produced;
            m_exec = exec;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MediaType supportedMediaType() {
            return m_mediaType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataType producedDataType() {
            return m_produced;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell create(final Response response) {
            final MediaType mediaType = response.getMediaType();
            if (supportedMediaType().isCompatible(mediaType)) {
                final DataCellFactory dataCellFactory = m_produced.getCellFactory(m_exec).get();
                final String charset = mediaType.getParameters().get("charset");
                if (charset == null || !JavaUtil.executeWithoutException(() -> Charset.forName(charset)) || !(dataCellFactory instanceof FromReader)) {
                    if (dataCellFactory instanceof FromInputStream) {
                        final FromInputStream fromInputStream = (FromInputStream)dataCellFactory;
                        final InputStream is = response.readEntity(InputStream.class);
                        try {
                            return fromInputStream.createCell(is);
                        } catch (IOException e) {
                            return new MissingCell(e.getMessage());
                        }
                    }
                } else {//dataCellFactory is a FromReader
                    final FromReader fromReader = (FromReader)dataCellFactory;
                    final InputStream is = response.readEntity(InputStream.class);
                    try (final Reader reader = new InputStreamReader(is, charset)) {
                        return fromReader.createCell(reader);
                    } catch (IOException | ParseException e) {
                        return new MissingCell(e.getMessage());
                    }
                }
                return new MissingCell("Cannot produce the requested type from the read input.");
            } else {
                return new MissingCell(
                    "The value in the body has " + mediaType + ", but was expecting " + valueDescriptor() + " value.");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String valueDescriptor() {
            return m_produced.getName();
        }
    }
}
