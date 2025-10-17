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

package io.ballerina.servicemodelgenerator.extension.model.response;

import org.eclipse.lsp4j.TextEdit;

import java.util.List;
import java.util.Map;

/**
 * Represents the response for the add or get default listener operation.
 *
 * @since 1.0.0
 */
public class AddOrGetDefaultListenerResponse extends AbstractServiceModelResponse {
    private String defaultListenerRef;
    private Map<String, List<TextEdit>> textEdits;

    public AddOrGetDefaultListenerResponse() {
    }

    public AddOrGetDefaultListenerResponse(Throwable e) {
        super.setError(e);
    }

    public String getDefaultListenerRef() {
        return defaultListenerRef;
    }

    public void setDefaultListenerRef(String defaultListenerRef) {
        this.defaultListenerRef = defaultListenerRef;
    }

    public Map<String, List<TextEdit>> getTextEdits() {
        return textEdits;
    }

    public void setTextEdits(Map<String, List<TextEdit>> textEdits) {
        this.textEdits = textEdits;
    }
}
