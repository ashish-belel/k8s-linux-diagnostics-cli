package com.ashish.diagnostics;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String namespace = "chaos-lab";
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--namespace") || args[i].equals("-n")) {
                namespace = args[i + 1];
            }
        }

        System.out.println("Running Kubernetes & Linux diagnostics on namespace: " + namespace + "...");
        List<RuleEngine.Finding> findings = ClusterScanner.scan(namespace);
        ReportGenerator.generate(namespace, findings);
    }
}
