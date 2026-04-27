/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.core.model.node.builtin;

import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.PropertyType;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;

import java.util.List;
import java.util.Map;

/**
 * Defines the contract for builtin activity types (REST, SOAP, Email).
 * Each strategy provides form fields for the UI and generates the corresponding Ballerina source code.
 *
 * @since 1.8.0
 */
public interface BuiltinActivityStrategy {

    /**
     * Represents a Ballerina import (org and module pair).
     *
     * @param org    the organisation (e.g. "ballerina")
     * @param module the module name (e.g. "http")
     */
    record Import(String org, String module) {
    }

    /**
     * Sets the form fields for this activity type using the fluent PropertiesBuilder API.
     *
     * @param nodeBuilder the node builder to add properties to
     * @param context     the template context for resolving symbols
     */
    void setFormProperties(NodeBuilder nodeBuilder, NodeBuilder.TemplateContext context);

    /**
     * Generates the activity function source code and returns the text edits.
     * This generates:
     * 1. Configurable variables for auth/connection config
     * 2. The @workflow:Activity annotated function with inline client code
     *
     * @param sourceBuilder the source builder used for code generation
     * @return the generated source code as a string for the function body
     */
    String generateActivityFunctionBody(SourceBuilder sourceBuilder);

    /**
     * Returns the parameter list string for the generated activity function.
     *
     * @param sourceBuilder the source builder
     * @return the parameters string (e.g., "string resourcePath, json payload")
     */
    String getActivityFunctionParams(SourceBuilder sourceBuilder);

    /**
     * Returns the return type for the generated activity function.
     *
     * @param sourceBuilder the source builder
     * @return the return type string (e.g., "json|error")
     */
    String getActivityReturnType(SourceBuilder sourceBuilder);

    /**
     * Returns the configurable variable declarations to be generated.
     * Default implementation returns an empty list (no configurables).
     *
     * @param sourceBuilder the source builder
     * @param activityName  the name of the activity (used as prefix for configurable names)
     * @return list of configurable variable declaration strings
     */
    default List<String> getConfigurableDeclarations(SourceBuilder sourceBuilder, String activityName) {
        return List.of();
    }

    /**
     * Returns the import statements required by this activity type.
     *
     * @param sourceBuilder the source builder
     * @return list of imports required by this activity
     */
    List<Import> getRequiredImports(SourceBuilder sourceBuilder);

    /**
     * Returns the default function name prefix for this activity type.
     *
     * @return the default name prefix (e.g., "callRest", "callSoap", "sendEmail")
     */
    String getDefaultFunctionNamePrefix();

    /**
     * Returns the default return type shown in the form's Return Type field.
     * Override this to change from the default "json".
     *
     * @return the default return type (e.g., "json", "xml")
     */
    default String getDefaultFormReturnType() {
        return "json";
    }

    /**
     * Sets the post-strategy form properties (return type, variable name, etc.).
     * Override this to customize the form layout for return type and variable name.
     * Default implementation adds Return Type and Result Variable Name fields.
     *
     * @param nodeBuilder the node builder to add properties to
     * @param context     the template context for resolving symbols
     */
    default void setPostProperties(NodeBuilder nodeBuilder, NodeBuilder.TemplateContext context) {
        nodeBuilder.properties().returnType(getDefaultFormReturnType(), null, false);
        nodeBuilder.properties().data(Property.RESULT_NAME, context.getAllVisibleSymbolNames(),
                Property.RESULT_NAME, Property.RESULT_DOC, false);
    }

    /**
     * Returns the label for this activity type.
     *
     * @return the display label
     */
    String getLabel();

    /**
     * Returns the description for this activity type.
     *
     * @return the display description
     */
    String getDescription();

    /**
     * Builds the argument entries for the call activity invocation.
     * Strategies should override this to return the named argument list built from
     * their own known parameter keys and the node properties — rather than re-parsing
     * the formatted string from {@link #getActivityFunctionParams}.
     *
     * @param sourceBuilder the source builder
     * @return list of argument entries (e.g. {@code "url: \"https://...\""})
     */
    default List<String> getCallActivityArgs(SourceBuilder sourceBuilder) {
        return List.of();
    }

    /**
     * Adds a named argument to {@code args}, quoting the value as a Ballerina string literal
     * when the property's currently-selected type is {@link Property.ValueType#TEXT}
     * (i.e., the user entered plain text, not an expression). Skips when the property is
     * missing or its value is empty.
     */
    static void addQuotedArg(List<String> args, String paramName,
                             Map<String, Property> properties, String propKey) {
        if (properties == null) {
            return;
        }
        Property prop = properties.get(propKey);
        if (prop == null || prop.value() == null || prop.value().toString().isEmpty()) {
            return;
        }
        String value = prop.value().toString();
        if (isTextSelected(prop)) {
            value = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        args.add(paramName + ": " + value);
    }

    /**
     * Returns {@code true} when the currently-selected type for the given property is
     * {@link Property.ValueType#TEXT} — meaning the raw user input must be wrapped in
     * Ballerina string quotes when emitted as source.
     */
    static boolean isTextSelected(Property prop) {
        if (prop == null || prop.types() == null) {
            return false;
        }
        return prop.types().stream()
                .filter(PropertyType::selected)
                .findFirst()
                .map(t -> t.fieldType() == Property.ValueType.TEXT)
                .orElse(false);
    }
}
