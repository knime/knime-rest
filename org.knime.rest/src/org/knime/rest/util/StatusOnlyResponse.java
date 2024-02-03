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
 *   Feb 3, 2024 (lw): created
 */
package org.knime.rest.util;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Link.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;

/**
 * Response object, only storing the status code. Can be constructed
 * if minimal information is available about the actual {@link Response}.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
public class StatusOnlyResponse extends Response {

    private final StatusType m_status;

    /**
     * Default constructor, only taking response code.
     *
     * @param statusCode
     */
    public StatusOnlyResponse(final int statusCode) {
        this(statusCode, null);
    }

    /**
     * Apart from response code also stores a reason phrase.
     *
     * @param statusCode
     * @param reasonPhrase
     */
    public StatusOnlyResponse(final int statusCode, final String reasonPhrase) {
        m_status = new Response.StatusType() {

            @Override
            public Family getFamily() {
                return Family.familyOf(statusCode);
            }

            @Override
            public String getReasonPhrase() {
                if (reasonPhrase != null) {
                    return reasonPhrase;
                }
                final var statusEnum = Response.Status.fromStatusCode(statusCode);
                return statusEnum != null ? statusEnum.getReasonPhrase() : "";
            }

            @Override
            public int getStatusCode() {
                return statusCode;
            }
        };
    }

    @Override
    public int getStatus() {
        return m_status.getStatusCode();
    }

    @Override
    public StatusType getStatusInfo() {
        return m_status;
    }

    @Override
    public Object getEntity() {
        return null;
    }

    @Override
    public <T> T readEntity(final Class<T> entityType) {
        return null;
    }

    @Override
    public <T> T readEntity(final GenericType<T> entityType) {
        return null;
    }

    @Override
    public <T> T readEntity(final Class<T> entityType, final Annotation[] annotations) {
        return null;
    }

    @Override
    public <T> T readEntity(final GenericType<T> entityType, final Annotation[] annotations) {
        return null;
    }

    @Override
    public boolean hasEntity() {
        return false;
    }

    @Override
    public boolean bufferEntity() {
        return false;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.WILDCARD_TYPE;
    }

    @Override
    public Locale getLanguage() {
        return null;
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public Set<String> getAllowedMethods() {
        return Collections.emptySet();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return Collections.emptyMap();
    }

    @Override
    public EntityTag getEntityTag() {
        return null;
    }

    @Override
    public Date getDate() {
        return null;
    }

    @Override
    public Date getLastModified() {
        return null;
    }

    @Override
    public URI getLocation() {
        return null;
    }

    @Override
    public Set<Link> getLinks() {
        return Collections.emptySet();
    }

    @Override
    public boolean hasLink(final String relation) {
        return false;
    }

    @Override
    public Link getLink(final String relation) {
        return null;
    }

    @Override
    public Builder getLinkBuilder(final String relation) {
        return null;
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        return new MultivaluedHashMap<>();
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return new MultivaluedHashMap<>();
    }

    @Override
    public String getHeaderString(final String name) {
        return null;
    }
}
