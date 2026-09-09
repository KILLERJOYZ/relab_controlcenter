import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.util.JavacTask;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.KtPsiFactory;

/** Parse-only audit: deliberately does not claim type, API, or Compose validation. */
public class SyntaxAudit {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        Process tracked = new ProcessBuilder("git", "ls-files", "--cached", "--others", "--exclude-standard", "*.kt", "*.kts", "*.java")
            .directory(root.toFile()).start();
        List<String> files = new String(tracked.getInputStream().readAllBytes()).lines().toList();
        if (tracked.waitFor() != 0) throw new IllegalStateException("git ls-files failed");
        Disposable disposable = Disposer.newDisposable();
        int kotlin = 0, java = 0, errors = 0;
        try {
            var env = KotlinCoreEnvironment.createForProduction(disposable,
                new CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES);
            var factory = new KtPsiFactory(env.getProject(), false);
            if (!PsiTreeUtil.collectElementsOfType(factory.createFile("Valid.kt", "fun audit() = 1\n"), PsiErrorElement.class).isEmpty()
                || PsiTreeUtil.collectElementsOfType(factory.createFile("Invalid.kt", "fun audit( =\n"), PsiErrorElement.class).isEmpty()
                || !PsiTreeUtil.collectElementsOfType(factory.createFile("Valid.gradle.kts", "plugins { id(\"example\") }\n"), PsiErrorElement.class).isEmpty()) {
                throw new IllegalStateException("Parser positive/negative sanity checks failed");
            }
            for (String relative : files) {
                Path file = root.resolve(relative);
                if (!Files.exists(file)) continue;
                String source = Files.readString(file).replace("\r\n", "\n").replace('\r', '\n');
                if (!relative.endsWith(".java")) {
                    kotlin++;
                    var psi = factory.createFile(file.getFileName().toString(), source);
                    for (var error : PsiTreeUtil.collectElementsOfType(psi, PsiErrorElement.class)) {
                        int line = 1 + (int) source.substring(0, error.getTextOffset()).chars().filter(c -> c == '\n').count();
                        if (errors < 50) System.out.println(relative + ":" + line + ": " + error.getErrorDescription());
                        errors++;
                    }
                } else {
                    java++;
                    var compiler = ToolProvider.getSystemJavaCompiler();
                    var diagnostics = new DiagnosticCollector<JavaFileObject>();
                    try (var manager = compiler.getStandardFileManager(diagnostics, null, null)) {
                        var task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                            List.of("-proc:none"), null, manager.getJavaFileObjects(file.toFile()));
                        task.parse();
                        for (var diagnostic : diagnostics.getDiagnostics()) {
                            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                                errors++;
                                System.out.println(relative + ":" + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(Locale.ROOT));
                            }
                        }
                    }
                }
            }
        } finally { Disposer.dispose(disposable); }
        System.out.println("Kotlin/KTS files=" + kotlin + ", Java files=" + java + ", syntax errors=" + errors);
        if (errors > 0) System.exit(1);
    }
}
