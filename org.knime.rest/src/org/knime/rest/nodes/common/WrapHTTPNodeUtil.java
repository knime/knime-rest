package org.knime.rest.nodes.common;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.wrapper.WorkflowManagerWrapper;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.internal.VariableSettings;
import org.knime.gateway.api.util.CoreUtil;
import org.knime.js.base.node.configuration.input.bool.BooleanDialogNodeSettings;
import org.knime.js.base.node.configuration.input.dbl.DoubleDialogNodeSettings;
import org.knime.js.base.node.configuration.input.integer.IntegerDialogNodeSettings;
import org.knime.js.base.node.configuration.input.string.StringDialogNodeSettings;
import org.knime.rest.nodes.common.RestKaiNodeInterface.Variable;
import org.knime.workbench.ui.layout.LayoutManager;

final class WrapHTTPNodeUtil {

    private WrapHTTPNodeUtil() {

    }

    /**
     * @param nc
     * @param configAndTemplateVariables
     * @param nodeSettings
     * @param cfg
     * @return
     */
    public static SubNodeContainer configureHTTPNodeAndResolveTemplates(final NodeContainer nc,
        final List<Variable> variables, final NodeSettings nodeSettings, final RestSettings cfg) {
        final var rootWfm = nc.getParent();
        final var subWorkflowManager =
            rootWfm.createAndAddSubWorkflow(new PortType[0], new PortType[]{BufferedDataTable.TYPE}, "Hello World");

        String urlWithTemplate = cfg.getConstantURL();
        String bodyWithTemplate = "";

        var urlVariables = extractVariablesFromTemplate(urlWithTemplate, variables);
        var bodyVariables = extractVariablesFromTemplate(bodyWithTemplate, variables);

        final var urlPerVariable =
            addNodesToResolveTemplate(subWorkflowManager, urlWithTemplate, urlVariables, "constructedUrl");
        final var bodyPerVariable =
            addNodesToResolveTemplate(subWorkflowManager, bodyWithTemplate, bodyVariables, "constructedBody");

        if (urlPerVariable.isPresent() && bodyPerVariable.isPresent()) {
            // TODO: Merge and use as new predecessor below
            throw new UnsupportedOperationException("Need to implement merging of expression-built variables.");
        }

        setVariablesInHttpNode(subWorkflowManager, nodeSettings, urlPerVariable, bodyPerVariable);
        urlPerVariable.or(() -> bodyPerVariable)
            .ifPresent(predecessor -> subWorkflowManager.addConnection(predecessor, 1, nc.getID(), 0));
        subWorkflowManager.addConnection(nc.getID(), 1, subWorkflowManager.getID(), 0);

        new LayoutManager(WorkflowManagerWrapper.wrap(subWorkflowManager), new Random().nextLong()).doLayout(null);

        final var snc = (SubNodeContainer)rootWfm
            .getNodeContainer(rootWfm.convertMetaNodeToSubNode(subWorkflowManager.getID()).getConvertedNodeID());
        snc.setUIInformation(NodeUIInformation.builder().setNodeLocation(20, 30, -1, -1).build());
        return snc;
    }

    private static void setVariablesInHttpNode(final WorkflowManager componentWfm, final NodeSettings nodeSettings,
        final Optional<NodeID> urlPerVariable, final Optional<NodeID> bodyPerVariable) {
        final var urlCfgKey = "Constant URI";
        final var bodyCfgKey = "Constant request body";
        try {
            final var modelSettings = nodeSettings.getNodeSettings("model");
        } catch (InvalidSettingsException e) {
            throw new IllegalStateException(e); // settings already added in kai node interface
        }
        final var nodeSettingsVariables = nodeSettings.addNodeSettings(SettingsType.MODEL.getVariablesConfigKey());
        urlPerVariable.ifPresent(_urlExpression -> {
            final var variableSettings = new VariableSettings(nodeSettings, SettingsType.MODEL);
            try {
                variableSettings.addUsedVariable(urlCfgKey, "constructedUrl");
            } catch (InvalidSettingsException ex) {
                throw new IllegalStateException(ex);
            }
            variableSettings.getVariableSettings().ifPresent(vars -> vars.copyTo(nodeSettingsVariables));
        });
        bodyPerVariable.ifPresent(_urlExpression -> {
            final var variableSettings = new VariableSettings(nodeSettings, SettingsType.MODEL);
            try {
                variableSettings.addUsedVariable(bodyCfgKey, "constructedBody");
            } catch (InvalidSettingsException ex) {
                throw new IllegalStateException(ex);
            }
            variableSettings.getVariableSettings().ifPresent(vars -> vars.copyTo(nodeSettingsVariables));
        });
    }

