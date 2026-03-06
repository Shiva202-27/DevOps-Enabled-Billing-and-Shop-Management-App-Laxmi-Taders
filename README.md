# 🏪 Laxmi Traders Billing App – DevOps Enabled Project

## 📌 Project Overview

Laxmi Traders Billing App is an Android-based billing system developed for a wholesale grocery shop.
This project was extended with a **complete DevOps pipeline** to demonstrate modern CI/CD, containerization, orchestration, and monitoring practices.

---

# 🛠 Tech Stack

### Development

- Android (Java)
- Android Studio

### DevOps Tools

- Git
- GitHub
- GitHub Actions
- Docker
- Kubernetes (Minikube)
- Prometheus
- Grafana

---

# ⚙ DevOps Architecture

Developer pushes code → CI/CD pipeline builds APK → Docker container builds environment → Kubernetes deploys monitoring stack → Prometheus collects metrics → Grafana visualizes metrics.

```
Developer
   ↓
Git Push
   ↓
GitHub Repository
   ↓
GitHub Actions CI/CD
   ↓
Build APK Artifact
   ↓
Docker Container
   ↓
Kubernetes Cluster
   ↓
Prometheus Monitoring
   ↓
Grafana Dashboard
```

---

# 🚀 CI/CD Pipeline

Implemented automated pipeline using **GitHub Actions**.

### Workflow Steps

1. Code pushed to GitHub
2. CI pipeline triggers
3. Gradle builds Android APK
4. APK artifact stored in GitHub Actions
5. Docker image build environment created

---

# 🐳 Containerization

Docker is used to ensure a reproducible build environment.

Example Docker build:

```
docker build -t laxmi-traders-app .
```

---

# ☸ Kubernetes Deployment

Monitoring stack deployed using **Helm** on Kubernetes (Minikube).

Components deployed:

- Prometheus Server
- Alertmanager
- Node Exporter
- Pushgateway
- Grafana Dashboard

---

# 📊 Monitoring

### Prometheus

Collects metrics from Kubernetes cluster.

Access:

```
http://localhost:9090
```

### Grafana

Visualizes metrics using dashboards.

Access:

```
http://localhost:3000
```

Default login:

```
admin / admin
```

---

# 📈 Skills Demonstrated

- CI/CD Pipeline Implementation
- Containerization with Docker
- Kubernetes Orchestration
- Monitoring and Observability
- Infrastructure Automation

---

# 👨‍💻 Author

Shivshankar Kasapnor
DevOps & Software Engineering Enthusiast
