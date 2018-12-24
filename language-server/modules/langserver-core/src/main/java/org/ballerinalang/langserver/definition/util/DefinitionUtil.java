/*
 * Copyright (c) 2018, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.langserver.definition.util;

import org.ballerinalang.langserver.common.constants.ContextConstants;
import org.ballerinalang.langserver.common.constants.NodeContextKeys;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.compiler.DocumentServiceKeys;
import org.ballerinalang.langserver.compiler.LSPackageLoader;
import org.ballerinalang.langserver.compiler.LSServiceOperationContext;
import org.ballerinalang.langserver.compiler.common.LSDocument;
import org.ballerinalang.langserver.definition.DefinitionTreeVisitor;
import org.ballerinalang.model.elements.PackageID;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.util.CompilerContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for go to definition functionality of language server.
 */
public class DefinitionUtil {
    /**
     * Get definition position for the given definition context.
     *
     * @param definitionContext context of the definition.
     * @return {@link List} list of locations
     */
    public static List<Location> getDefinitionPosition(LSServiceOperationContext definitionContext) {
        List<Location> contents = new ArrayList<>();
        if (definitionContext.get(NodeContextKeys.SYMBOL_KIND_OF_NODE_KEY) == null) {
            return contents;
        }
        String nodeKind = definitionContext.get(NodeContextKeys.SYMBOL_KIND_OF_NODE_KEY);

        BLangPackage bLangPackage = getPackageOfTheOwner(definitionContext
                .get(NodeContextKeys.NODE_OWNER_PACKAGE_KEY), definitionContext);
        BLangNode bLangNode = null;
        switch (nodeKind) {
            case ContextConstants.FUNCTION:
                bLangNode = bLangPackage.functions.stream()
                        .filter(function -> function.name.getValue()
                                .equals(definitionContext.get(NodeContextKeys.NAME_OF_NODE_KEY)))
                        .findAny().orElse(null);
                break;
            case ContextConstants.OBJECT:
            case ContextConstants.RECORD:
            case ContextConstants.TYPE_DEF:
                bLangNode = bLangPackage.typeDefinitions.stream()
                        .filter(typeDef -> typeDef.name.getValue()
                                .equals(definitionContext.get(NodeContextKeys.NAME_OF_NODE_KEY)))
                        .findAny().orElse(null);
                break;
            case ContextConstants.ENDPOINT:
                bLangNode = bLangPackage.globalEndpoints.stream()
                        .filter(globalEndpoint -> globalEndpoint.name.value
                                .equals(definitionContext.get(NodeContextKeys.NAME_OF_NODE_KEY)))
                        .findAny().orElse(null);

                if (bLangNode == null) {
                    DefinitionTreeVisitor definitionTreeVisitor = new DefinitionTreeVisitor(definitionContext);
                    definitionContext.get(NodeContextKeys.NODE_STACK_KEY).pop().accept(definitionTreeVisitor);
                    if (definitionContext.get(NodeContextKeys.NODE_KEY) != null) {
                        bLangNode = definitionContext.get(NodeContextKeys.NODE_KEY);
                    }
                }

                break;
            case ContextConstants.VARIABLE:
                bLangNode = bLangPackage.globalVars.stream()
                        .filter(globalVar -> globalVar.name.getValue()
                                .equals(definitionContext.get(NodeContextKeys.VAR_NAME_OF_NODE_KEY)))
                        .findAny().orElse(null);

                if (bLangNode == null) {
                    bLangNode = bLangPackage.constants.stream()
                            .filter(constant -> constant.name.getValue()
                                    .equals(definitionContext.get(NodeContextKeys.VAR_NAME_OF_NODE_KEY)))
                            .findAny().orElse(null);
                }

                // BLangNode is null only when node at the cursor position is a local variable.
                if (bLangNode == null) {
                    DefinitionTreeVisitor definitionTreeVisitor = new DefinitionTreeVisitor(definitionContext);
                    definitionContext.get(NodeContextKeys.NODE_STACK_KEY).pop().accept(definitionTreeVisitor);
                    if (definitionContext.get(NodeContextKeys.NODE_KEY) != null) {
                        bLangNode = definitionContext.get(NodeContextKeys.NODE_KEY);
                        break;
                    }
                }
                break;
            default:
                break;
        }
        if (bLangNode == null) {
            return contents;
        }

        Location l = new Location();
        TextDocumentPositionParams position = definitionContext.get(DocumentServiceKeys.POSITION_KEY);
        String parentPath = new LSDocument(position.getTextDocument().getUri()).getSourceRoot();
        if (parentPath != null) {
            String fileName = bLangNode.getPosition().getSource().getCompilationUnitName();
            Path filePath = Paths
                    .get(CommonUtil.getPackageURI(definitionContext.get(NodeContextKeys.PACKAGE_OF_NODE_KEY)
                            .name.getValue(), parentPath, definitionContext
                            .get(NodeContextKeys.PACKAGE_OF_NODE_KEY).name.getValue()), fileName);
            l.setUri(filePath.toUri().toString());
            Range r = new Range();
            // Subtract 1 to convert the token lines and char positions to zero based indexing
            r.setStart(new Position(bLangNode.getPosition().getStartLine() - 1,
                    bLangNode.getPosition().getStartColumn() - 1));
            r.setEnd(new Position(bLangNode.getPosition().getEndLine() - 1,
                    bLangNode.getPosition().getEndColumn() - 1));
            l.setRange(r);
            contents.add(l);
        }

        return contents;

    }

    /**
     * Get the package of the owner of given node.
     *
     * @param packageID         package id
     * @param definitionContext definition context
     * @return {@link BLangPackage} package of the owner
     */
    private static BLangPackage getPackageOfTheOwner(PackageID packageID, LSServiceOperationContext definitionContext) {
        CompilerContext compilerContext = definitionContext.get(DocumentServiceKeys.COMPILER_CONTEXT_KEY);
        return LSPackageLoader.getPackageById(compilerContext, packageID);
    }
}
