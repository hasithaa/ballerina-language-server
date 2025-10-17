/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.core.model;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.DiagnosticHandler;
import io.ballerina.modelgenerator.commons.ModuleInfo;

import java.util.List;
import java.util.Map;

/**
 * @param name                  type Symbol Name
 * @param editable              is editable type
 * @param metadata              type symbol short display details
 * @param codedata              codedata of the type
 * @param properties            properties of the type
 * @param members               members of the type
 * @param restMember            rest member of the type
 * @param includes              type inclusions of the type
 * @param functions             functions of a class or object
 * @param annotationAttachments annotations of the type
 * @param allowAdditionalFields allow additional fields
 */
public record TypeData(
        String name,
        boolean editable,
        Metadata metadata,
        Codedata codedata,
        Map<String, Property> properties,
        List<Member> members,
        Member restMember,
        List<String> includes,
        List<Function> functions,
        List<AnnotationAttachment> annotationAttachments,
        boolean allowAdditionalFields
) {

    public static class TypeDataBuilder {

        private String name;
        private boolean editable = false;
        private List<Member> members;
        private Member restMember;
        private List<Function> functions;
        private List<String> includes;
        private List<AnnotationAttachment> annotationAttachments;
        protected Metadata.Builder<TypeDataBuilder> metadataBuilder;
        protected Codedata.Builder<TypeDataBuilder> codedataBuilder;
        protected FormBuilder<TypeDataBuilder> formBuilder;
        protected ModuleInfo moduleInfo;
        protected SemanticModel semanticModel;
        protected boolean allowAdditionalFields;
        protected DiagnosticHandler diagnosticHandler;

        public TypeDataBuilder() {
        }

        public String name() {
            return this.name;
        }

        public TypeDataBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TypeDataBuilder editable() {
            this.editable = true;
            return this;
        }

        public Metadata.Builder<TypeDataBuilder> metadata() {
            if (this.metadataBuilder == null) {
                this.metadataBuilder = new Metadata.Builder<>(this);
            }
            return this.metadataBuilder;
        }

        public Codedata.Builder<TypeDataBuilder> codedata() {
            if (this.codedataBuilder == null) {
                this.codedataBuilder = new Codedata.Builder<>(this);
            }
            return this.codedataBuilder;
        }

        public FormBuilder<TypeDataBuilder> properties() {
            if (this.formBuilder == null) {
                this.formBuilder = new FormBuilder<>(semanticModel, diagnosticHandler, moduleInfo, this);
            }
            return this.formBuilder;
        }

        public TypeDataBuilder members(List<Member> members) {
            this.members = members;
            return this;
        }

        public TypeDataBuilder restMember(Member restMember) {
            this.restMember = restMember;
            return this;
        }

        public TypeDataBuilder functions(List<Function> functions) {
            this.functions = functions;
            return this;
        }

        public TypeDataBuilder includes(List<String> includes) {
            this.includes = includes;
            return this;
        }

        public TypeDataBuilder annotationAttachments(List<AnnotationAttachment> annotationAttachments) {
            this.annotationAttachments = annotationAttachments;
            return this;
        }

        public TypeDataBuilder allowAdditionalFields(boolean allowAdditionalFields) {
            this.allowAdditionalFields = allowAdditionalFields;
            return this;
        }

        public TypeDataBuilder semanticModel(SemanticModel semanticModel) {
            this.semanticModel = semanticModel;
            return this;
        }

        public TypeDataBuilder diagnosticHandler(DiagnosticHandler diagnosticHandler) {
            this.diagnosticHandler = diagnosticHandler;
            return this;
        }

        public TypeDataBuilder defaultModuleName(ModuleInfo moduleInfo) {
            this.moduleInfo = moduleInfo;
            return this;
        }

        public TypeData build() {
            return new TypeData(
                    name,
                    editable,
                    metadataBuilder == null ? null : metadataBuilder.build(),
                    codedataBuilder == null ? null : codedataBuilder.build(),
                    formBuilder == null ? null : formBuilder.build(),
                    members,
                    restMember,
                    includes,
                    functions,
                    annotationAttachments,
                    allowAdditionalFields
            );
        }
    }

    public record AnnotationAttachment(String modulePrefix, String name, Map<String, Property> properties) {

        @Override
        public String toString() {
            if (name == null || name.isEmpty()) {
                return "";
            }

            if (modulePrefix == null || modulePrefix.isEmpty()) {
                return "@" + name;
            }

            return "@" + modulePrefix + ":" + name;
        }
    }
}
