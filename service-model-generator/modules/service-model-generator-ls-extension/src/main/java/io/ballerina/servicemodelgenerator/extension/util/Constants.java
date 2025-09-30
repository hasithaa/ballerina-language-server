/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.servicemodelgenerator.extension.util;

import io.ballerina.servicemodelgenerator.extension.model.MetaData;

/**
 * Represents constants for the trigger model generator service.
 *
 * @since 1.0.0
 */
public class Constants {

    public static final String CAPABILITY_NAME = "serviceModel";
    public static final String DOUBLE_QUOTE = "\"";
    public static final String TAB = "\t";
    public static final String SPACE = " ";
    public static final String OPEN_BRACE = "{";
    public static final String CLOSE_BRACE = "}";
    public static final String NEW_LINE = System.lineSeparator();
    public static final String TWO_NEW_LINES = NEW_LINE + NEW_LINE;
    public static final String NEW_LINE_WITH_TAB = NEW_LINE + TAB;
    public static final String IMPORT_STMT_TEMPLATE = "%nimport %s/%s;%n";
    public static final String AT = "@";
    public static final String COLON = ":";

    public static final String SINGLE_SELECT_VALUE = "SINGLE_SELECT";
    public static final String MULTIPLE_SELECT_VALUE = "MULTIPLE_SELECT";
    public static final String HTTP_DEFAULT_LISTENER_ITEM_LABEL = "(+) Create and use the default HTTP listener" +
            " (port: 9090)";
    public static final String DEFAULT_LISTENER_ITEM_LABEL = "(+) Create and use a %s listener with " +
            "default configurations";
    public static final String DEFAULT_LISTENER_VAR_NAME = "%sDefaultListener";
    public static final String LISTENER_VAR_NAME = "%sListener";

    public static final String HTTP = "http";
    public static final String GRAPHQL = "graphql";
    public static final String TCP = "tcp";
    public static final String AI = "ai";

    public static final String KAFKA = "kafka";
    public static final String RABBITMQ = "rabbitmq";
    public static final String MQTT = "mqtt";
    public static final String ASB = "asb";
    public static final String SF = "salesforce";
    public static final String TRIGGER_GITHUB = "trigger.github";

    public static final String FTP = "ftp";
    public static final String FILE = "file";

    public static final String PROPERTY_REQUIRED_FUNCTIONS = "requiredFunctions";
    public static final String PROPERTY_DESIGN_APPROACH = "designApproach";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_BASE_PATH = "basePath";

    public static final String KIND_QUERY = "QUERY";
    public static final String KIND_MUTATION = "MUTATION";
    public static final String KIND_SUBSCRIPTION = "SUBSCRIPTION";
    public static final String KIND_RESOURCE = "RESOURCE";
    public static final String KIND_REMOTE = "REMOTE";
    public static final String KIND_DEFAULT = "DEFAULT";
    public static final String KIND_REQUIRED = "REQUIRED";
    public static final String KIND_DEFAULTABLE = "DEFAULTABLE";
    public static final String KIND_OBJECT_METHOD = "OBJECT_METHOD";

    public static final String PARAMETER = "parameter";
    public static final String SERVICE = "service";
    public static final String RESOURCE = "resource";
    public static final String REMOTE = "remote";
    public static final String BASE_PATH = "basePath";
    public static final String ON = "on";
    public static final String SUBSCRIBE = "subscribe";
    public static final String GET = "get";
    public static final String INIT = "init";

    public static final String VALUE_TYPE_CONDITIONAL_FIELDS = "CONDITIONAL_FIELDS";
    public static final String VALUE_TYPE_FORM = "FORM";
    public static final String VALUE_TYPE_CHOICE = "CHOICE";
    public static final String VALUE_TYPE_HEADER_SET = "HEADER_SET";
    public static final String VALUE_TYPE_SINGLE_SELECT = "SINGLE_SELECT";
    public static final String VALUE_TYPE_EXPRESSION = "EXPRESSION";
    public static final String VALUE_TYPE_IDENTIFIER = "IDENTIFIER";
    public static final String VALUE_TYPE_TYPE = "TYPE";
    public static final String VALUE_TYPE_STRING = "STRING";
    public static final String HTTP_PARAM_TYPE_QUERY = "QUERY";
    public static final String HTTP_PARAM_TYPE_PAYLOAD = "PAYLOAD";

    public static final String ARG_TYPE_LISTENER_VAR_NAME = "LISTENER_VAR_NAME";
    public static final String ARG_TYPE_LISTENER_PARAM_REQUIRED = "LISTENER_PARAM_REQUIRED";
    public static final String ARG_TYPE_LISTENER_PARAM_INCLUDED_FIELD = "LISTENER_PARAM_INCLUDED_FIELD";
    public static final String ARG_TYPE_LISTENER_PARAM_INCLUDED_DEFAULTABLE_FIELD =
            "LISTENER_PARAM_INCLUDED_DEFAULTABLE_FIELD";
    public static final String ARG_TYPE_SERVICE_BASE_PATH = "SERVICE_BASE_PATH";
    public static final String ARG_TYPE_SERVICE_TYPE_DESCRIPTOR = "SERVICE_TYPE_DESCRIPTOR";

    public static final String TYPE_SERVICE = "Service";

    public static final String CLASS_TYPE_SERVICE = "service";
    public static final String CLASS_TYPE_CLIENT = "client";
    public static final String CLASS_TYPE_DEFAULT = "default";

