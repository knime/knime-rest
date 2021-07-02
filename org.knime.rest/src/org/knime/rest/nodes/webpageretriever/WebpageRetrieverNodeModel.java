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
 *   Sep 25, 2019 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.rest.nodes.webpageretriever;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.spec.InvocationBuilderImpl;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.rest.generic.EachRequestAuthentication;
import org.knime.rest.nodes.common.RestNodeModel;
import org.w3c.dom.NodeList;

/**
 * Node model of the Webpage Retriever node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class WebpageRetrieverNodeModel extends RestNodeModel<WebpageRetrieverSettings> {

    private static final String XMLNS = "xmlns";

    // the base URI is needed to replace relative URLs with the absolute ones
    private URI m_baseURI;

    /**
     * Constructor for a WebPageRetrieverNodeModel.
     */
    protected WebpageRetrieverNodeModel() {
        super();
    }

    @Override
    protected WebpageRetrieverSettings createSettings() {
        return new WebpageRetrieverSettings();
    }

    @Override
    protected Invocation invocation(final Builder request, final DataRow row, final DataTableSpec spec) {
        if (!(request instanceof InvocationBuilderImpl)) {
            // should never happen
            throw new IllegalStateException("Unexpected invocation builder: " + request.getClass().toString());
        }
        m_baseURI = ((InvocationBuilderImpl)request).getWebClient().getBaseURI();
        return request.buildGet();
    }

    @Override
    protected void addBodyValues(final List<DataCell> cells, final Response response, final DataCell missing) {
        super.addBodyValues(cells, response, missing);
        // the last cell is the one either containing the body value or is missing
        final DataCell cell = cells.remove(cells.size() - 1);
        // we only want to output the html and optionally the cookies sent by the server
        cells.clear();

        // if the body value cell is missing, there must have been an error, so return the missing cell
        if (cell.isMissing()) {
            if (response == null || response.getStatus() == 200) {
                cells.add(cell);
            } else {
                cells.add(
                    new MissingCell("Status Code: " + response.getStatus() + "; " + ((MissingCell)cell).getError()));
            }
            return;
        }
        // we can only handle strings
        if (!(cell instanceof StringValue)) {
            final String errorMsg = "Incompatible media type cannot be parsed: " + response.getMediaType();
            cells.add(new MissingCell(errorMsg));
            return;
        }
        // if not missing, get and parse the body value
        final String bodyValue = ((StringValue)cell).getStringValue();
        if (m_baseURI == null) {
            // should never happen
            throw new IllegalStateException("The base URI must not be null.");
        }
        final Document htmlDocument = Jsoup.parse(bodyValue, m_baseURI.toString());

        // replace relative URLs with absolute URLs
        if (m_settings.isReplaceRelativeURLS()) {
            replaceRelativeURLsWithAbsoluteURLs(htmlDocument);
        }

        // handle unbound prefixes
        handleUnboundPrefixes(htmlDocument);

        // remove the comments in the document
        removeComments(htmlDocument);

        // output either as XML or String cell
        if (m_settings.isOutputAsXML()) {
            // convert to w3c document
            org.w3c.dom.Document w3cDoc = new W3CDom().fromJsoup(htmlDocument);

            // fix an issue where the xmlns attribute was twice in the output for certain elements.
            // (can be tested with this webpage: http://www.handletheheat.com/peanut-butter-snickerdoodles
            // that could not be parsed by XPath if the attribute was set twice)
            // may be fixed with a later version of jsoup
            final NodeList list = w3cDoc.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                removeNameSpaceAttributes(list.item(i));
            }

            cells.add(XMLCellFactory.create(w3cDoc));
        } else {
            cells.add(new StringCell(htmlDocument.html()));
        }

        if (m_settings.isExtractCookies()) {
            final Map<String, NewCookie> cookies = response.getCookies();
            final List<StringCell> cookiesAsListOfStringCells =
                cookies.values().stream().map(NewCookie::toString).map(StringCell::new).collect(Collectors.toList());
            cells.add(cookiesAsListOfStringCells.isEmpty() ? new MissingCell("The server did not send any cookies.")
                : CollectionCellFactory.createListCell(cookiesAsListOfStringCells));
        }
    }

    // tag names that contain unbound prefixes (prefixes not defined in the namespace) will be replaced
    private static void handleUnboundPrefixes(final Document htmlDocument) {
        // collect all bound prefixes
        final Set<String> namespacePrefixes = Optional.ofNullable(htmlDocument.select("html").first())//
            .map(e -> e.attributes().asList().stream()//
                .filter(a -> a.getKey().startsWith(XMLNS + ":"))//
                .map(a -> a.getKey().substring(a.getKey().indexOf(":") + 1))//
                .collect(Collectors.toSet()))
            .orElse(new HashSet<>());

        // replace tag names with unbound prefixes
        final String separator = ":";
        for (final Element element : htmlDocument.select("*")) {
            // we are interested in element tags that contain ':'
            if (element.tagName().contains(separator)) {
                // split the tag into prefix and suffix
                final String[] tagSplit = element.tagName().split(separator, 2);
                // if the prefix is bound, i.e., the namespace is defined in the root, skip
                if (namespacePrefixes.contains(tagSplit[0])) {
                    continue;
                }
                // otherwise, replace the tag with a new tag that starts with the prefix and is followed by an id which
                // is the hash code of the suffix
                // NOTE: The id should actually be unique and always the same for the same strings, #hashCode does
                // not guarantee uniqueness but it's easy and should be good enough. Think about this if ever necessary.
                final int id;
                if (tagSplit.length > 1) {
                    id = (separator + tagSplit[1]).hashCode();
                } else {
                    // no suffix would be a strange case, but may happen
                    id = separator.hashCode();
                }
                element.tagName(tagSplit[0] + id);
            }
        }
    }

    // remove xmlns attributes, they will be in the output anyway since they are set as namespace
    private static void removeNameSpaceAttributes(final org.w3c.dom.Node node) {
        if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
            ((org.w3c.dom.Element)node).removeAttribute(XMLNS);
        }
        // do it recursively
        final NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            removeNameSpaceAttributes(list.item(i));
        }
    }

    private static void replaceRelativeURLsWithAbsoluteURLs(final Document htmlDocument) {
        // replace hyperlinks
        final Elements links = htmlDocument.select("a");
        replaceURLsInElements(links, "href");

        // replace sources
        final Elements media = htmlDocument.select("[src]");
        replaceURLsInElements(media, "src");

        // replace links with attribute href
        final Elements imports = htmlDocument.select("link[href]");
        replaceURLsInElements(imports, "href");
    }

    private static void replaceURLsInElements(final Elements elements, final String attributeKey) {
        for (org.jsoup.nodes.Element link : elements) {
            final String absUrl = link.absUrl(attributeKey);
            // set the absolute URL and overwrite possible relative ones
            link.attr(attributeKey, absUrl);
        }
    }

    private static void removeComments(final Node node) {
        for (int i = 0; i < node.childNodeSize();) {
            final Node child = node.childNode(i);
            if (child instanceof Comment) {
                child.remove();
            } else {
                // remove recursively
                removeComments(child);
                i++;
            }
        }
    }

    @Override
    protected DataColumnSpec[] createNewColumnsSpec(final DataTableSpec spec) {
        final UniqueNameGenerator uniqueNameGenerator = new UniqueNameGenerator(spec);
        // the last column in responseHeaderSpecs is the body column that is replaced by the parsed html
        final DataColumnSpec htmlSpec = uniqueNameGenerator.newColumn(m_settings.getOutputColumnName(),
            m_settings.isOutputAsXML() ? XMLCell.TYPE : StringCell.TYPE);
        final boolean extractCookies = m_settings.isExtractCookies();
        final List<DataColumnSpec> newSpecs = new ArrayList<>();
        newSpecs.add(htmlSpec);
        if (extractCookies) {
            final DataColumnSpec cookieColumn = uniqueNameGenerator.newColumn(m_settings.getCookieOutputColumnName(),
                ListCell.getCollectionType(StringCell.TYPE));
            newSpecs.add(cookieColumn);
        }
        maybeAddErrorCauseColSpec(newSpecs, uniqueNameGenerator);
        return newSpecs.toArray(new DataColumnSpec[0]);
    }

    @Override
    protected void makeFirstCall(final DataRow row,
        final List<EachRequestAuthentication> enabledEachRequestAuthentications, final DataTableSpec spec,
        final ExecutionContext exec) throws Exception {
        super.makeFirstCall(row, enabledEachRequestAuthentications, spec, exec);
        // we have to override this variable since it will define the output spec if no input is connected
        m_newColumnsBasedOnFirstCalls = createNewColumnsSpec(spec);
    }

    @Override
    protected void reset() {
        super.reset();
        m_baseURI = null;
    }

}
