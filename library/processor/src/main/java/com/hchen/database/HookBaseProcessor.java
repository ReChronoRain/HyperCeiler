/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.hchen.database;

import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * @author 焕晨HChen
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.hchen.database.HookBase")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class HookBaseProcessor extends AbstractProcessor {
    private int count = 0;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (++count > 1) return true;

        try (Writer writer = processingEnv.getFiler()
            .createSourceFile("com.sevtinge.hyperceiler.libhook.base.DataBase")
            .openWriter()) {

            writeHeader(writer);
            writeDataEntries(writer, roundEnv);
            writeFooter(writer);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void writeHeader(Writer writer) throws IOException {
        writer.write("""
                /*
                 * This file is part of HyperCeiler.

                 * HyperCeiler is free software: you can redistribute it and/or modify
                 * it under the terms of the GNU Affero General Public License as
                 * published by the Free Software Foundation, either version 3 of the
                 * License.

                 * This program is distributed in the hope that it will be useful,
                 * but WITHOUT ANY WARRANTY; without even the implied warranty of
                 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
                 * GNU Affero General Public License for more details.

                 * You should have received a copy of the GNU Affero General Public License
                 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

                 * Copyright (C) 2023-2026 HyperCeiler Contributions
                 */
                package com.sevtinge.hyperceiler.libhook.base;

                import java.util.HashMap;

                /**
                 * 注解处理器自动生成的模块数据
                 *
                 * @author 焕晨HChen
                 */
                public class DataBase {
                    public final String targetPackage;
                    public final int minSdk;
                    public final int maxSdk;
                    public final float minOSVersion;
                    public final float maxOSVersion;
                    public final int deviceType;

                    public DataBase(String targetPackage, int minSdk, int maxSdk,
                                   float minOSVersion, float maxOSVersion, int deviceType) {
                        this.targetPackage = targetPackage;
                        this.minSdk = minSdk;
                        this.maxSdk = maxSdk;
                        this.minOSVersion = minOSVersion;
                        this.maxOSVersion = maxOSVersion;
                        this.deviceType = deviceType;
                    }

                    public static HashMap<String, DataBase> get() {
                        HashMap<String, DataBase> dataMap = new HashMap<>();
                """);
    }

    private void writeDataEntries(Writer writer, RoundEnvironment roundEnv) throws IOException {
        for (Element element : roundEnv.getElementsAnnotatedWith(HookBase.class)) {
            if (!(element instanceof TypeElement typeElement)) {
                System.out.println("W: Element is not TypeElement!");
                continue;
            }

            String fullClassName = typeElement.getQualifiedName().toString();
            HookBase hookBase = element.getAnnotation(HookBase.class);

            writer.write(String.format(
                "        dataMap.put(\"%s\", new DataBase(\"%s\", %d, %d, %sF, %sF, %d));\n",
                fullClassName,
                hookBase.targetPackage(),
                hookBase.minSdk(),
                hookBase.maxSdk(),
                hookBase.minOSVersion(),
                hookBase.maxOSVersion(),
                hookBase.deviceType()));
        }
    }

    private void writeFooter(Writer writer) throws IOException {
        writer.write("""
                        return dataMap;
                    }
                }""");
    }
}
