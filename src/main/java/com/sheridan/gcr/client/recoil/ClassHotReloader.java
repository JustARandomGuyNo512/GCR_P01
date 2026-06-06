package com.sheridan.gcr.client.recoil;

//import net.minecraft.client.Minecraft;
//import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

//import javax.tools.*;
//import java.io.ByteArrayOutputStream;
//import java.io.OutputStream;
//import java.net.URI;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClassHotReloader {
//    private static final Path SOURCE_FILE_PATH = Paths.get("C:\\Users\\tjy13\\IdeaProjects\\GunsCraft_Reforged\\src\\main\\java\\com\\sheridan\\gcr\\client\\recoil\\RecoilUpdater.java");
//    private static final String FULL_CLASS_NAME = "com.sheridan.gcr.client.recoil.RecoilUpdater";
//
//    public static void reload() {
//        try {
//
//            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//            if (compiler == null) {
//                sendMessageToPlayer("错误: 找不到系统Java编译器。请确保你使用的是JDK而不是JRE来运行游戏。");
//                return;
//            }
//
//
//            InMemoryFileManager fileManager = new InMemoryFileManager(compiler.getStandardFileManager(null, null, null));
//
//            if (!Files.exists(SOURCE_FILE_PATH)) {
//                sendMessageToPlayer("错误: 找不到源文件: " + SOURCE_FILE_PATH.toAbsolutePath());
//                return;
//            }
//            String sourceCode = Files.readString(SOURCE_FILE_PATH);
//            JavaFileObject sourceFile = new InMemoryJavaFileObject(FULL_CLASS_NAME, sourceCode);
//
//
//            String classPath = System.getProperty("java.class.path");
//            Iterable<String> options = java.util.Arrays.asList("-classpath", classPath);
//
//
//            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
//            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, Collections.singletonList(sourceFile));
//
//            boolean success = task.call();
//
//            if (success) {
//
//                InMemoryClassLoader classLoader = new InMemoryClassLoader(fileManager.getClassBytes());
//                Class<?> reloadedClass = classLoader.loadClass(FULL_CLASS_NAME);
//
//                Object newInstance = reloadedClass.getDeclaredConstructor().newInstance();
//                IRecoilUpdater newHandlerInstance = (IRecoilUpdater) newInstance;
//                RecoilHandler.INSTANCE.setRecoilUpdater(newHandlerInstance);
//                //IRecoilCameraHandler handler = (IRecoilCameraHandler) newInstance;
//                //RecoilCameraHandler._debugReloadInstance(handler);
//
//                sendMessageToPlayer("成功热重载");
//            } else {
//
//                StringBuilder errorMsg = new StringBuilder("编译失败:\n");
//                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
//                    errorMsg.append(String.format("行 %d: %s\n", diagnostic.getLineNumber(), diagnostic.getMessage(null)));
//                }
//                sendMessageToPlayer(errorMsg.toString());
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            sendMessageToPlayer("热重载时发生严重错误: " + e.getMessage());
//        }
//    }
//
//    private static void sendMessageToPlayer(String message) {
//        Minecraft.getInstance().execute(() -> {
//            if (Minecraft.getInstance().player != null) {
//                Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
//            }
//        });
//    }
//
//
//    private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
//        private final String content;
//        protected InMemoryJavaFileObject(String className, String content) {
//            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
//            this.content = content;
//        }
//        @Override
//        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
//            return content;
//        }
//    }
//
//
//    private static class InMemoryJavaClassObject extends SimpleJavaFileObject {
//        private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        protected InMemoryJavaClassObject(String name, Kind kind) {
//            super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
//        }
//        public byte[] getBytes() {
//            return stream.toByteArray();
//        }
//        @Override
//        public OutputStream openOutputStream() {
//            return stream;
//        }
//    }
//
//
//    private static class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
//        private final Map<String, InMemoryJavaClassObject> classObjects = new HashMap<>();
//
//        protected InMemoryFileManager(StandardJavaFileManager fileManager) {
//            super(fileManager);
//        }
//
//        public Map<String, byte[]> getClassBytes() {
//            Map<String, byte[]> result = new HashMap<>();
//            classObjects.forEach((name, obj) -> result.put(name, obj.getBytes()));
//            return result;
//        }
//
//        @Override
//        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
//            InMemoryJavaClassObject classObject = new InMemoryJavaClassObject(className, kind);
//            classObjects.put(className, classObject);
//            return classObject;
//        }
//    }
//
//    private static class InMemoryClassLoader extends ClassLoader {
//        private final Map<String, byte[]> classBytes;
//
//        public InMemoryClassLoader(Map<String, byte[]> classBytes) {
//            super(InMemoryClassLoader.class.getClassLoader());
//            this.classBytes = classBytes;
//        }
//
//        @Override
//        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
//            if (classBytes.containsKey(name)) {
//                return findClass(name);
//            }
//            return super.loadClass(name, resolve);
//        }
//
//        @Override
//        protected Class<?> findClass(String name) throws ClassNotFoundException {
//            byte[] bytes = classBytes.get(name);
//            if (bytes == null) {
//                return super.findClass(name);
//            }
//            return defineClass(name, bytes, 0, bytes.length);
//        }
//    }
}

