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

package io.ballerina.servicemodelgenerator.extension.model;

import io.ballerina.modelgenerator.commons.Annotation;
import io.ballerina.servicemodelgenerator.extension.util.Constants;
import io.ballerina.servicemodelgenerator.extension.util.ServiceClassUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.ballerina.servicemodelgenerator.extension.util.Constants.ANNOT_PREFIX;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.FIELD_NAME_METADATA;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.FIELD_TYPE_METADATA;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.FUNCTION_NAME_METADATA;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.FUNCTION_RETURN_TYPE_METADATA;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.KIND_OBJECT_METHOD;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.RESOURCE_FUNCTION_RETURN_TYPE_METADATA;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.RESOURCE_NAME_METADATA;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.VALUE_TYPE_EXPRESSION;

/**
 * Represents a function in a service declaration or in a service class.
 *
 * @since 1.0.0
 */
public class Function {
    private MetaData metadata;
    private List<String> qualifiers;
    private String kind;
    private Value accessor;
    private Value name;
    private List<Parameter> parameters;
    private Map<String, Parameter> schema;
    private FunctionReturnType returnType;
    private boolean enabled;
    private boolean optional;
    private boolean editable;
    private boolean canAddParameters;
    private Codedata codedata;
    private Map<String, Value> properties;

    public Function(MetaData metadata, List<String> qualifiers, String kind, Value accessor, Value name,
                    List<Parameter> parameters, Map<String, Parameter> schema, FunctionReturnType returnType,
                    boolean enabled, boolean optional, boolean editable, boolean canAddParameters, Codedata codedata,
                    Map<String, Value> properties) {
        this.metadata = metadata;
        this.qualifiers = qualifiers;
        this.kind = kind;
        this.accessor = accessor;
        this.name = name;
        this.parameters = parameters;
        this.schema = schema;
        this.returnType = returnType;
        this.enabled = enabled;
        this.optional = optional;
        this.editable = editable;
        this.codedata = codedata;
        this.properties = properties;
    }

    public static Function getNewFunctionModel(ServiceClassUtil.ServiceClassContext context) {
        return switch (context) {
            case GRAPHQL_DIAGRAM -> buildGraphqlFunction();
            case TYPE_DIAGRAM -> buildTypeDiagramFunction();
            case SERVICE_DIAGRAM -> buildServiceFunction();
            default -> buildDefaultFunction();
        };
    }

    private static FunctionBuilder createBaseFunctionBuilder() {
        return new FunctionBuilder()
                .metadata("", "")
                .accessor(functionAccessor())
                .parameters(new ArrayList<>())
                .kind(KIND_OBJECT_METHOD)
                .enabled(true);
    }

    private static Function buildGraphqlFunction() {
        return createBaseFunctionBuilder()
                .name(name(FIELD_NAME_METADATA))
                .returnType(returnType(FIELD_TYPE_METADATA))
                .schema(createParameterSchema(Parameter.graphqlParamSchema()))
                .build();
    }

    private static Function buildServiceFunction() {
        return createBaseFunctionBuilder()
                .name(name(FUNCTION_NAME_METADATA))
                .returnType(returnType(FUNCTION_RETURN_TYPE_METADATA))
                .schema(createParameterSchema(Parameter.functionParamSchema()))
                .build();
    }

    private static Function buildDefaultFunction() {
        return createBaseFunctionBuilder()
                .name(name(FUNCTION_NAME_METADATA))
                .returnType(returnType(FUNCTION_RETURN_TYPE_METADATA))
                .schema(createParameterSchema(Parameter.functionParamSchema()))
                .build();
    }

    private static Function buildTypeDiagramFunction() {
        return createBaseFunctionBuilder()
                .name(name(RESOURCE_NAME_METADATA))
                .returnType(returnType(RESOURCE_FUNCTION_RETURN_TYPE_METADATA))
                .schema(createParameterSchema(Parameter.functionParamSchema()))
                .setProperties(createResourceConfigAnnotation())
                .build();
    }

    private static Map<String, Parameter> createParameterSchema(Parameter paramSchema) {
        return Map.of(Constants.PARAMETER, paramSchema);
    }

    private static Map<String, Value> createResourceConfigAnnotation() {
        Annotation annotation = new Annotation(
                "ResourceConfig",
                "Resource Configuration",
                "Configuration related to the resource function.",
                "ResourceConfig",
                null, null, null
        );

        Value annotationValue = createAnnotation(annotation);
        annotationValue.setEnabled(false);

        return Map.of(ANNOT_PREFIX + annotation.annotationName(), annotationValue);
    }

    private static Value functionAccessor() {
        return new Value.ValueBuilder()
                .setMetadata(Constants.FUNCTION_ACCESSOR_METADATA)
                .valueType(Constants.VALUE_TYPE_IDENTIFIER)
                .enabled(true)
                .editable(true)
                .build();
    }

    private static Value name(MetaData metadata) {
        return new Value.ValueBuilder()
                .setMetadata(metadata)
                .valueType(Constants.VALUE_TYPE_IDENTIFIER)
                .enabled(true)
                .editable(true)
                .build();
    }

    public static FunctionReturnType returnType(MetaData metadata) {
        Value value = new Value.ValueBuilder()
                .setMetadata(metadata)
                .valueType(Constants.VALUE_TYPE_TYPE)
                .enabled(true)
                .editable(true)
                .optional(true)
                .build();
        return new FunctionReturnType(value);
    }

