package com.ashish.diagnostics;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandRunner {

    public record CommandResult(int exitCode, String stdout, String stderr) {}

    public static CommandResult run(List<String> command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            String stdout;
            String stderr;
            try (BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                stdout = outReader.lines().collect(Collectors.joining("\n"));
                stderr = errReader.lines().collect(Collectors.joining("\n"));
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(-1, stdout, "Command timed out after 10s");
            }
            return new CommandResult(process.exitValue(), stdout, stderr);
        } catch (Exception e) {
            return new CommandResult(-1, "", e.getMessage());
        }
    }
}
