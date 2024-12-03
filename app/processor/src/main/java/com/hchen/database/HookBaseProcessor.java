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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.hchen.database;

import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.function.Consumer;

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
    int count = 0;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ++count;
        if (count > 1) return true;
        delayedOutput();
        try (Writer writer = processingEnv.getFiler().createSourceFile("com.sevtinge.hyperceiler.module.base.DataBase").openWriter()) {
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
                    
                      * Copyright (C) 2023-2024 HyperCeiler Contributions
                    */
                    package com.sevtinge.hyperceiler.module.base;
                    
                    import java.util.HashMap;
                    
                    /**
                     * 注解处理器自动生成的 Map 图
                     *
                     * @author 焕晨HChen
                     */
                    public class DataBase {
                        public String mTargetPackage;
                        public int mTargetSdk;
                        public float mTargetOSVersion;
                        public boolean isPad;
                    
                        public DataBase(String targetPackage, int targetSdk, float targetOSVersion, boolean isPad){
                            this.mTargetPackage = targetPackage;
                            this.mTargetSdk = targetSdk;
                            this.mTargetOSVersion = targetOSVersion;
                            this.isPad = isPad;
                        }
                    
                        public static HashMap<String, DataBase> get() {
                            HashMap<String, DataBase> dataMap = new HashMap<>();
                    """);
            roundEnv.getElementsAnnotatedWith(HookBase.class).forEach(new Consumer<Element>() {
                @Override
                public void accept(Element element) {
                    String fullClassName = null;
                    if (element instanceof TypeElement typeElement) {
                        fullClassName = typeElement.getQualifiedName().toString();
                        // System.out.println("Full class name: " + fullClassName);
                    }
                    if (fullClassName == null) {
                        System.out.println("W: Full class name is null!!!");
                    }
                    HookBase hookBase = element.getAnnotation(HookBase.class);
                    String targetPackage = hookBase.targetPackage();
                    int targetSdk = hookBase.targetSdk();
                    float targetOSVersion = hookBase.targetOSVersion();
                    boolean isPad = hookBase.isPad();
                    try {
                        writer.write("        ");
                        writer.write("dataMap.put(\"" + fullClassName + "\", new DataBase(\"" + targetPackage + "\", "
                                + targetSdk + ", " + targetOSVersion + "F, " + isPad + "));\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            writer.write("""
                            return dataMap;
                        }
                    }
                    """);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /*
     ________  ___  ___   ________   ________       ___    ___ 
    |\  _____\|\  \|\  \ |\   __  \ |\   __  \     |\  \  /  /|
    \ \  \__/ \ \  \\\  \\ \  \|\  \\ \  \|\  \    \ \  \/  / /
     \ \   __\ \ \  \\\  \\ \   _  _\\ \   _  _\    \ \    / / 
      \ \  \_|  \ \  \\\  \\ \  \\  \|\ \  \\  \|    \/  /  /  
       \ \__\    \ \_______\\ \__\\ _\ \ \__\\ _\  __/  / /    
        \|__|     \|_______| \|__|\|__| \|__|\|__||\___/ /     
                                                  \|___|/                                                   
    * */
    private void delayedOutput() {
        final String RESET = "\033[0m";  // Text Reset
        final String RED = "\033[0;31m";    // RED
        final String GREEN = "\033[0;32m";  // GREEN
        final String YELLOW = "\033[0;33m"; // YELLOW
        final String BLUE = "\033[0;34m";   // BLUE
        final String PURPLE = "\033[0;35m"; // PURPLE
        final String CYAN = "\033[0;36m";   // CYAN
        final String WHITE = "\033[0;37m";  // WHITE
        System.out.println(BLUE + "                                                            " + RESET);
        System.out.println(BLUE + " ________  ___  ___   ________   ________       ___    ___ " + RESET);
        System.out.println(BLUE + "|\\  _____\\|\\  \\|\\  \\ |\\   __  \\ |\\   __  \\     |\\  \\  /  /|" + RESET);
        System.out.println(BLUE + "\\ \\  \\__/ \\ \\  \\\\\\  \\\\ \\  \\|\\  \\\\ \\  \\|\\  \\    \\ \\  \\/  / /" + RESET);
        System.out.println(BLUE + " \\ \\   __\\ \\ \\  \\\\\\  \\\\ \\   _  _\\\\ \\   _  _\\    \\ \\    / / " + RESET);
        System.out.println(BLUE + "  \\ \\  \\_|  \\ \\  \\\\\\  \\\\ \\  \\\\  \\|\\ \\  \\\\  \\|    \\/  /  /  " + RESET);
        System.out.println(BLUE + "   \\ \\__\\    \\ \\_______\\\\ \\__\\\\ _\\ \\ \\__\\\\ _\\  __/  / /    " + RESET);
        System.out.println(BLUE + "    \\|__|     \\|_______| \\|__|\\|__| \\|__|\\|__||\\___/ /     " + RESET);
        System.out.println(BLUE + "                                              \\|___|/      " + RESET);
        System.out.println(BLUE + "                                                            " + RESET);
        System.out.println(BLUE + "                                 Code By 焕晨HChen  " + RESET); // 别改谢谢
    }

}