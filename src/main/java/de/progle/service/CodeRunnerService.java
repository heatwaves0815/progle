package de.progle.service;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeRunnerService {

    private static final int RUN_TIMEOUT_SECONDS = 10;
    private static final int MAX_OUTPUT_LENGTH = 2000;

    private static final Pattern CLASS_PATTERN =
            Pattern.compile("\\bclass\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
    private static final Pattern PUBLIC_CLASS_PATTERN =
            Pattern.compile("\\bpublic\\s+class\\s+([A-Za-z_$][A-Za-z0-9_$]*)");

    public record RunResult(boolean compiled, String compileErrors, String programOutput,
                            String programErrors, boolean timedOut, String note) {

        static RunResult compileFailed(String errors) {
            return new RunResult(false, errors, "", "", false, "");
        }

        public boolean hasProgramOutput() {
            return programOutput != null && !programOutput.isBlank();
        }
    }

    public RunResult compileAndRun(String source) {
        return compileAndRun(source, null);
    }

    public RunResult compileAndRun(String source, String stdinInput) {
        String className = findClassName(source);
        if (className == null) {
            return RunResult.compileFailed(
                    "Keine Java-Klasse gefunden. Definiere eine Klasse, z. B. 'public class Steckbrief'.");
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new RunResult(true, "", "", "", false,
                    "Kein JDK-Compiler verfügbar - es fand nur die KI-Prüfung statt, keine lokale Kompilierung.");
        }
        try {
            Path directory = Files.createTempDirectory("progle-run");
            Path sourceFile = directory.resolve(className + ".java");
            Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StringWriter compilerOutput = new StringWriter();
            try (StandardJavaFileManager fileManager =
                         compiler.getStandardFileManager(diagnostics, Locale.GERMANY, StandardCharsets.UTF_8)) {
                boolean success = compiler.getTask(compilerOutput, fileManager, diagnostics,
                        List.of("-d", directory.toString()), null,
                        fileManager.getJavaFileObjects(sourceFile)).call();
                if (!success) {
                    return RunResult.compileFailed(
                            truncate(collectCompilerErrors(diagnostics, compilerOutput.toString())));
                }
            }

            RunResult result = runProgram(directory, className, stdinInput);
            deleteDirectory(directory);
            return result;
        } catch (Exception e) {
            return new RunResult(true, "", "", "", false,
                    "Interner Fehler beim lokalen Testen: " + e.getMessage());
        }
    }

    private RunResult runProgram(Path directory, String className, String stdinInput) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-cp");
        command.add(directory.toString());
        command.add("-Dfile.encoding=UTF-8");
        command.add("-Dstdout.encoding=UTF-8");
        command.add(className);
        Process process = new ProcessBuilder(command).redirectErrorStream(false).start();

        if (stdinInput != null && !stdinInput.isEmpty()) {
            try (var stdin = process.getOutputStream()) {
                stdin.write((stdinInput.endsWith("\n") ? stdinInput : stdinInput + "\n")
                        .getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            }
        } else {
            process.getOutputStream().close();
        }

        StringBuilder output = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        Thread outputThread = pump(process.getInputStream(), output);
        Thread errorThread = pump(process.getErrorStream(), errors);

        boolean finished = process.waitFor(RUN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        boolean timedOut = false;
        if (!finished) {
            process.destroyForcibly();
            timedOut = true;
            outputThread.join(500);
            errorThread.join(500);
        } else {
            outputThread.join(1000);
            errorThread.join(1000);
        }
        return new RunResult(true, "", truncate(output.toString()), truncate(errors.toString()), timedOut, "");
    }

    private static String javaExecutable() {
        String executable = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "java.exe"
                : "java";
        return Paths.get(System.getProperty("java.home"), "bin", executable).toString();
    }

    private Thread pump(InputStream in, StringBuilder target) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    target.append(line).append(System.lineSeparator());
                }
            } catch (Exception ignored) {
            }
        }, "progle-output-pump");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private String collectCompilerErrors(DiagnosticCollector<JavaFileObject> diagnostics, String fallback) {
        StringBuilder builder = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                builder.append("Zeile ").append(diagnostic.getLineNumber()).append(": ")
                        .append(diagnostic.getMessage(Locale.GERMANY)).append(System.lineSeparator());
            }
        }
        if (builder.isEmpty()) {
            builder.append(fallback);
        }
        return builder.toString().trim();
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.strip();
        if (trimmed.length() > MAX_OUTPUT_LENGTH) {
            return trimmed.substring(0, MAX_OUTPUT_LENGTH) + "\n... (Ausgabe gekürzt)";
        }
        return trimmed;
    }

    private void deleteDirectory(Path directory) {
        try (var paths = Files.list(directory)) {
            paths.forEach(path -> path.toFile().delete());
        } catch (Exception ignored) {
        }
    }

    private String findClassName(String source) {
        Matcher publicClass = PUBLIC_CLASS_PATTERN.matcher(source);
        if (publicClass.find()) {
            return publicClass.group(1);
        }
        Matcher anyClass = CLASS_PATTERN.matcher(source);
        if (anyClass.find()) {
            return anyClass.group(1);
        }
        return null;
    }
}
