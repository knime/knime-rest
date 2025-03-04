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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RegExUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
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
import org.knime.core.data.xml.XMLVersion;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.rest.generic.EachRequestAuthentication;
import org.knime.rest.nodes.common.RestNodeModel;
import org.w3c.dom.NodeList;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

/**
 * Node model of the Webpage Retriever node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class WebpageRetrieverNodeModel extends RestNodeModel<WebpageRetrieverSettings> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WebpageRetrieverNodeModel.class);

    private static final HeaderDelegate<NewCookie> COOKIE_DELEGATE =
        RuntimeDelegate.getInstance().createHeaderDelegate(NewCookie.class);

    private static final String XMLNS = "xmlns";

    // the base URI is needed to replace relative URLs with the absolute ones
    private URI m_baseURI;

    /**
     * Constructor for a WebPageRetrieverNodeModel.
     * @param cfg The node creation configuration.
     */
    protected WebpageRetrieverNodeModel(final NodeCreationConfiguration cfg) {
        super(cfg);
    }

    @Override
    protected WebpageRetrieverSettings createSettings() {
        return new WebpageRetrieverSettings();
    }

    @Override
    protected Invocation invocation(final Builder request, final DataRow row, final DataTableSpec spec) {
        m_baseURI = URI.create(WebClient.getConfig(request).getHttpConduit().getAddress());
        return request.buildGet();
    }

    @Override
    protected DataCell parseBodyCell(final Response response) {
        // the cell is the one either containing the body value or is missing
        final DataCell bodyCell = super.parseBodyCell(response);
        // if the response is null (e.g. config error), return null since there is nothing to parse
        if (bodyCell == null) {
            return null;
        }
        // if the body value cell is missing, there must have been an error, so return the missing cell
        if (bodyCell.isMissing()) {
            if (response.getStatus() == 200) {
                return bodyCell;
            } else {
                return new MissingCell(
                    "Status Code: " + response.getStatus() + "; " + ((MissingCell)bodyCell).getError());
            }
        }
        // we can only handle strings
        if (!(bodyCell instanceof StringValue)) {
            final String errorMsg = "Incompatible media type cannot be parsed: " + response.getMediaType();
            return new MissingCell(errorMsg);
        }
        // if not missing, get and parse the body value
        final String bodyValue = ((StringValue)bodyCell).getStringValue();
        if (m_baseURI == null) {
            // should never happen
            throw new IllegalStateException("The base URL must not be null.");
        }
        final var htmlDocument = Jsoup.parse(
            // remove the invalid chars of the default XML version used by cell writers
            m_settings.isOutputAsXML() //
                ? RegExUtils.removeAll(bodyValue, XMLVersion.getCellDefault().invalidChars()) //
                : bodyValue,
            m_baseURI.toString());

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
            // Fix an issue where "xlink:href" attributes are present in <svg> elements, but the attribute is
            // deprecated and should be replaced by "href" only. "xlink:href" caused errors downstream in the XML
            // parser.
            replaceDeprecatedXLinkHrefAttributesInSVGs(htmlDocument);

            // Fix a problem that showed up with JSoup 1.14: the DOCTYPE is part of the tree, which causes W3CDom
            // to insert a line (<!DOCTYPE html PUBLIC "" "">) that causes errors in the XML parser. So we simply
            // remove the DOCTYPE.
            htmlDocument.childNodes() //
                .stream() //
                .filter(DocumentType.class::isInstance) //
                .forEach(Node::remove);

            org.w3c.dom.Document w3cDoc = new PatchedW3CDom().fromJsoup(htmlDocument);

            // fix an issue where the xmlns attribute was twice in the output for certain elements.
            // (can be tested with this webpage: http://www.handletheheat.com/peanut-butter-snickerdoodles
            // that could not be parsed by XPath if the attribute was set twice)
            // may be fixed with a later version of jsoup
            final NodeList list = w3cDoc.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                removeNameSpaceAttributes(list.item(i));
            }

            return XMLCellFactory.create(w3cDoc);
        }

        return new StringCell(htmlDocument.html());
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

    private static void replaceDeprecatedXLinkHrefAttributesInSVGs(final Document htmlDocument) {
        for (var svgElement: htmlDocument.select("svg")) {
            replaceDeprecatedXLinkHrefAttributes(svgElement);
        }
    }

    private static void replaceDeprecatedXLinkHrefAttributes(final Element element) {
        if (element.attributes().hasKeyIgnoreCase("xlink:href")) {
            if (element.attributes().hasKeyIgnoreCase("href")) {
                LOGGER.warn("SVG element " + element.tagName()
                    + " has both, xlink:href and href attributes set. Removing deprecated xlink:href attribute.");
            } else {
                element.attr("href", element.attr("xlink:href"));
                element.attributes().removeIgnoreCase("xlink:href");
            }
        }

        // apply recursively
        for (var child : element.children()) {
            replaceDeprecatedXLinkHrefAttributes(child);
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

    /**
     * {@inheritDoc}
     *
     * Additionally extracts cookies from the response. If the response is null or does not contain any
     * cookies, a missing cell is added. Otherwise, a list cell with all cookies is added.
     */
    @Override
    protected List<DataCell> computeFinalOutputCells(final Response response,
        final List<DataCell> headerCells, final DataCell... bodyCells) {
        CheckUtils.checkArgument(bodyCells.length == 1, "Too many body cells, expected only one");
        headerCells.clear();
        if (m_settings.isExtractCookies()) {
            final var noCookiesCell = new MissingCell("The server did not send any cookies.");
            if (response == null) {
                return super.computeFinalOutputCells(response, headerCells, bodyCells[0], noCookiesCell);
            }
            final Map<String, NewCookie> cookies = response.getCookies();
            // maps each cookie to a StingCell
            final List<StringCell> cookiesAsListOfStringCells = cookies.values().stream() //
                    .map(cookie -> new StringCell(COOKIE_DELEGATE.toString(cookie))) //
                    .toList();
            // StringCells of cookies are then aggregated into a ListCell
            return super.computeFinalOutputCells(response, headerCells, bodyCells[0],
                cookiesAsListOfStringCells.isEmpty() ? noCookiesCell
                    : CollectionCellFactory.createListCell(cookiesAsListOfStringCells));
        }
        return super.computeFinalOutputCells(response, headerCells, bodyCells[0]);
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
        updateErrorCauseColumnSpec(newSpecs, uniqueNameGenerator);
        return newSpecs.toArray(new DataColumnSpec[0]);
    }

    @Override
    protected void makeFirstCall(final DataRow row,
        final List<EachRequestAuthentication> enabledEachRequestAuthentications, final DataTableSpec spec,
        final ExecutionContext exec) throws InvalidSettingsException, IOException {
        super.makeFirstCall(row, enabledEachRequestAuthentications, spec, exec);
        // we have to override this variable since it will define the output spec if no input is connected
        m_newColumnsBasedOnFirstCalls = createNewColumnsSpec(spec);
    }

    @Override
    protected void reset() {
        super.reset();
        m_baseURI = null;
    }

    /**
     * In Jsoup 1.17.2 (which ships with Eclipse 2024-03 and therefore AP 5.3) all HTML elements without explicitly
     * declared namespace are implicitly assigned to {@code "http://www.w3.org/1999/xhtml"} (a regression, AP-22864).
     * We have to extend here to get access to the inner class {@link W3CDom.W3CBuilder}, which we have to modify.
     */
    private class PatchedW3CDom extends W3CDom {

        /** Default constructor, namespace-aware. */
        public PatchedW3CDom() {
            this.namespaceAware(true); // NOSONAR
        }

        @Override
        @SuppressWarnings({"unchecked", "java:S3011"})
        public void convert(final Element in, final org.w3c.dom.Document out) {
            final var builder = new W3CBuilder(out);
            try {
                // clear the namespace entry added by the constructor (https://github.com/jhy/jsoup/commit/4a278e9)
                final var namespacesStackField = W3CBuilder.class.getDeclaredField("namespacesStack");
                namespacesStackField.setAccessible(true);
                ((Stack<Map<?, ?>>)namespacesStackField.get(builder)).peek().clear();
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException
                    | IllegalAccessException e) {
                LOGGER.coding("Could not reset namespaces stack", e);
            }
            final var inDoc = in.ownerDocument();
            if (inDoc != null && !StringUtil.isBlank(inDoc.location())) {
                out.setDocumentURI(inDoc.location());
            }
            // skip the #root node if a Document
            Element rootEl = in instanceof Document ? in.firstElementChild() : in;
            NodeTraversor.traverse(builder, rootEl);
        }
    }
}
