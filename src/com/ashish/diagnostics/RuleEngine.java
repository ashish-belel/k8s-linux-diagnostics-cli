package com.ashish.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleEngine {

    public record Finding(String severity, String component, String symptom, String rootCause, String remediation) {}

    private static final Pattern EXIT_137 = Pattern.compile("Exit Code:\\s*137");
    private static final Pattern OOM_REASON = Pattern.compile("Reason:\\s*OOMKilled");
    private static final Pattern IMAGE_PULL = Pattern.compile("(ErrImagePull|ImagePullBackOff|Failed to pull image)");
    private static final Pattern DISK_USAGE = Pattern.compile("(\\S+)\\s+\\S+\\s+\\S+\\s+\\S+\\s+(\\d+)%\\s+(\\S+)");
    private static final Pattern LOG_ERROR = Pattern.compile("(?i)(FATAL|ERROR|Exception|missing).*");
    private static final Pattern LISTENING_PORT = Pattern.compile("LISTEN\\s+\\d+\\s+\\d+\\s+\\S+:(\\d+)\\s+");

    public static List<Finding> analyzePorts(String ssOutput) {
        List<Finding> findings = new ArrayList<>();
        List<Integer> watchedPorts = List.of(80, 443, 3000, 8080, 9090, 9092);

        for (String line : ssOutput.split("\n")) {
            Matcher m = LISTENING_PORT.matcher(line);
            if (m.find()) {
                int port = Integer.parseInt(m.group(1));
                if (watchedPorts.contains(port)) {
                    findings.add(new Finding(
                        "WARNING",
                        "Host Port (" + port + ")",
                        "Port Already Bound (LISTEN)",
                        "Port " + port + " is actively in use on the host, which may cause bind conflicts for new services or agents.",
                        "Run `sudo ss -tulnp | grep :" + port + "` to identify the owning PID before deploying."
                    ));
                }
            }
        }
        return findings;
    }

    public static Finding analyzePod(String podName, String describeOutput, String previousLogs) {
        if (IMAGE_PULL.matcher(describeOutput).find()) {
            return new Finding(
                "CRITICAL",
                "Pod/" + podName,
                "ImagePullBackOff / ErrImagePull",
                "Container image tag does not exist or registry authentication failed.",
                "Verify the image name/tag in the Deployment spec and check imagePullSecrets."
            );
        }

        if (EXIT_137.matcher(describeOutput).find()) {
            boolean isOom = OOM_REASON.matcher(describeOutput).find();
            String cause = isOom
                ? "Container exceeded cgroup memory limit and was terminated by the kernel (SIGKILL / OOMKilled)."
                : "Container received SIGKILL (Exit 137) — likely a failing liveness probe or manual kill.";
            String fix = isOom
                ? "Increase resources.limits.memory in the Deployment spec or profile memory usage."
                : "Inspect livenessProbe thresholds and node events.";
            return new Finding("CRITICAL", "Pod/" + podName, "Exit Code 137 (" + (isOom ? "OOMKilled" : "SIGKILL") + ")", cause, fix);
        }

        Matcher logMatcher = LOG_ERROR.matcher(previousLogs);
        String errorSnippet = logMatcher.find() ? logMatcher.group(0).trim() : "Non-zero exit code on startup.";
        return new Finding(
            "HIGH",
            "Pod/" + podName,
            "CrashLoopBackOff / Startup Failure",
            "Application crashed during initialization: \"" + errorSnippet + "\"",
            "Check missing environment variables, ConfigMaps, or startup command arguments."
        );
    }

    public static List<Finding> analyzeEndpoints(String endpointsOutput) {
        List<Finding> findings = new ArrayList<>();
        for (String line : endpointsOutput.split("\n")) {
            if (line.startsWith("NAME") || line.isBlank()) continue;
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 2 && parts[1].equals("<none>")) {
                findings.add(new Finding(
                    "HIGH",
                    "Service/" + parts[0],
                    "Empty Endpoints (<none>)",
                    "Service selector labels do not match any Running/Ready Pod labels.",
                    "Run `kubectl get svc " + parts[0] + " -o yaml` and align spec.selector with Pod metadata.labels."
                ));
            }
        }
        return findings;
    }

    public static List<Finding> analyzeDisk(String dfOutput) {
        List<Finding> findings = new ArrayList<>();
        for (String line : dfOutput.split("\n")) {
            Matcher m = DISK_USAGE.matcher(line);
            if (m.find()) {
                int pct = Integer.parseInt(m.group(2));
                String mount = m.group(3);
                if (pct >= 85 && !mount.startsWith("/snap") && !mount.startsWith("/usr/src")) {
                    findings.add(new Finding(
                        "WARNING",
                        "Host Disk (" + mount + ")",
                        "High Disk Utilization (" + pct + "%)",
                        "Filesystem exceeded 85% threshold; kubelet may trigger image garbage collection or pod eviction.",
                        "Prune unused Docker images/volumes (`docker system prune`) or expand storage."
                    ));
                }
            }
        }
        return findings;
    }
}
