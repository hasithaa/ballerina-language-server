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

package io.ballerina.designmodelgenerator.core.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a module client declaration node.
 *
 * @since 1.0.0
 */
public class Connection extends DesignGraphNode {

    private final String symbol;
    private Location location;
    private final Scope scope;
    private final String icon;
    private final Set<String> dependentFunctions;
    private final Set<String> dependentConnection;
    private String type = "Connection";

    public Connection(String symbol, String sortText, Location location, Scope scope, String icon) {
        super(sortText);
        this.symbol = symbol;
        this.location = location;
        this.scope = scope;
        this.icon = icon;
        this.dependentFunctions = new HashSet<>();
        this.dependentConnection = new HashSet<>();
    }

    public Connection(String symbol, String sortText, Location location, Scope scope, String icon,
                      boolean enableFlow) {
        super(enableFlow, sortText);
        this.symbol = symbol;
        this.location = location;
        this.scope = scope;
        this.icon = icon;
        this.dependentFunctions = new HashSet<>();
        this.dependentConnection = new HashSet<>();
    }

    public Connection(String symbol, String sortText, Location location, Scope scope, String icon,
                      boolean enableFlow, String type) {
        super(enableFlow, sortText);
        this.symbol = symbol;
        this.location = location;
        this.scope = scope;
        this.icon = icon;
        this.type = type;
        this.dependentFunctions = new HashSet<>();
        this.dependentConnection = new HashSet<>();
    }

    public enum Scope {
        LOCAL,
        GLOBAL
    }

    public String getIcon() {
        return icon;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getSymbol() {
        return symbol;
    }

    public Location getLocation() {
        return location;
    }

    public Scope getScope() {
        return scope;
    }

    public Set<String> getDependentFunctions() {
        return dependentFunctions;
    }

    public void addDependentFunction(String dependentFunction) {
        dependentFunctions.add(dependentFunction);
    }

    public Set<String> getDependentConnection() {
        return dependentConnection;
    }

    public void addDependentConnection(String dependentConnection) {
        this.dependentConnection.add(dependentConnection);
    }

    public String getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol.hashCode(), location.hashCode(), scope.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Connection connection)) {
            return false;
        }
        return Objects.equals(connection.getUuid(), this.getUuid());
    }
}
