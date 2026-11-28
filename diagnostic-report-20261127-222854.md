# Automated Environment Diagnostic & RCA Report

- **Timestamp:** 2026-11-27 22:28:54
- **Target Namespace:** `chaos-lab`
- **Total Findings:** 4

## Findings Summary

| Severity | Component | Symptom | Root Cause | Recommended Remediation |
| :--- | :--- | :--- | :--- | :--- |
| **CRITICAL** | `Pod/auth-service-67768fb574-pdt28` | Exit Code 137 (OOMKilled) | Container exceeded cgroup memory limit and was terminated by the kernel (SIGKILL / OOMKilled). | Increase resources.limits.memory in the Deployment spec or profile memory usage. |
| **HIGH** | `Pod/cbs-connector-7d8c8f5f86-h8jbw` | CrashLoopBackOff / Startup Failure | Application crashed during initialization: "FATAL ERROR: Missing required environment variable DB_CONNECTION_URL" | Check missing environment variables, ConfigMaps, or startup command arguments. |
| **CRITICAL** | `Pod/payment-gateway-74fc54cbb4-l8hz7` | ImagePullBackOff / ErrImagePull | Container image tag does not exist or registry authentication failed. | Verify the image name/tag in the Deployment spec and check imagePullSecrets. |
| **HIGH** | `Service/ledger-api-svc` | Empty Endpoints (<none>) | Service selector labels do not match any Running/Ready Pod labels. | Run `kubectl get svc ledger-api-svc -o yaml` and align spec.selector with Pod metadata.labels. |
