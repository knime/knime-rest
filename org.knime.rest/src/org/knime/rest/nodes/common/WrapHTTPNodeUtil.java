package org.knime.rest.nodes.common;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.MetaPortInfo;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowAnnotationID;
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
     * Add a credentials configuration node based on the authentication configuration
     * @param componentWfm The workflow manager to add the node to
     * @param config The REST configuration containing authentication settings
     * @return The NodeID of the created credentials configuration node, or null if no credentials needed
     */
    private static NodeID addCredentialsConfiguration(final WorkflowManager componentWfm, final RestKaiNodeConfig config) {
        if (config.authentication == null || config.authentication.credentialsVariable == null) {
            return null;
        }

        final String authType = config.authentication.type;
        if (!authType.equals("Bearer") && !authType.equals("Basic")) {
            return null;
        }

        final var credVar = config.authentication.credentialsVariable;
        final boolean hideUsername = authType.equals("Bearer");

        return addAndConfigureNode(componentWfm,
            "org.knime.js.base.node.configuration.input.credentials.CredentialsDialogNodeFactory",
            nodeSettings -> {
                var modelSettings = nodeSettings.addNodeSettings("model");
                modelSettings.addString("parameterName", credVar.name);
                modelSettings.addString("label", credVar.title != null ? credVar.title : credVar.name);
                modelSettings.addString("description", credVar.description != null ? credVar.description : "");
                modelSettings.addString("usernameLabel", credVar.usernamePlaceholder != null ? credVar.usernamePlaceholder : "Username");
                modelSettings.addString("passwordLabel", credVar.passwordOrTokenPlaceholder != null ? credVar.passwordOrTokenPlaceholder : (hideUsername ? "Token" : "Password"));
                modelSettings.addString("flowVariableName", credVar.name);
                modelSettings.addBoolean("prompt_username", !hideUsername);
                modelSettings.addBoolean("hideInDialog", false);
                modelSettings.addBoolean("required", true);
                modelSettings.addBoolean("use_server_login", false);
                modelSettings.addBoolean("no_display", false);
                modelSettings.addString("error_message", "");

                // Configure default value structure
                var defaultValue = modelSettings.addNodeSettings("defaultValue");
                defaultValue.addBoolean("isSavePassword", false);
                defaultValue.addBoolean("useServerLoginCredentials", false);
                var credentialsValue = defaultValue.addNodeSettings("credentialsValue");
                credentialsValue.addBoolean("isCredentials_Internals", true);
                credentialsValue.addString("name", "");
                credentialsValue.addString("login", "");
            });
    }

    /**
     * @param nc
     * @param nodeSettings
     * @param cfg
     * @param variableSetters
     * @param configAndTemplateVariables
     * @return
     */
    static SubNodeContainer configureHTTPNodeAndResolveTemplates(final NativeNodeContainer nc,
        final List<Variable> variables, final RestKaiNodeConfig config, final NodeSettings nodeSettings,
        final RestSettings cfg, final List<Consumer<VariableSettings>> variableSetters) {
        final var rootWfm = nc.getParent();
        try (var unused = rootWfm.lock()) {
            final var ncModelClass = nc.getNodeModel().getClass();
            final var collapseResult = rootWfm.collapseIntoMetaNode(new NodeID[]{nc.getID()},
                new WorkflowAnnotationID[0], nc.getNodeAnnotation().getText());
            final var subWorkflowId = collapseResult.getCollapsedMetanodeID();
            rootWfm.changeMetaNodeOutputPorts(subWorkflowId, new MetaPortInfo[]{ //
                MetaPortInfo.builder().setPortType(BufferedDataTable.TYPE).build(), //
            });
            final var componentID = rootWfm.convertMetaNodeToSubNode(subWorkflowId).getConvertedNodeID();
            final var component = (SubNodeContainer)rootWfm.getNodeContainer(componentID);
            component.setUIInformation(nc.getUIInformation());
            final var componentWfm = component.getWorkflowManager();

            final var httpNodeId =
                componentWfm.findNodes(ncModelClass, false).keySet().stream().findFirst().orElseThrow();

            String urlWithTemplate = cfg.getConstantURL();
            String bodyWithTemplate = "";
            if (cfg instanceof RestWithBodySettings rwbs) {
                bodyWithTemplate = rwbs.getConstantRequestBody();
            }

            var urlVariables = extractVariablesFromTemplate(urlWithTemplate, variables);
            var bodyVariables = extractVariablesFromTemplate(bodyWithTemplate, variables);
            var otherVariables = extractAllOtherVariables(urlVariables, bodyVariables, variables);

            final var urlPerVariable =
                addNodesToResolveTemplate(componentWfm, urlWithTemplate, urlVariables, "constructedUrl");
            final var bodyPerVariable =
                addNodesToResolveTemplate(componentWfm, bodyWithTemplate, bodyVariables, "constructedBody");

            NodeID otherVariablesNodeId = null;
            if (!otherVariables.isEmpty()) {
                otherVariablesNodeId = variablesToKnimeNodes(componentWfm, otherVariables);
            }

            NodeID credentialsNodeId = addCredentialsConfiguration(componentWfm, config);

            // TODO: Connect to http node


            if (urlPerVariable.isPresent() && bodyPerVariable.isPresent()) {
                // TODO: Merge and use as new predecessor below
                throw new UnsupportedOperationException("Need to implement merging of expression-built variables.");
            }

            setVariablesInHttpNode(componentWfm, nodeSettings, urlPerVariable, bodyPerVariable, variableSetters);
            try {
                componentWfm.loadNodeSettings(httpNodeId, nodeSettings);
            } catch (InvalidSettingsException e) {
                // TODO Auto-generated catch block
            }
            urlPerVariable.or(() -> bodyPerVariable)
                .ifPresent(predecessor -> componentWfm.addConnection(predecessor, 1, httpNodeId, 0));
            componentWfm.addConnection(httpNodeId, 1, component.getVirtualOutNodeID(), 1);

            //            final var bodyCellExtractor = addAndConfigureNode(componentWfm,
            //                "org.knime.base.node.preproc.table.cellextractor.CellExtractorNodeFactory",
            //                new CellExtractorSettings(cfg.getResponseBodyColumn(), 1));
            //
            //            final var textView =
            //                addAndConfigureNode(componentWfm, "org.knime.base.views.node.textview.TextViewNodeFactory", s -> {
            //                    s.addNodeSettings(SettingsType.VIEW.getConfigKey()).addString("richTextContent", "");
            //                    final var nodeSettingsVariables = s.addNodeSettings(SettingsType.VIEW.getVariablesConfigKey());
            //                    urlPerVariable.ifPresent(_urlExpression -> {
            //                        final var variableSettings = new VariableSettings(s, SettingsType.VIEW);
            //                        try {
            //                            variableSettings.addUsedVariable("richTextContent", "extracted_cell");
            //                        } catch (InvalidSettingsException ex) {
            //                            throw new IllegalStateException(ex);
            //                        }
            //                        variableSettings.getVariableSettings().ifPresent(vars -> vars.copyTo(nodeSettingsVariables));
            //                    });
            //                });
            //            componentWfm.addConnection(httpNodeId, 1, bodyCellExtractor, 1);
            //            componentWfm.addConnection(bodyCellExtractor, 1, textView, 0);

            // Add Text view preview

            new LayoutManager(WorkflowManagerWrapper.wrap(componentWfm), new Random().nextLong()).doLayout(null);
            return component;
        }
    }

    private static List<Variable> extractAllOtherVariables(final List<Variable> urlVariables,
        final List<Variable> bodyVariables, final List<Variable> variables) {
        if (variables == null || variables.isEmpty()) {
            return List.of();
        }
        return variables.stream().filter(var -> !urlVariables.contains(var) && !bodyVariables.contains(var)).toList();

    }

    private static void setVariablesInHttpNode(final WorkflowManager componentWfm, final NodeSettings nodeSettings,
        final Optional<NodeID> urlPerVariable, final Optional<NodeID> bodyPerVariable, final List<Consumer<VariableSettings>> variableSetters) {
        final var urlCfgKey = "Constant URI";
        final var bodyCfgKey = "Constant request body";
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
        if (!variableSetters.isEmpty()) {
            final var variableSettings = new VariableSettings(nodeSettings, SettingsType.MODEL);

            variableSetters.forEach(setter -> setter.accept(variableSettings));
            variableSettings.getVariableSettings().ifPresent(vars -> vars.copyTo(nodeSettingsVariables));

        }
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
        NodeID predecessor = variablesToKnimeNodes(componentWfm, variables);

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

    /**
     * @param componentWfm
     * @param variables
     * @return
     */
    public static NodeID variablesToKnimeNodes(final WorkflowManager componentWfm, final List<Variable> variables) {
        NodeID predecessor = null;
        for (var variable : variables) {
            var newNode = addConfigurationNode(componentWfm, variable);
            if (predecessor != null) {
                componentWfm.addConnection(predecessor, 1, newNode, 0);
            }
            predecessor = newNode;
        }
        return predecessor;
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
                new IntegerDialogNodeSettings(variable.name, //
                    variable.title, //
                    variable.description, //
                    (Integer)variable.defaultValue //
                ));
        } else if (variable.type == Variable.VariableType.DOUBLE) {
            return addAndConfigureNode(wfm, "org.knime.js.base.node.configuration.input.dbl.DoubleDialogNodeFactory",
                new DoubleDialogNodeSettings(variable.name, //
                    variable.title, //
                    variable.description, //
                    (Double)variable.defaultValue));
        } else if (variable.type == Variable.VariableType.BOOLEAN) {
            return addAndConfigureNode(wfm, "org.knime.js.base.node.configuration.input.bool.BooleanDialogNodeFactory",
                new BooleanDialogNodeSettings(variable.name, //
                    variable.title, //
                    variable.description, //
                    (Boolean)variable.defaultValue //
                ));
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
            try {
                ((NativeNodeContainer)wfm.getNodeContainer(id)).getModelSettingsUsingFlowObjectStack(); // Necessary to initialize the default model settings. Otherwise, the "model" field will not be present in the saved settings.
            } catch (Exception ex) {
                // E.g. nullpointer if not connected
            }

            wfm.saveNodeSettings(id, nodeSettings);
            configureNodeSettings.accept(nodeSettings);
            wfm.loadNodeSettings(id, nodeSettings);
            return id;
        } catch (IOException | InvalidSettingsException ex) {
            throw new RuntimeException(ex);
        }
    }

}