    static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{(.*?)\\}\\}");

    /**
     * Extract variables from a template string that are used in the given variables list
     *
     * @param template The template string containing {{variableName}} placeholders
     * @param allVariables The complete list of available variables
     * @return List of variables that are actually used in the template
     */
    private static List<Variable> extractVariablesFromTemplate(final String template,
        final List<Variable> allVariables) {
        if (template == null || template.isEmpty() || allVariables.isEmpty()) {
            return List.of();
        }

        // Extract variable names from template using regex
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);

        var usedVariableNames = new HashSet<String>();
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            usedVariableNames.add(varName);
        }

        // Filter allVariables to only include those used in the template
        return allVariables.stream().filter(variable -> usedVariableNames.contains(variable.name)).toList();
    }

    private static Optional<NodeID> addNodesToResolveTemplate(final WorkflowManager componentWfm, final String template,
        final List<Variable> variables, final String outVarName) {
        if (variables.isEmpty()) {
            return Optional.empty();
        }
        NodeID predecessor = null;
        for (var variable : variables) {
            var newNode = addConfigurationNode(componentWfm, variable);
            if (predecessor != null) {
                componentWfm.addConnection(predecessor, 1, newNode, 0);
            }
            predecessor = newNode;
        }

        var expression = templateToExpression(template);
        var expressionNode = addAndConfigureNode(componentWfm,
            "org.knime.base.expressions.node.variable.ExpressionFlowVariableNodeFactory", s -> {
                final var m = s.getNodeSettings("model");
                final var firstExpression = m.getNodeSettings("additionalExpressions").getNodeSettings("0");
                firstExpression.addString("script", expression);
                firstExpression.addString("createdFlowVariable", outVarName);

            });
        componentWfm.addConnection(predecessor, 1, expressionNode, 0);
        return Optional.of(expressionNode);
    }

    private static String templateToExpression(final String template) {
        // First we need to make sure that when we put the string in quotes below, we don't break existing quotes
        final var escapedTemplate = template.replace("\"", "\\\"");
        Matcher matcher = TEMPLATE_PATTERN.matcher(escapedTemplate);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            // Replace {{variable}} with " + $$["variable"] + "
            matcher.appendReplacement(result, Matcher.quoteReplacement("\" + $$[\"" + varName + "\"] + \""));
        }
        matcher.appendTail(result);

        return "\"" + result.toString() + "\"";
    }

    private static NodeID addConfigurationNode(final WorkflowManager wfm, final Variable variable) {
        if (variable.type == Variable.VariableType.STRING) {
            return addAndConfigureNode(wfm, "org.knime.js.base.node.configuration.input.string.StringDialogNodeFactory",
                new StringDialogNodeSettings(// TODO: Let KAI set these values using the add_node tool. We do not want to have to add this public constructor and also there might be other fields that make sense to set (e.g. regex)
                    variable.name, //
                    variable.title, //
                    variable.description, //
                    (String)variable.defaultValue//
                ));
        } else if (variable.type == Variable.VariableType.INT) {
            return addAndConfigureNode(wfm,
                "org.knime.js.base.node.configuration.input.integer.IntegerDialogNodeFactory",
                new IntegerDialogNodeSettings());
        } else if (variable.type == Variable.VariableType.DOUBLE) {
            return addAndConfigureNode(wfm, "org.knime.js.base.node.configuration.input.double.DoubleDialogNodeFactory",
                new DoubleDialogNodeSettings());
        } else if (variable.type == Variable.VariableType.BOOLEAN) {
            return addAndConfigureNode(wfm,
                "org.knime.js.base.node.configuration.input.boolean.BooleanDialogNodeFactory",
                new BooleanDialogNodeSettings());
        } else {
            throw new UnsupportedOperationException("Unsupported variable type: " + variable.type);
        }
    }

    private static NodeID addAndConfigureNode(final WorkflowManager wfm, final String factoryClass,
        final DefaultNodeSettings parameters) {
        return addAndConfigureNode(wfm, factoryClass, nodeSettings -> {
            var modelSettings = nodeSettings.addNodeSettings("model");
            DefaultNodeSettings.saveSettings(parameters.getClass(), parameters, modelSettings);
        });
    }

    interface ThrowingConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }

    private static NodeID addAndConfigureNode(final WorkflowManager wfm, final String factoryClass,
        final ThrowingConsumer<NodeSettings, InvalidSettingsException> configureNodeSettings) {
        try {
            var id = CoreUtil.createAndAddNode(factoryClass, null, 10, 10, wfm, false);
            final var nodeSettings = new NodeSettings("root");
            ((NativeNodeContainer)wfm.getNodeContainer(id)).getModelSettingsUsingFlowObjectStack(); // Necessary to initialize the default model settings. Otherwise, the "model" field will not be present in the saved settings.

            wfm.saveNodeSettings(id, nodeSettings);
            configureNodeSettings.accept(nodeSettings);
            wfm.loadNodeSettings(id, nodeSettings);
            return id;
        } catch (IOException | InvalidSettingsException ex) {
            throw new RuntimeException(ex);
        }
    }

}