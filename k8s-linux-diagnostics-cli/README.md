# Kubernetes & Linux Environment Diagnostics CLI

A lightweight, zero-dependency **Core Java (JDK 21)** command-line utility built to automate first-line triage across containerized Kubernetes clusters (`Kind`) and Linux host environments.

Designed around **Customer Solution Engineering (CSE)** workflows, this tool eliminates repetitive manual inspection by executing targeted `kubectl`, `df`, and `ss` checks, parsing outputs via Regex, classifying root causes, and generating a stakeholder-ready Markdown **Root Cause Analysis (RCA)** report.

---

## Why This Exists

During production incidents or customer onboarding, engineers spend significant time running the same baseline commands (`kubectl describe`, `logs`, `get endpoints`, `df -h`, `ss -tulnp`) to isolate why a service is degraded. This utility standardizes that initial diagnostic sweep into a single fast command that runs on resource-constrained Linux hosts without requiring heavy frameworks or external agents.

---

## Diagnostic Coverage

| Failure State / Signal | Command Inspected | Detection Logic | Automated Remediation Guidance |
| :--- | :--- | :--- | :--- |
| **`ImagePullBackOff`** | `kubectl describe pod` | Matches image pull / tag not found events | Verify image tag, registry URL, and `imagePullSecrets`. |
| **`OOMKilled` (Exit 137)** | `kubectl describe pod` | Matches `Exit Code: 137` + verifies `Reason: OOMKilled` | Increase container memory limits/requests or investigate memory spikes. |
| **`CrashLoopBackOff`** | `kubectl logs --previous` | Parses container crash logs & non-zero exit codes | Check missing environment variables, config maps, or startup args. |
| **Broken Service Routing** | `kubectl get endpoints` | Detects `<none>` endpoints for active Services | Align Service `selector` labels with target Pod labels. |
| **Host Disk Pressure** | `df -h` | Flags mounted filesystems exceeding `85%` usage | Prune unused Docker images/volumes or expand host storage. |
| **Port / Socket Checks** | `ss -tulnp` | Audits active listening sockets on target ports | Identify port binding conflicts or stopped host services. |

---

## Prerequisites

* **OS:** Linux (Ubuntu / WSL2 Ubuntu)
* **Java:** JDK 21 (Eclipse Temurin or OpenJDK)
* **Tools:** `kubectl`, `docker`, and `kind` (for running the included fault sandbox)
* **Permissions:** Read-only `kubectl` access to the target namespace (`get`, `describe`, `logs`)

---

## Quickstart

### 1. Spin Up the Local Fault Sandbox (Optional)
Deploy the four deliberately broken Kubernetes manifests into an isolated `chaos-lab` namespace to test the CLI:

```bash
kind create cluster --name cse-lab
kubectl create namespace chaos-lab
kubectl apply -f chaos-manifests/ -n chaos-lab
```

### 2. Compile & Run the Diagnostics CLI
```bash
javac -d out src/com/cse/diagnostics/*.java
java -cp out com.cse.diagnostics.Main --namespace chaos-lab
```

### 3. Review the Output
The CLI prints a color-coded triage summary to the terminal and writes a timestamped report (`diagnostic-report-<timestamp>.md`) ready to attach to a support ticket or customer handoff.

---

## Customer Operations Documentation

To mirror real-world solution engineering engagements, this repository includes client-facing operational docs under `/docs`:

* **[`docs/PREFLIGHT_CHECKLIST.md`](docs/PREFLIGHT_CHECKLIST.md):** Environment readiness checks (disk headroom, DNS, ports, RBAC) prior to platform deployment.
* **[`docs/SAMPLE_RCA_REPORT.md`](docs/SAMPLE_RCA_REPORT.md):** Example post-incident RCA document formatted for both technical teams and non-technical stakeholders.

---

## Repository Structure

```text
k8s-linux-diagnostics-cli/
├── chaos-manifests/                # 4 broken K8s YAMLs (ImagePull, OOM, CrashLoop, DNS/Endpoints)
├── docs/
│   ├── PREFLIGHT_CHECKLIST.md      # Pre-deployment customer environment checklist
│   └── SAMPLE_RCA_REPORT.md        # Sample Markdown RCA generated from lab outages
├── src/
│   └── com/cse/diagnostics/
│       ├── Main.java               # CLI argument parsing & execution flow
│       ├── CommandRunner.java      # ProcessBuilder wrapper with execution timeouts
│       ├── ClusterScanner.java     # Runs kubectl, df, and ss diagnostics
│       ├── RuleEngine.java         # Regex-based symptom & root-cause classifier
│       └── ReportGenerator.java    # Terminal summary + Markdown RCA export
└── README.md
```

---

## License

This project is licensed under the MIT License.
