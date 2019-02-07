/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.spec.tests;

import org.ballerinalang.launcher.LauncherUtils;
import org.ballerinalang.testerina.core.BTestRunner;
import org.ballerinalang.testerina.core.TesterinaConstants;
import org.ballerinalang.testerina.util.TesterinaUtils;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.FileSystemProjectDirectory;
import org.wso2.ballerinalang.compiler.SourceDirectory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.testng.Assert.assertFalse;

/**
 * Test class to run the Ballerina Specification Conformance Test Suite.
 *
 * @since 0.990.4
 */
public class SpecConformanceTests {

    @Test
    public void testSpecConformance() {
        BTestRunner testRunner = new BTestRunner();
        Path sourceRootPath = LauncherUtils.getSourceRootPath(Paths.get("").toString());
        SourceDirectory sourceDirectory = new FileSystemProjectDirectory(sourceRootPath);
        List<String> sourceFileList = sourceDirectory.getSourcePackageNames();
        Path[] paths = sourceFileList.stream()
                .map(Paths::get)
                .sorted()
                .toArray(Path[]::new);
        TesterinaUtils.setManifestConfigs(sourceRootPath);
        testRunner.runTest(sourceRootPath.toString(), paths, null, true);
        TesterinaUtils.cleanUpDir(sourceRootPath.resolve(TesterinaConstants.TESTERINA_TEMP_DIR));
        assertFalse(testRunner.getTesterinaReport().isFailure());
    }
}
