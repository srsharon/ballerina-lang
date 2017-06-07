/*
*   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
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
package org.ballerinalang.nativeimpl.lang.files;

import org.ballerinalang.bre.Context;
import org.ballerinalang.model.types.TypeEnum;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.AbstractNativeFunction;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.Attribute;
import org.ballerinalang.natives.annotations.BallerinaAnnotation;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.util.exceptions.BallerinaException;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copy Function
 */
@BallerinaFunction(
        packageName = "ballerina.lang.files",
        functionName = "copy",
        args = {@Argument(name = "source", type = TypeEnum.STRUCT, structType = "File",
                structPackage = "ballerina.lang.files"),
                @Argument(name = "destination", type = TypeEnum.STRUCT, structType = "File",
                        structPackage = "ballerina.lang.files")},
        isPublic = true
)
@BallerinaAnnotation(annotationName = "Description", attributes = { @Attribute(name = "value",
        value = "This function copies a file from a given location to another") })
@BallerinaAnnotation(annotationName = "Param", attributes = { @Attribute(name = "source",
        value = "File that should be copied") })
@BallerinaAnnotation(annotationName = "Param", attributes = { @Attribute(name = "destination",
        value = "The location where the File should be pasted") })
public class Copy extends AbstractNativeFunction {

    @Override public BValue[] execute(Context context) {

        BStruct source = (BStruct) getArgument(context, 0);
        BStruct destination = (BStruct) getArgument(context, 1);

            File file = new File(source.getValue(0).stringValue());
            File destinationFile = new File(destination.getValue(0).stringValue());

            File parent = destinationFile.getParentFile();
            if (parent != null) {
                if (!parent.exists()) {
                    if (!parent.mkdirs()) {
                        throw new BallerinaException("Error in writing file");
                    }
                }
            }
            if (!copy(file, destinationFile)) {
                throw new BallerinaException("Error while copying file");
            }

        return VOID_RETURN;
    }

    private boolean copy(File src, File dest) {

        InputStream in = null;
        OutputStream out = null;
        try {

            if (src.isDirectory()) {

                if (!dest.exists()) {
                    if (!dest.mkdir()) {
                        return false;
                    }
                }

                String files[] = src.list();
                if (files == null) {
                    return false;
                }
                for (String file : files) {
                    File srcFile = new File(src, file);
                    File destFile = new File(dest, file);
                    //recursive copy
                    if (!copy(srcFile, destFile)) {
                        return false;
                    }
                }

            } else {

                in = new FileInputStream(src);
                out = new FileOutputStream(dest);

                byte[] buffer = new byte[1024];

                int length;

                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

            }
            return true;
        } catch (IOException e) {
            throw new BallerinaException("Error while copying file");
        } finally {
            if (in != null) {
                closeQuietly(in);
            }
            if (out != null) {
                closeQuietly(out);
            }
        }
    }

    private void closeQuietly(Closeable resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (Exception e) {
            throw new BallerinaException("Exception during Resource.close()", e);
        }
    }
}
