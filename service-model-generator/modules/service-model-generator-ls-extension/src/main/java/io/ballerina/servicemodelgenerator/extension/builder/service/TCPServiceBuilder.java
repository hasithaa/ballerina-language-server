package io.ballerina.servicemodelgenerator.extension.builder.service;

import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.ServiceInitModel;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.model.context.AddModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.AddServiceInitModelContext;
import io.ballerina.servicemodelgenerator.extension.util.ListenerUtil;
import io.ballerina.servicemodelgenerator.extension.util.ServiceClassUtil;
import io.ballerina.servicemodelgenerator.extension.util.Utils;
import io.ballerina.tools.text.LineRange;
import org.eclipse.lsp4j.TextEdit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.ballerina.servicemodelgenerator.extension.util.Constants.ARG_TYPE_LISTENER_PARAM_INCLUDED_DEFAULTABLE_FILED;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.ARG_TYPE_LISTENER_PARAM_INCLUDED_FILED;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.ARG_TYPE_LISTENER_PARAM_REQUIRED;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.BALLERINA;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.CLOSE_BRACE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.LISTENER_VAR_NAME;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.NEW_LINE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.ON;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.OPEN_BRACE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.SERVICE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.SPACE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.TCP;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.TWO_NEW_LINES;
import static io.ballerina.servicemodelgenerator.extension.util.ListenerUtil.getDefaultListenerDeclarationStmt;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getProtocol;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getRequiredFunctionsForServiceType;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.populateRequiredFunctionsForServiceType;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.FunctionAddContext.TRIGGER_ADD;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getImportStmt;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.importExists;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.populateRequiredFuncsDesignApproachAndServiceType;

/**
 * Builder class for TCP service.
 *
 * @since 1.2.0
 */
public final class TCPServiceBuilder extends AbstractServiceBuilder {

    private static final String TCP_SERVICE_CLASS_NAME = "TcpEchoService";

    @Override
    public Map<String, List<TextEdit>> addServiceInitSource(AddServiceInitModelContext context) {
        ServiceInitModel serviceInitModel = context.serviceInitModel();
        Map<String, Value> properties = serviceInitModel.getProperties();
        ModulePartNode modulePartNode = context.document().syntaxTree().rootNode();

        ListenerDTO result = buildListenerDTO(context, properties, serviceInitModel, modulePartNode);

        String serviceName = Utils.generateTypeIdentifier(context.semanticModel(), context.document(),
                modulePartNode.lineRange().endLine(), TCP_SERVICE_CLASS_NAME);
        List<String> functionsStr = List.of(tcpServiceClassTemplate(serviceName));

        StringBuilder builder = new StringBuilder(NEW_LINE)
                .append(result.listenerDeclaration())
                .append(NEW_LINE)
                .append(SERVICE).append(SPACE).append(serviceInitModel.getBasePath(result.listenerProtocol()))
                .append(SPACE).append(ON).append(SPACE).append(result.listenerVarName()).append(SPACE).append(OPEN_BRACE)
                .append(NEW_LINE)
                .append(String.join(TWO_NEW_LINES, functionsStr)).append(NEW_LINE)
                .append(CLOSE_BRACE).append(NEW_LINE);

        List<TextEdit> edits = new ArrayList<>();
        if (!importExists(modulePartNode, serviceInitModel.getOrgName(), serviceInitModel.getModuleName())) {
            String importText = getImportStmt(serviceInitModel.getOrgName(), serviceInitModel.getModuleName());
            edits.add(new TextEdit(Utils.toRange(modulePartNode.lineRange().startLine()), importText));
        }
        edits.add(new TextEdit(Utils.toRange(modulePartNode.lineRange().endLine()), builder.toString()));

        String serviceClass = ServiceClassUtil.getTcpConnectionServiceTemplate().formatted(serviceName);
        edits.add(new TextEdit(Utils.toRange(modulePartNode.lineRange().endLine()), serviceClass));
        return Map.of(context.filePath(), edits);
    }

    @Override
    public Map<String, List<TextEdit>> addModel(AddModelContext context) throws Exception {
        List<TextEdit> edits = new ArrayList<>();
        ListenerUtil.DefaultListener defaultListener = ListenerUtil.getDefaultListener(context);
        if (Objects.nonNull(defaultListener)) {
            String stmt = getDefaultListenerDeclarationStmt(defaultListener);
            edits.add(new TextEdit(Utils.toRange(defaultListener.linePosition()), stmt));
        }

        Service service = context.service();
        populateRequiredFuncsDesignApproachAndServiceType(service);
        populateRequiredFunctionsForServiceType(service);

        ModulePartNode rootNode = context.document().syntaxTree().rootNode();
        LineRange lineRange = rootNode.lineRange();
        String serviceName = Utils.generateTypeIdentifier(context.semanticModel(), context.document(),
                lineRange.endLine(), TCP_SERVICE_CLASS_NAME);

        StringBuilder serviceBuilder = new StringBuilder(NEW_LINE);
        buildServiceNodeStr(service, serviceBuilder);
        buildServiceNodeBody(List.of(tcpServiceClassTemplate(serviceName)), serviceBuilder);

        edits.add(new TextEdit(Utils.toRange(lineRange.endLine()), serviceBuilder.toString()));

        if (!importExists(rootNode, BALLERINA, TCP)) {
            String importStatement = Utils.getImportStmt(service.getOrgName(), service.getModuleName());
            edits.addFirst(new TextEdit(Utils.toRange(lineRange.startLine()), importStatement));
        }

        String serviceClass = ServiceClassUtil.getTcpConnectionServiceTemplate().formatted(serviceName);
        edits.add(new TextEdit(Utils.toRange(lineRange.endLine()), serviceClass));

        return Map.of(context.filePath(), edits);
    }

    public static String tcpServiceClassTemplate(String serviceClassName) {
        String f = "    remote function onConnect(tcp:Caller caller) returns tcp:ConnectionService|tcp:Error? {%n" +
                "        do {%n" +
                "            %s connectionService = new %s();%n" +
                "            return connectionService;%n" +
                "        } on fail error err {%n" +
                "            // handle error%n" +
                "            return error(\"unhandled error\", err);%n" +
                "        }%n" +
                "    }";
        return f.formatted(serviceClassName, serviceClassName);
    }

    @Override
    public String kind() {
        return TCP;
    }
}
