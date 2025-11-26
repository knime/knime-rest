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
 *   23. Jan. 2016. (Gabor Bakos): created
 */
package org.knime.rest.nodes.delete;

import static org.knime.node.impl.description.PortDescription.dynamicPort;
import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;
import org.knime.rest.nodes.common.RestNodeFactory;

/**
 * Node factory for the node of DELETE http method.
 *
 * @author Gabor Bakos
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class RestDeleteNodeFactory extends RestNodeFactory<RestDeleteNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    /**
     * Constructor
     */
    public RestDeleteNodeFactory() {
        super();
    }

    @Override
    public RestDeleteNodeModel createNodeModel(final NodeCreationConfiguration cfg) {
        return new RestDeleteNodeModel(cfg);
    }

    private static final String NODE_NAME = "DELETE Request";

    private static final String NODE_ICON = "./rest-delete.png";

    private static final String SHORT_DESCRIPTION = "DELETE REST client";

    private static final String FULL_DESCRIPTION = """
            <p>This node can be used to issue HTTP DELETE requests. DELETE requests are used to delete resources on a
            server. Usually you don't send any data with the request and you also don't get anything back, except for
            maybe a status message. Although sending additional data with a DELETE request generally has no defined
            semantics, some HTTP service implementations may still use this. Hence, a request body can be attached in
            this node as well.</p>

            <p>The node allows you to either send a request to a fixed URL (which is specified in the dialog) or to a
            list of URLs provided by an optional input table. Every URL will result in one request which in turn will
            result in one row in the output table. You can define custom request headers in the dialog.</p>
            <p>The sent data is usually taken from a column of the input table but you can also provide a constant value
            in the dialog instead.</p>
            <p>By default the output table will contain a column with the received data, its content type, and the HTTP
            status code. The node tries to automatically convert the received data into a KNIME data type based on its
            content type. In case no automatic conversion is possible, binary cells will be created.<br />
            You can extract additional response headers into column by defining them in the dialog.</p>
            <p>The node supports several authentication methods, e.g. BASIC and DIGEST. Other authentication methods may
             be provided by additional extensions.</p>

            <p>
            The node supports the Credential port as input (see dynamic input ports). If the port is added, it must
            supply a Credential that can be embedded into the HTTP Authorization header, and all request done by the
            node will use the Credential from the port, regardless of other node settings. The OAuth2 Authenticator ,
            nodes provide such a Credential for example.
            </p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
        fixedPort("Table", "Optional data table containing the variable parameters of the requests."),
        dynamicPort("Credential", "Credential", """
            A Credential, that can be embedded into the HTTP Authorization header. If this port is added, then all
            request done by the node will always use the Credential from the port, regardless of other node
            settings. The OAuth2 Authenticator nodes provide such a Credential for example.
            """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
        fixedPort("DELETE results", "Data table containing columns from the responses.")
    );

    /**
     * @since 5.10
     */
    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration cfg) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.10
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, RestDeleteNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(
            NODE_NAME,
            NODE_ICON,
            INPUT_PORTS,
            OUTPUT_PORTS,
            SHORT_DESCRIPTION,
            FULL_DESCRIPTION,
            List.of(),
            RestDeleteNodeParameters.class,
            null,
            NodeType.Manipulator,
            List.of(),
            null);
    }

    /**
     * @since 5.10
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, RestDeleteNodeParameters.class));
    }
}
