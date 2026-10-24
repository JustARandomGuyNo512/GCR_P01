import com.sun.source.util.JavacTask;
import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/** Pure syntax parser: success does NOT imply Minecraft/Forge typechecking. */
public final class Java17SyntaxAudit {
    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args.length == 0 ? "src/main/java" : args[0]);
        List<File> files;
        try (Stream<Path> s = Files.walk(root)) {
            files = s.filter(p -> p.toString().endsWith(".java")).map(Path::toFile).sorted().toList();
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            var units = fm.getJavaFileObjectsFromFiles(files);
            JavacTask task = (JavacTask) compiler.getTask(null, fm, diagnostics,
                List.of("-proc:none", "--release", "17"), null, units);
            task.parse(); // syntax only; do not analyze symbols without Forge classpath
        }
        var errors = diagnostics.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR).toList();
        for (var d : errors) {
            System.err.printf("%s:%d:%d: %s%n", d.getSource() == null ? "unknown" : d.getSource().getName(), d.getLineNumber(), d.getColumnNumber(), d.getMessage(Locale.ROOT));
        }
        System.out.printf("Java 17 parse: %d source files, %d syntax errors%n", files.size(), errors.size());
        if (!errors.isEmpty()) System.exit(1);
    }
}
