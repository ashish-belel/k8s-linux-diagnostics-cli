package com.ashish.diagnostics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportGenerator {

    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";
    private static final String RESET = "\u001B[0m";

    public static void generate(String namespace, List<RuleEngine.Finding> findings) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String readableTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        System.out.println(CYAN + "\n============================================================" + RESET);
        System.out.println(CYAN + "  K8s & Linux Diagnostics Report | Namespace: " + namespace + RESET);
        System.out.println(CYAN + "============================================================" + RESET);

        if (findings.isEmpty()) {
            System.out.println(GREEN + "[OK] No anomalies detected across pods, endpoints, or host disk." + RESET);
        } else {
            for (RuleEngine.Finding f : findings) {
                String color = f.severity().equals("CRITICAL") ? RED : YELLOW;
                System.out.printf("%s[%s]%s %s — %s%n", color, f.severity(), RESET, f.component(), f.symptom());
                System.out.printf("   Root Cause : %s%n", f.rootCause());
                System.out.printf("   Remediation: %s%n%n", f.remediation());
            }
        }

        // Write Markdown RCA file
        StringBuilder md = new StringBuilder();
        md.append("# Automated Environment Diagnostic & RCA Report\n\n");
        md.append("- **Timestamp:** ").append(readableTime).append("\n");
        md.append("- **Target Namespace:** `").append(namespace).append("`\n");
        md.append("- **Total Findings:** ").append(findings.size()).append("\n\n");
        md.append("## Findings Summary\n\n");
        md.append("| Severity | Component | Symptom | Root Cause | Recommended Remediation |\n");
        md.append("| :--- | :--- | :--- | :--- | :--- |\n");

        for (RuleEngine.Finding f : findings) {
            md.append(String.format("| **%s** | `%s` | %s | %s | %s |%n",
                f.severity(), f.component(), f.symptom(), f.rootCause(), f.remediation()));
        }

        String fileName = "diagnostic-report-" + timestamp + ".md";
        try {
            Files.writeString(Path.of(fileName), md.toString());
            System.out.println(GREEN + "Markdown RCA report written to: " + fileName + RESET);
        } catch (Exception e) {
            System.err.println(RED + "Failed to write report file: " + e.getMessage() + RESET);
        }
    }
}