    public static final String TYPE_HTTP_SERVICE_CONFIG = "http:ServiceConfig";

    public static final String VARIABLE_NAME_KEY = "variableNameKey";

    // protocol listeners
    public static final String HTTP_DEFAULT_LISTENER_EXPR = "http:getDefaultListener()";
    public static final String GRAPHQL_DEFAULT_LISTENER_EXPR = "new (listenTo = 8080)";
    public static final String TCP_DEFAULT_LISTENER_EXPR = "new (localPort = 8080)";

    // event listeners
    public static final String KAFKA_DEFAULT_LISTENER_EXPR = "new (bootstrapServers = \"\")";
    public static final String RABBITMQ_DEFAULT_LISTENER_EXPR = "new (host = \"localhost\", port = 5672)";
    public static final String MQTT_DEFAULT_LISTENER_EXPR = "new(\"tcp://localhost:1883\", \"listener-unique-id\", " +
            "\"mqtt/topic\")";
    public static final String ASB_DEFAULT_LISTENER_EXPR = "new (connectionString = \"\", " +
            "entityConfig = {queueName: \"test-queue\"}, autoComplete = false)";
    public static final String SF_DEFAULT_LISTENER_EXPR = "new (auth = {username: \"\", password: \"\"})";
    public static final String GITHUB_DEFAULT_LISTENER_EXPR = "new ()";

    // file listeners
    public static final String FTP_DEFAULT_LISTENER_EXPR = "new ()";
    public static final String FILE_DEFAULT_LISTENER_EXPR = "new (path = \"\")";

    public static final MetaData SERCVICE_CLASS_NAME_METADATA = new MetaData("Class Name",
            "The name of the class definition");
    public static final MetaData GRAPHQL_CLASS_NAME_METADATA = new MetaData("Object Name",
            "The name of the object");
    public static final MetaData ARGUMENT_TYPE_METADATA = new MetaData("Argument Type",
            "The type of the argument");
    public static final MetaData ARGUMENT_NAME_METADATA = new MetaData("Argument Name",
            "The name of the argument");
    public static final MetaData ARGUMENT_DEFAULT_VALUE_METADATA = new MetaData("Default Value",
            "The default value of the argument");
    public static final MetaData ARGUMENT_DOCUMENTATION_METADATA = new MetaData("Description",
            "The description of the argument");
    public static final MetaData PARAMETER_TYPE_METADATA = new MetaData("Parameter Type",
            "The type of the parameter");
    public static final MetaData PARAMETER_NAME_METADATA = new MetaData("Parameter Name",
            "The name of the parameter");
    public static final MetaData PARAMETER_DEFAULT_VALUE_METADATA = new MetaData("Default Value",
            "The default value of the parameter");
    public static final MetaData PARAMETER_DOCUMENTATION_METADATA = new MetaData("Description",
            "The description of the parameter");
    public static final MetaData FIELD_TYPE_METADATA = new MetaData("Field Type",
            "The type of the field");
    public static final MetaData FIELD_NAME_METADATA = new MetaData("Field Name",
            "The name of the field");
    public static final MetaData FIELD_DOCUMENTAION_METADATA = new MetaData("Description",
            "The description of the field");
    public static final MetaData FIELD_DEFAULT_VALUE_METADATA = new MetaData("Initial Value",
            "The initial value of the filed");
    public static final MetaData FUNCTION_RETURN_TYPE_METADATA = new MetaData("Return Type",
            "The return type of the function");
    public static final MetaData FUNCTION_NAME_METADATA = new MetaData("Function Name",
            "The name of the function");
    public static final MetaData FUNCTION_ACCESSOR_METADATA = new MetaData("Accessor",
            "The accessor of the function");
    public static final MetaData RESOURCE_NAME_METADATA = new MetaData("Resource Path",
            "The resource path");
    public static final MetaData RESOURCE_FUNCTION_RETURN_TYPE_METADATA = new MetaData("Return Type",
            "The return type of the resource");
    public static final MetaData RESOURCE_FUNCTION_DOCUMENTATION_METADATA = new MetaData("Description",
            "The description of the resource");
    public static final MetaData SERVICE_DOCUMENTATION_METADATA = new MetaData("Description", "The " +
            "description of the object");

    // organization names
    public static final String BALLERINA = "ballerina";

    // annotation attachment points
    public static final String OBJECT_METHOD = "OBJECT_METHOD";

    public static final String DEFAULT = "default";
    public static final String RESOURCE_CONFIG = "ResourceConfig";
    public static final String ANNOT_PREFIX = "annot";

    // GraphQL advance params
    public static final String GRAPHQL_CONTEXT = "context";
    public static final String GRAPHQL_FIELD = "'field";

    // GraphQL function property keys
    public static final String GRAPHQL_CONTEXT_KEY = "paramContext";
    public static final String GRAPHQL_FIELD_KEY = "paramField";
    public static final String VALUE_FIELD = "value";

    // Type API contexts
    public static final String GRAPHQL_FIELD_TYPE = "GRAPHQL_FIELD_TYPE";
    public static final String GRAPHQL_INPUT_TYPE = "GRAPHQL_INPUT_TYPE";

    // GraphQL types
    public static final String GRAPHQL_SCALAR_TYPE = "Scalar Types";
    public static final String GRAPHQL_USER_DEFINED_TYPE = "User Defined";
    public static final String GRAPHQL_ENUM_TYPE = "Enum Types";

    private Constants() {
    }
}
