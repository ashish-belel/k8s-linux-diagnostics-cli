# Environment Pre-Flight Readiness Checklist

Complete these checks before deploying observability agents or running diagnostics in a customer Linux/Kubernetes environment.

## 1. Host & OS Readiness
- [ ] **Disk Headroom:** Verify root (`/`) and container storage (`/var/lib/docker` or `/var/lib/containerd`) are below **85% utilization** (`df -h`).
- [ ] **Memory Availability:** Confirm sufficient free RAM and check swap status (`free -m`).
- [ ] **Java Runtime:** Confirm OpenJDK 21+ is installed on the jump box or target host (`java -version`).

## 2. Kubernetes & RBAC Access
- [ ] **Cluster Connectivity:** Verify `kubectl cluster-info` and `kubectl get nodes` return `Ready` status.
- [ ] **Namespace Scope:** Confirm the target namespace exists (`kubectl get ns`).
- [ ] **Read-Only Permissions:** Verify the active `kubeconfig` has read access (`get`, `list`, `describe`, `logs`) for Pods and Endpoints.

## 3. Network & Port Checks
- [ ] **CoreDNS Health:** Confirm `coredns` pods are `Running` in `kube-system`.
- [ ] **Required Ports:** Audit active listening sockets (`ss -tulnp`) to rule out host port conflicts before deployment.
