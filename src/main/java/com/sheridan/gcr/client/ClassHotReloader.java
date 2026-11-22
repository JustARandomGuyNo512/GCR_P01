package com.sheridan.gcr.client;

import com.sheridan.gcr.client.recoil.IRecoilCameraHandler;
import com.sheridan.gcr.client.recoil.IRecoilUpdater;
import com.sheridan.gcr.client.recoil.RecoilCameraHandler;
import com.sheridan.gcr.client.recoil.RecoilHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 开发期类热重载：用 JDK 编译器现场编译源文件，装进新的类加载器，再替换现有实例。
 *
 * <p>以前这里写死只重载 {@code RecoilCameraHandler}；现在“编译哪些文件、入口类是谁、装到哪”
 * 都由参数决定。一个目标 = 源码（单文件或整个包）+ 入口类名 + 接口类型 + 安装回调。
 * 默认的两个入口是 {@link #reloadRecoilUpdater()}（开发中的新后坐力 updater）和
 * {@link #reloadCameraHandler()}（旧的相机 handler）。</p>
 *
 * <p><b>updater 入口会编译整个 {@code recoil/test} 包</b>：随机扰动的参数在
 * {@code NoiseRecoilData} / {@code NoiseRecoilHandler} 里，只重载 updater 本身的话，
 * 改了同包参数类的默认值也不会生效。整包编译出来的类都进同一个子类加载器，
 * 所以新 updater 用到的就是新参数类，而不是父加载器里的旧版本。</p>
 *
 * <p>编译在客户端主线程上同步执行，会卡顿一两秒；只用于开发环境。</p>
 */
@OnlyIn(Dist.CLIENT)
public class ClassHotReloader {
    /** 换热重载目标时改这两个常量即可。 */
    private static final String RECOIL_UPDATER_CLASS = "com.sheridan.gcr.client.recoil.test.DemoRecoilUpdater";
    private static final String CAMERA_HANDLER_CLASS = "com.sheridan.gcr.client.recoil.RecoilCameraHandler";

    /**
     * 热重载当前开发中的后坐力 updater，并把新实例装回 {@link RecoilHandler}。
     *
     * <p>新实例会继承旧实例的 {@code RecoilData} 引用，避免换实例后
     * {@code getRecoilData()} 变成 null。开火活动、弹簧状态、上次开火时间这些瞬态会从零开始；
     * 相机层已经累积的后坐力要等热重载后的下一枪才会开始回落。</p>
     */
    public static void reloadRecoilUpdater() {
        Path sourceRoot = findSourceRoot();
        if (sourceRoot == null) {
            sendMessageToPlayer("错误: 找不到 src/main/java，无法热重载");
            return;
        }
        reloadPackage(sourceFile(sourceRoot, RECOIL_UPDATER_CLASS).getParent(), RECOIL_UPDATER_CLASS,
                IRecoilUpdater.class, reloaded -> {
                    IRecoilUpdater previous = RecoilHandler.INSTANCE.getRecoilUpdater();
                    if (previous != null && previous.getRecoilData() != null) {
                        reloaded.setRecoilData(previous.getRecoilData());
                    }
                    RecoilHandler.INSTANCE.setRecoilUpdater(reloaded);
                });
    }

    /** 热重载相机后坐力 handler；和旧版行为一致，只编译这一个类。 */
    public static void reloadCameraHandler() {
        Path sourceRoot = findSourceRoot();
        if (sourceRoot == null) {
            sendMessageToPlayer("错误: 找不到 src/main/java，无法热重载");
            return;
        }
        reloadFile(sourceFile(sourceRoot, CAMERA_HANDLER_CLASS), CAMERA_HANDLER_CLASS,
                IRecoilCameraHandler.class, RecoilCameraHandler::_debugReloadInstance);
    }

    /**
     * 编译单个源文件，成功后实例化 {@code entryClassName} 并交给 {@code installer}。
     */
    public static <T> void reloadFile(Path sourceFile, String entryClassName,
                                      Class<T> apiType, Consumer<T> installer) {
        if (!Files.isRegularFile(sourceFile)) {
            sendMessageToPlayer("错误: 找不到源文件: " + sourceFile.toAbsolutePath());
            return;
        }
        compileAndInstall(List.of(sourceFile), entryClassName, apiType, installer);
    }

    /**
     * 编译一个包目录下的所有 {@code .java}，成功后实例化 {@code entryClassName} 并安装。
     * 同包类会在同一个子类加载器里重新定义，所以参数类的修改也能一起生效。
     */
    public static <T> void reloadPackage(Path packageDir, String entryClassName,
                                         Class<T> apiType, Consumer<T> installer) {
        if (!Files.isDirectory(packageDir)) {
            sendMessageToPlayer("错误: 找不到源码目录: " + packageDir.toAbsolutePath());
            return;
        }

        List<Path> sources = new ArrayList<>();
        try (Stream<Path> files = Files.list(packageDir)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .forEach(sources::add);
        } catch (IOException e) {
            sendMessageToPlayer("错误: 读取源码目录失败: " + e.getMessage());
            return;
        }
        if (sources.isEmpty()) {
            sendMessageToPlayer("错误: 源码目录里没有 .java: " + packageDir.toAbsolutePath());
            return;
        }
        compileAndInstall(sources, entryClassName, apiType, installer);
    }

    private static Path sourceFile(Path sourceRoot, String className) {
        return sourceRoot.resolve(className.replace('.', '/') + ".java");
    }

    /**
     * 定位项目里的 {@code src/main/java}。
     *
     * <p>不能直接用相对路径：ModDevGradle 的 runClient 把工作目录设成项目下的 {@code run/}
     * （旧的绝对路径写法也因此在别的机器上失效）。这里从本类的编译输出目录和当前工作目录
     * 分别向上找，直到看见 {@code src/main/java}，Gradle 和 IDE 启动都能覆盖。</p>
     */
    private static Path findSourceRoot() {
        List<Path> starts = new ArrayList<>(2);
        try {
            starts.add(Paths.get(ClassHotReloader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()));
        } catch (Exception ignored) {
            // 某些类加载器不提供 code source，退回到工作目录。
        }
        starts.add(Paths.get(System.getProperty("user.dir", ".")));

        for (Path start : starts) {
            Path dir = start.toAbsolutePath();
            if (!Files.isDirectory(dir)) {
                dir = dir.getParent();
            }
            for (; dir != null; dir = dir.getParent()) {
                Path candidate = dir.resolve("src/main/java");
                if (Files.isDirectory(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static <T> void compileAndInstall(List<Path> sources, String entryClassName,
                                              Class<T> apiType, Consumer<T> installer) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            sendMessageToPlayer("错误: 找不到系统Java编译器。请确保你使用的是JDK而不是JRE来运行游戏。");
            return;
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager standardFileManager =
                compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
        try (InMemoryFileManager fileManager = new InMemoryFileManager(standardFileManager)) {
            Iterable<? extends JavaFileObject> units =
                    standardFileManager.getJavaFileObjectsFromPaths(sources);

            // -proc:none：这些类没有注解处理需求，跳过 classpath 上的 Mixin AP，编译更快也更稳。
            Iterable<String> options = Arrays.asList(
                    "-classpath", System.getProperty("java.class.path", ""),
                    "-proc:none");

            JavaCompiler.CompilationTask task =
                    compiler.getTask(null, fileManager, diagnostics, options, null, units);

            if (!task.call()) {
                StringBuilder errorMsg = new StringBuilder("编译失败:\n");
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    errorMsg.append(String.format("行 %d: %s\n",
                            diagnostic.getLineNumber(), diagnostic.getMessage(null)));
                }
                sendMessageToPlayer(errorMsg.toString());
                return;
            }

            Map<String, byte[]> classBytes = fileManager.getClassBytes();
            if (!classBytes.containsKey(entryClassName)) {
                sendMessageToPlayer("错误: 编译结果里没有 " + entryClassName);
                return;
            }

            InMemoryClassLoader classLoader = new InMemoryClassLoader(classBytes);
            Class<?> reloadedClass = Class.forName(entryClassName, true, classLoader);
            Object newInstance = reloadedClass.getDeclaredConstructor().newInstance();
            if (!apiType.isInstance(newInstance)) {
                sendMessageToPlayer("错误: " + entryClassName + " 没有实现 " + apiType.getSimpleName());
                return;
            }

            installer.accept(apiType.cast(newInstance));
            sendMessageToPlayer("成功热重载: " + entryClassName);
        } catch (IOException e) {
            sendMessageToPlayer("错误: 读取源文件失败: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendMessageToPlayer("热重载时发生严重错误: " + e);
        }
    }

    private static void sendMessageToPlayer(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        minecraft.execute(() -> {
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal(message));
            }
        });
    }


    private static class InMemoryJavaClassObject extends SimpleJavaFileObject {
        private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        protected InMemoryJavaClassObject(String name, Kind kind) {
            super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
        }
        public byte[] getBytes() {
            return stream.toByteArray();
        }
        @Override
        public OutputStream openOutputStream() {
            return stream;
        }
    }


    private static class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final Map<String, InMemoryJavaClassObject> classObjects = new HashMap<>();

        protected InMemoryFileManager(StandardJavaFileManager fileManager) {
            super(fileManager);
        }

        public Map<String, byte[]> getClassBytes() {
            Map<String, byte[]> result = new HashMap<>();
            classObjects.forEach((name, obj) -> result.put(name, obj.getBytes()));
            return result;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            InMemoryJavaClassObject classObject = new InMemoryJavaClassObject(className, kind);
            classObjects.put(className, classObject);
            return classObject;
        }
    }

    private static class InMemoryClassLoader extends ClassLoader {
        private final Map<String, byte[]> classBytes;

        public InMemoryClassLoader(Map<String, byte[]> classBytes) {
            super(InMemoryClassLoader.class.getClassLoader());
            this.classBytes = classBytes;
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (classBytes.containsKey(name)) {
                return findClass(name);
            }
            return super.loadClass(name, resolve);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classBytes.get(name);
            if (bytes == null) {
                return super.findClass(name);
            }
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
