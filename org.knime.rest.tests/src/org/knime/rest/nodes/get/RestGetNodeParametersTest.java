package org.knime.rest.nodes.get;

import java.io.FileInputStream;
import java.io.IOException;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.rest.nodes.common.webui.RestNodeParameters;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class RestGetNodeParametersTest extends DefaultNodeSettingsSnapshotTest {

    RestGetNodeParametersTest() {
        super(getConfig());
    }

    private static final PortObjectSpec[] INPUT_PORT_SPECS = new PortObjectSpec[]{
        new DataTableSpec(
            new DataColumnSpecCreator("URL", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Header1", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Header2", StringCell.TYPE).createSpec()
        )
    };

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .withInputPortObjectSpecs(INPUT_PORT_SPECS) //
            .testJsonFormsForModel(RestNodeParameters.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            .build();
    }

    private static RestNodeParameters readSettings() {
        try {
            var path = getSnapshotPath(RestNodeParameters.class).getParent().resolve("node_settings")
                .resolve("RestGetNodeParameters.xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return NodeParametersUtil.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    RestNodeParameters.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }
}
