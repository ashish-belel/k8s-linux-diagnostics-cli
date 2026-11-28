package com.ashish.diagnostics;

import java.util.ArrayList;
import java.util.List;

public class ClusterScanner {

    public static List<RuleEngine.Finding> scan(String namespace) {
        List<RuleEngine.Finding> findings = new ArrayList<>();

        // 1. Scan Pods in namespace
        var podsRes = CommandRunner.run(List.of("kubectl", "get", "pods", "-n", namespace, "--no-headers"));
        if (podsRes.exitCode() != 0) {
            findings.add(new RuleEngine.Finding(
                "CRITICAL", "Kubernetes API", "kubectl command failed",
                podsRes.stderr(), "Verify kubeconfig context and cluster reachability."
            ));
            return findings;
        }

        for (String line : podsRes.stdout().split("\n")) {
            if (line.isBlank()) continue;
            String[] cols = line.trim().split("\\s+");
            if (cols.length < 3) continue;

            String podName = cols[0];
            String status = cols[2];

            if (!status.equals("Running") && !status.equals("Completed")) {
                var descRes = CommandRunner.run(List.of("kubectl", "describe", "pod", podName, "-n", namespace));
                var logsRes = CommandRunner.run(List.of("kubectl", "logs", podName, "-n", namespace, "--previous", "--tail=20"));
                String combinedLogs = logsRes.stdout().isBlank() ? logsRes.stderr() : logsRes.stdout();
                findings.add(RuleEngine.analyzePod(podName, descRes.stdout(), combinedLogs));
            }
        }

        // 2. Scan Service Endpoints
        var epRes = CommandRunner.run(List.of("kubectl", "get", "endpoints", "-n", namespace));
        if (epRes.exitCode() == 0) {
            findings.addAll(RuleEngine.analyzeEndpoints(epRes.stdout()));
        }

        // 3. Scan Host Disk Pressure
        var dfRes = CommandRunner.run(List.of("df", "-h"));
        if (dfRes.exitCode() == 0) {
            findings.addAll(RuleEngine.analyzeDisk(dfRes.stdout()));
        }

        // 4. Scan Host Listening Ports for Potential Conflicts
        var ssRes = CommandRunner.run(List.of("ss", "-tulnp"));
        if (ssRes.exitCode() == 0) {
            findings.addAll(RuleEngine.analyzePorts(ssRes.stdout()));
        }

        return findings;
    }
}