    public static Map<String, Value> createAnnotationsMap(List<Annotation> annotations) {
        Map<String, Value> annotationMap = new HashMap<>();
        for (Annotation annotation : annotations) {
            Value value = createAnnotation(annotation);
            String annotKey = ANNOT_PREFIX + annotation.annotationName();
            annotationMap.put(annotKey, value);
        }
        return annotationMap;
    }

    public static Value createAnnotation(Annotation annotation) {
        Codedata codedata = new Codedata.Builder()
                .setType("ANNOTATION_ATTACHMENT")
                .setOriginalName(annotation.annotationName())
                .setOrgName(annotation.orgName())
                .setModuleName(annotation.moduleName())
                .build();
        String[] parts = annotation.typeConstrain().split(":");
        String type = parts.length > 1 ? parts[1] : parts[0];
        return new Value.ValueBuilder()
                .setMetadata(new MetaData(annotation.displayName(), annotation.description()))
                .setCodedata(codedata)
                .valueType(VALUE_TYPE_EXPRESSION)
                .setPlaceholder("{}")
                .setValueTypeConstraint(annotation.typeConstrain())
                .enabled(true)
                .editable(true)
                .optional(true)
                .setAdvanced(true)
                .setMembers(List.of(new PropertyTypeMemberInfo(type,
                        annotation.packageIdentifier(), "RECORD_TYPE", false)))
                .build();
    }

    public MetaData getMetadata() {
        return metadata;
    }

    public void setMetadata(MetaData metadata) {
        this.metadata = metadata;
    }

    public List<String> getQualifiers() {
        return qualifiers;
    }

    public void setQualifiers(List<String> qualifiers) {
        this.qualifiers = qualifiers;
    }

    public void addQualifier(String qualifier) {
        this.qualifiers.add(qualifier);
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Value getAccessor() {
        return accessor;
    }

    public void setAccessor(Value accessor) {
        this.accessor = accessor;
    }

    public Value getName() {
        return name;
    }

    public void setName(Value name) {
        this.name = name;
    }

    public List<Parameter> getParameters() {
        return Objects.isNull(parameters) ? new ArrayList<>() : parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }

    public FunctionReturnType getReturnType() {
        return returnType;
    }

    public void setReturnType(FunctionReturnType returnType) {
        this.returnType = returnType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public Codedata getCodedata() {
        return codedata;
    }

    public void setCodedata(Codedata codedata) {
        this.codedata = codedata;
    }

    public Map<String, Parameter> getSchema() {
        return schema;
    }

    public void setSchema(Map<String, Parameter> schema) {
        this.schema = schema;
    }

    public Map<String, Value> getProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    public void setProperties(Map<String, Value> properties) {
        this.properties = properties;
    }

    public void addProperty(String key, Value property) {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }
        this.properties.put(key, property);
    }

    public Value getProperty(String key) {
        if (this.properties == null) {
            return null;
        }
        return this.properties.get(key);
    }

    public boolean isCanAddParameters() {
        return canAddParameters;
    }

    public void setCanAddParameters(boolean canAddParameters) {
        this.canAddParameters = canAddParameters;
    }

    public static class FunctionBuilder {
        private MetaData metadata;
        private Codedata codedata;
        private List<String> qualifiers;
        private String kind;
        private Value accessor;
        private Value name;
        private List<Parameter> parameters;
        private Map<String, Parameter> schema;
        private FunctionReturnType returnType;
        private boolean enabled = false;
        private boolean optional = false;
        private boolean editable = false;
        private boolean canAddParameters = false;
        private Map<String, Value> properties;

        public FunctionBuilder metadata(String label, String description) {
            this.metadata = new MetaData(label, description);
            return this;
        }

        public FunctionBuilder setMetadata(MetaData metadata) {
            this.metadata = metadata;
            return this;
        }

        public FunctionBuilder setQualifiers(List<String> qualifiers) {
            this.qualifiers = qualifiers;
            return this;
        }

        public FunctionBuilder kind(String kind) {
            this.kind = kind;
            return this;
        }

        public FunctionBuilder accessor(Value accessor) {
            this.accessor = accessor;
            return this;
        }

        public FunctionBuilder name(Value name) {
            this.name = name;
            return this;
        }

        public FunctionBuilder parameters(List<Parameter> parameters) {
            this.parameters = parameters;
            return this;
        }

        public FunctionBuilder schema(Map<String, Parameter> schema) {
            this.schema = schema;
            return this;
        }

        public FunctionBuilder returnType(FunctionReturnType returnType) {
            this.returnType = returnType;
            return this;
        }

        public FunctionBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public FunctionBuilder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public FunctionBuilder editable(boolean editable) {
            this.editable = editable;
            return this;
        }

        public FunctionBuilder canAddParameters(boolean canAddParameters) {
            this.canAddParameters = canAddParameters;
            return this;
        }

        public FunctionBuilder setCodedata(Codedata codedata) {
            this.codedata = codedata;
            return this;
        }

        public FunctionBuilder setProperties(Map<String, Value> properties) {
            this.properties = properties;
            return this;
        }

        public Function build() {
            return new Function(metadata, qualifiers, kind, accessor, name, parameters, schema, returnType, enabled,
                    optional, editable, canAddParameters, codedata, properties);
        }
    }
}
