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

package io.ballerina.servicemodelgenerator.extension.builder;

import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.ServiceInitModel;
import io.ballerina.servicemodelgenerator.extension.model.context.AddServiceInitModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.GetServiceInitModelContext;
import org.eclipse.lsp4j.TextEdit;

import java.util.List;
import java.util.Map;

/**
 * Interface for building service model specific models.
 *
 * @since 1.3.0
 **/
public interface ServiceNodeBuilder extends NodeBuilder<Service> {

    /**
     * Get the unified listener and service declaration model.
     */
    ServiceInitModel getServiceInitModel(GetServiceInitModelContext context);

    /**
     * Get the listener and service declaration source.
     */
    Map<String, List<TextEdit>> addServiceInitSource(AddServiceInitModelContext context);
}
