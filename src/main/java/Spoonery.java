import spoon.Launcher;
import spoon.SpoonException;
import spoon.compiler.SpoonResource;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.*;
import spoon.support.compiler.VirtualFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/***
 * This class is used for manipulation of Java source code using Spoon
 */
public class Spoonery {

    public static void main(String[] args) throws IOException {
        // this is just used for testing purposes

        String code = "public class Test { private Test(int num1, int num2) { this.num = num1 + num2; } private Test(int num) { this.num = num; } int num; public static void main(String[] args) { System.out.println(\"Hi.\") } public class B { public static class C { } } }";

        code = Files.readString(Paths.get("src/main/java/Graph.java"));

        //System.out.println(getTrueClassName(code));
        //System.out.println(hasPrivateConstructor(code));
        //System.out.println(getPrivateConstructors(code));
        //System.out.println(setPrivateConstructorsPublic(code));
        System.out.println(hasIrregularConstructor(code));
        //System.out.println(hasNestedStaticClass(code));
    }

    public static String getTrueClassName(String code) throws SpoonException {
        Launcher launcher = new Launcher();
        launcher.addInputResource((SpoonResource)(new VirtualFile(code)));
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setAutoImports(true);
        CtModel model = launcher.buildModel();
        Collection<CtType<?>> allTypes = model.getAllTypes();

        for (CtType type : allTypes) {
            if (type.isClass() && type.hasModifier(ModifierKind.PUBLIC)) {
                return type.getSimpleName();
            }
        }

        throw new SpoonException("No public class found");
    }

    public static boolean hasPrivateConstructor(String code) {
        Launcher launcher = new Launcher();
        launcher.addInputResource((SpoonResource)(new VirtualFile(code)));
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setAutoImports(true);
        CtModel model = launcher.buildModel();
        Collection<CtType<?>> allTypes = model.getAllTypes();

        for (CtType type : allTypes) {
            if (type.isClass()) {
                CtClass<?> clazz = (CtClass) type;
                for (CtConstructor constructor : clazz.getConstructors()) {
                    if (constructor.hasModifier(ModifierKind.PRIVATE)) return true;
                }
            }
        }
        return false;
    }

    private static List<String> getPrivateConstructors(String code) {
        Launcher launcher = new Launcher();
        launcher.addInputResource((SpoonResource)(new VirtualFile(code)));
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setAutoImports(true);
        CtModel model = launcher.buildModel();
        Collection<CtType<?>> allTypes = model.getAllTypes();

        List<String> constructors = new ArrayList<>();

        for (CtType type : allTypes) {
            if (type.isClass()) {
                CtClass<?> clazz = (CtClass) type;

                // we're only interested in the public class
                if (!clazz.hasModifier(ModifierKind.PUBLIC)) continue;

                for (CtConstructor constructor : clazz.getConstructors()) {
                    if (constructor.hasModifier(ModifierKind.PRIVATE)) {
                        //System.out.println("Pretty:\n" + constructor.prettyprint());
                        //System.out.println("toString:\n" + constructor.toString());
                        //System.out.println("Difference:\n" + StringUtils.difference(constructor.prettyprint(), constructor.toString()));

                        constructors.add(constructor.prettyprint());
                    }
                }
            }
        }
        return constructors;
    }

    /***
     * This is done with regex, which might be a pretty ugly solution. Should fail very rarely though.
     * @param code
     * @return
     */
    public static String setPrivateConstructorsPublic(String code) {
        List<String> constructors = getPrivateConstructors(code);
        for (String temp : constructors) {
            temp = temp.replaceAll("(private.*\\))(?s).*", "$1");
            code = code.replace(temp, temp.replace("private", "public"));
        }
        return code;
    }

    public static boolean hasIrregularConstructor(String code) {
        Launcher launcher = new Launcher();
        launcher.addInputResource((SpoonResource)(new VirtualFile(code)));
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setAutoImports(true);
        CtModel model = launcher.buildModel();
        Collection<CtType<?>> allTypes = model.getAllTypes();

        for (CtType type : allTypes) {
            if (type.isClass() && type.hasModifier(ModifierKind.PUBLIC)) {
                CtClass<?> clazz = (CtClass) type;
                for (CtConstructor constructor : clazz.getConstructors()) {
                    if (constructor.isImplicit()) return false;
                    else if (constructor.getParameters().size() == 0) return false;
                }
                return true;
            }
        }
        throw new SpoonException("No public class found");
    }

    public static boolean hasNestedStaticClass(String code) {
        Launcher launcher = new Launcher();
        launcher.addInputResource((SpoonResource)(new VirtualFile(code)));
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setAutoImports(true);
        CtModel model = launcher.buildModel();
        Collection<CtType<?>> allTypes = model.getAllTypes();

        for (CtType<?> type : allTypes) {
            if (type.isClass()) {
                CtClass<?> clazz = (CtClass<?>) type;
                if (clazz.hasModifier(ModifierKind.STATIC)) return true;
                if (hasNestedStaticClassHelper(clazz)) return true;
            }
        }
        return false;
    }

    private static boolean hasNestedStaticClassHelper(CtClass<?> clazz) {
        for (CtType<?> nestedType : clazz.getNestedTypes()) {
            if (nestedType.isClass()) {
                CtClass<?> nestedClass = (CtClass<?>) nestedType;
                if (nestedClass.hasModifier(ModifierKind.STATIC)) return true;
                if (hasNestedStaticClassHelper(nestedClass)) return true;
            }
        }
        return false;
    }
}
