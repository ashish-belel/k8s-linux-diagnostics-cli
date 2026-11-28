# Post-Incident Root Cause Analysis (RCA)

- **Incident ID:** LAB-INC-001
- **Environment:** Local Kind Cluster (`cse-lab`) / Namespace: `chaos-lab`
- **Detection Method:** Automated triage via `k8s-linux-diagnostics-cli`
- **Severity:** Critical (Service Degradation)

---

## 1. Executive Summary
During a simulated payment-stack outage in `chaos-lab`, three backend workloads failed to start and one API service became unreachable. Automated diagnostics isolated all four root causes in under 3 seconds, enabling targeted remediation without manual pod-by-pod inspection.

## 2. Automated Findings & Root Causes

| Severity | Component | Symptom | Root Cause | Remediation Applied |
| :--- | :--- | :--- | :--- | :--- |
| **CRITICAL** | `Pod/auth-service` | Exit Code 137 (`OOMKilled`) | Container exceeded its `32Mi` cgroup memory limit and was terminated by the Linux kernel (`SIGKILL`). | Raised `resources.limits.memory` to `128Mi` in the Deployment spec. |
| **CRITICAL** | `Pod/payment-gateway` | `ImagePullBackOff` | Deployment referenced a non-existent container image tag (`nginx:99.99-nonexistent-tag`). | Updated image tag to a verified release (`nginx:alpine`). |
| **HIGH** | `Pod/cbs-connector` | `CrashLoopBackOff` | Startup failed with `FATAL ERROR: Missing required environment variable DB_CONNECTION_URL`. | Injected `DB_CONNECTION_URL` via Kubernetes Secret/ConfigMap. |
| **HIGH** | `Service/ledger-api-svc` | Empty Endpoints (`<none>`) | Service `spec.selector` (`ledger-api-typo`) did not match the running Pod label (`ledger-api`). | Corrected the Service selector label to `app: ledger-api`. |

## 3. Preventive Actions
1. **CI/CD Manifest Validation:** Enforce image tag verification and label-selector linting prior to deployment.
2. **Resource Right-Sizing:** Establish baseline memory profiling before setting strict container memory limits.
3. **Standardized Triage:** Integrate `k8s-linux-diagnostics-cli` into on-call runbooks to cut initial Mean Time to Identify (MTTI).
