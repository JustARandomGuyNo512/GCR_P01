package com.sheridan.gcr.client.aiStuff;

import com.sheridan.gcr.GCR;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 开发环境下的“源目录优先”资源解析器。
 *
 * <p>在 ModDevGradle 的 run 配置下，游戏实际读取的是 {@code build/resources/main}（processResources
 * 产出的副本），而不是 {@code src/main/resources}。这会导致修改源资源后，即便重新读取文件也只能读到旧内容。
 * 本解析器在开发环境下优先从 {@code src/main/resources} / {@code src/generated/resources} 读取文件，
 * 找不到时才回退到原版的 {@link ResourceManager}，从而让“热重载”真的能读到磁盘上刚保存的内容。</p>
 *
 * <p><b>线程/侧安全性：</b>本类刻意不添加 {@code @OnlyIn(Dist.CLIENT)}，也不引用任何客户端专属类型，
 * 这样公共侧的 {@code AbstractDeferredLoader}（pivot/voxel 数据）也可以在开发环境复用同一套解析逻辑，
 * 而不会在专用服务器上因为类被裁剪而崩溃。需要客户端 ResourceManager 的地方由调用方传入。</p>
 */
public final class DevAssetResolver {

    /** 可用 {@code -Dgcr.hotreload.sourceRoot=<dir>[;<dir>]} 显式指定开发资源根目录。 */
    public static final String SOURCE_ROOT_PROPERTY = "gcr.hotreload.sourceRoot";

    /** 会从工作目录逐级向上查找的资源根目录候选项，按优先级排列。 */
    private static final String[] SOURCE_ROOT_CANDIDATES = {
            "src/main/resources",
            "src/generated/resources"
    };

    private static List<Path> sourceRoots;
    private static boolean resolved;

    private DevAssetResolver() {
    }

    /** 已启用的开发资源根目录；为空表示开发源覆盖不可用（例如正式环境）。 */
    public static synchronized List<Path> sourceRoots() {
        if (resolved) {
            return sourceRoots;
        }
        resolved = true;
        List<Path> roots = new ArrayList<>();

        String explicit = System.getProperty(SOURCE_ROOT_PROPERTY);
        if (explicit != null && !explicit.isBlank()) {
            for (String part : explicit.split(File.pathSeparator)) {
                if (!part.isBlank()) {
                    addDirectory(roots, Paths.get(part));
                }
            }
        } else if (GCR.IS_DEVELOPMENT) {
            Path dir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
            // 运行目录可能是项目根目录，也可能是 run/ 之类的工作目录，所以向上找几级
            for (int depth = 0; depth < 4 && dir != null; depth++, dir = dir.getParent()) {
                int before = roots.size();
                for (String candidate : SOURCE_ROOT_CANDIDATES) {
                    addDirectory(roots, dir.resolve(candidate));
                }
                if (roots.size() > before) {
                    break;
                }
            }
        }

        sourceRoots = List.copyOf(roots);
        if (!sourceRoots.isEmpty()) {
            GCR.LOGGER.info("[GCR HotReload] dev source roots: {}", sourceRoots);
        } else if (GCR.IS_DEVELOPMENT) {
            GCR.LOGGER.info("[GCR HotReload] no dev source root found, resources will be read from {}",
                    "the vanilla ResourceManager");
        }
        return sourceRoots;
    }

    private static void addDirectory(List<Path> roots, Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (Files.isDirectory(normalized) && !roots.contains(normalized)) {
            roots.add(normalized);
        }
    }

    /** 开发源覆盖是否可用。 */
    public static boolean enabled() {
        return !sourceRoots().isEmpty();
    }

    /** 把 {@code gcr:model_assets/gltf/xx.gltf} 映射为 {@code <root>/assets/gcr/model_assets/gltf/xx.gltf}。 */
    public static String toPackPath(ResourceLocation location) {
        return "assets/" + location.getNamespace() + "/" + location.getPath();
    }

    /** 返回该资源在开发源目录中的真实文件；不存在则返回 {@code null}。 */
    @Nullable
    public static Path resolveSourceFile(ResourceLocation location) {
        String packPath = toPackPath(location);
        for (Path root : sourceRoots()) {
            Path candidate = root.resolve(packPath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** 开发源目录中是否存在该资源（热重载时用来决定要不要走覆盖分支）。 */
    public static boolean hasSourceFile(ResourceLocation location) {
        return resolveSourceFile(location) != null;
    }

    /** 资源是否可以取到：开发源文件或给定的 {@link ResourceManager} 任一命中即为 true。 */
    public static boolean exists(ResourceLocation location, @Nullable ResourceManager fallback) {
        if (hasSourceFile(location)) {
            return true;
        }
        return fallback != null && fallback.getResource(location).isPresent();
    }

    /**
     * 打开一个资源流：开发源文件优先，否则回退到给定的 {@link ResourceManager}。
     *
     * @return 资源不存在时返回 {@code null}
     */
    @Nullable
    public static InputStream openOrNull(ResourceLocation location, @Nullable ResourceManager fallback) {
        Path source = resolveSourceFile(location);
        if (source != null) {
            try {
                return Files.newInputStream(source);
            } catch (IOException e) {
                GCR.LOGGER.warn("[GCR HotReload] failed to open dev source {}: {}", source, e.toString());
            }
        }
        if (fallback == null) {
            return null;
        }
        return fallback.getResource(location).map(resource -> {
            try {
                return resource.open();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).orElse(null);
    }
}
