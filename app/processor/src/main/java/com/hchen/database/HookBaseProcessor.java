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

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.hchen.database.HookBase")
@SupportedSourceVersion(SourceVersion.RELEASE_22)
public class HookBaseProcessor extends AbstractProcessor {
    int count = 0;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ++count;
        if (count > 1) return true;
        delayedOutput();
        try (Writer writer = processingEnv.getFiler().createSourceFile("com.sevtinge.hyperceiler.module.base.DataBase").openWriter()) {
            writer.write("""
                    package com.sevtinge.hyperceiler.module.base;
                                        
                    import java.util.HashMap;
                                        
                    public class DataBase {
                        public static class DataHelper {
                            public String fullName;
                            public int android;
                            public boolean isPad;
                            public boolean skip;
                                        
                            public DataHelper(String fullName,int android,boolean isPad,boolean skip){
                                this.fullName = fullName;
                                this.android = android;
                                this.isPad = isPad;
                                this.skip = skip;
                            }
                        }
                                        
                        public static HashMap<String, DataHelper> get() {
                            HashMap<String, DataHelper> dataMap = new HashMap<>();
                    """);
            roundEnv.getElementsAnnotatedWith(HookBase.class).forEach(new Consumer<Element>() {
                @Override
                public void accept(Element element) {
                    String fullClassName = null;
                    if (element instanceof TypeElement typeElement) {
                        fullClassName = getFullClassName(typeElement);
                        // System.out.println("Full class name: " + fullClassName);
                    }
                    if (fullClassName == null) {
                        System.out.println("W: Full class name is null!!!");
                    }
                    HookBase hookBase = element.getAnnotation(HookBase.class);
                    String pkg = hookBase.pkg();
                    int android = hookBase.tarAndroid();
                    boolean isPad = hookBase.isPad();
                    boolean skip = hookBase.skip();
                    try {
                        writer.write("        ");
                        writer.write("dataMap.put(\"" + pkg + "\", new DataHelper(\"" + fullClassName + "\", "
                                + android + ", " + isPad + ", " + skip + "));\n");
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

    private String getFullClassName(TypeElement typeElement) {
        return typeElement.getQualifiedName().toString();
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
        System.out.println(BLUE + " ________  ___  ___   ________   ________       ___    ___ " + RESET);
        System.out.println(BLUE + "|\\  _____\\|\\  \\|\\  \\ |\\   __  \\ |\\   __  \\     |\\  \\  /  /|" + RESET);
        System.out.println(BLUE + "\\ \\  \\__/ \\ \\  \\\\\\  \\\\ \\  \\|\\  \\\\ \\  \\|\\  \\    \\ \\  \\/  / /" + RESET);
        System.out.println(BLUE + " \\ \\   __\\ \\ \\  \\\\\\  \\\\ \\   _  _\\\\ \\   _  _\\    \\ \\    / / " + RESET);
        System.out.println(BLUE + "  \\ \\  \\_|  \\ \\  \\\\\\  \\\\ \\  \\\\  \\|\\ \\  \\\\  \\|    \\/  /  /  " + RESET);
        System.out.println(BLUE + "   \\ \\__\\    \\ \\_______\\\\ \\__\\\\ _\\ \\ \\__\\\\ _\\  __/  / /    " + RESET);
        System.out.println(BLUE + "    \\|__|     \\|_______| \\|__|\\|__| \\|__|\\|__||\\___/ /     " + RESET);
        System.out.println(BLUE + "                                              \\|___|/      " + RESET);
        System.out.println(BLUE + "                                                            " + RESET);
        System.out.println(BLUE + "                                               Code By HChenX      " + RESET);
    }

}