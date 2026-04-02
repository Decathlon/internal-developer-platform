---
title: Kubernetes Deployment
description: Deploy IDP-Core to production Kubernetes clusters
---

> [!CAUTION]
> This guide is still in DRAFT mode. Not all capabilities have been implemented yet. Please refer to the [Docker Deployment](docker.md) guide for production deployments until this guide is completed.

This guide covers deploying IDP-Core to Kubernetes, ensuring production-grade scalability, high availability, and resilience.

## Prerequisites

- Kubernetes 1.25+
- kubectl configured
- Helm 3.10+ (optional)
- PostgreSQL database (managed recommended)

---

## Kubernetes Manifests

### Namespace

```yaml title="namespace.yaml"
apiVersion: v1
kind: Namespace
metadata:
  name: idp
  labels:
    app.kubernetes.io/name: idp-core
```

### ConfigMap

```yaml title="configmap.yaml"
apiVersion: v1
kind: ConfigMap
metadata:
  name: idp-core-config
  namespace: idp
data:
  SPRING_PROFILES_ACTIVE: "prod"
  SERVER_PORT: "8080"
  LOG_LEVEL: "INFO"
  OTEL_SERVICE_NAME: "idp-core"
```

### Secret

```yaml title="secret.yaml"
apiVersion: v1
kind: Secret
metadata:
  name: idp-core-secrets
  namespace: idp
type: Opaque
stringData:
  DATABASE_URL: "jdbc:postgresql://postgres.database:5432/idp"
  DATABASE_USERNAME: "idp"
  DATABASE_PASSWORD: "your-secure-password"
```

> [!warning] "Secret Management"
> In production, use a secret management solution like:
>
> - Kubernetes External Secrets
> - HashiCorp Vault
> - AWS Secrets Manager
> - Azure Key Vault
> - GCP Secret Manager

### Deployment

We recommend deploying the Internal Developer Platform in multiple deployments based on functional areas (API, worker, scheduler) for better scalability and resource management. Below is an example of a single deployment for simplicity.

```yaml title="deployment.yaml"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: idp-core
  namespace: idp
  labels:
    app: idp-core
spec:
  replicas: 3
  selector:
    matchLabels:
      app: idp-core
  template:
    metadata:
      labels:
        app: idp-core
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: idp-core
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      containers:
        - name: idp-core
          image: decathlon/internal-developer-platform:latest
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
              name: http
          envFrom:
            - configMapRef:
                name: idp-core-config
          env:
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: idp-core-secrets
                  key: DATABASE_URL
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: idp-core-secrets
                  key: DATABASE_USERNAME
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: idp-core-secrets
                  key: DATABASE_PASSWORD
            - name: JAVA_OPTS
              value: "-Xmx1g -Xms512m -XX:+UseG1GC"
          resources:
            requests:
              memory: "1Gi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "2000m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3
          startupProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 30
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop:
                - ALL
          volumeMounts:
            - name: tmp
              mountPath: /tmp
      volumes:
        - name: tmp
          emptyDir: {}
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchExpressions:
                    - key: app
                      operator: In
                      values:
                        - idp-core
                topologyKey: kubernetes.io/hostname
```

### Service

```yaml title="service.yaml"
apiVersion: v1
kind: Service
metadata:
  name: idp-core
  namespace: idp
  labels:
    app: idp-core
spec:
  type: ClusterIP
  ports:
    - port: 80
      targetPort: 8080
      protocol: TCP
      name: http
  selector:
    app: idp-core
```

### Ingress

```yaml title="ingress.yaml"
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: idp-core
  namespace: idp
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
spec:
  tls:
    - hosts:
        - idp.example.com
      secretName: idp-tls
  rules:
    - host: idp.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: idp-core
                port:
                  number: 80
```

### Horizontal Pod Autoscaler

```yaml title="hpa.yaml"
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: idp-core
  namespace: idp
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: idp-core
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
```

### Pod Disruption Budget

```yaml title="pdb.yaml"
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: idp-core
  namespace: idp
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: idp-core
```

---

## Apply Manifests

```bash
# Apply all manifests
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f ingress.yaml
kubectl apply -f hpa.yaml
kubectl apply -f pdb.yaml

# Or apply entire directory
kubectl apply -f k8s/
```

---

## Database Options

### In-Cluster PostgreSQL (Dev/Test)

```yaml
# Add PostgreSQL as a dependency
dependencies:
  - name: postgresql
    version: "12.x.x"
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled
```

### Managed PostgreSQL (Production)

| Provider     | Service                                |
| ------------ | -------------------------------------- |
| Aiven        | Multicloud managed PostgreSQL instance |
| AWS          | RDS for PostgreSQL                     |
| GCP          | Cloud SQL                              |
| Azure        | Azure Database for PostgreSQL          |
| DigitalOcean | Managed Databases                      |

Configuration for managed database:

```yaml
database:
  url: jdbc:postgresql://my-db.abc123.us-east-1.rds.amazonaws.com:5432/idp
  existingSecret: aws-rds-credentials
```

---

## Operations

### Rolling Update

```bash
# Update image
kubectl set image deployment/idp-core idp-core=ghcr.io/decathlon/idp-core:1.1.0 -n idp

# Watch rollout
kubectl rollout status deployment/idp-core -n idp

# Rollback if needed
kubectl rollout undo deployment/idp-core -n idp
```

### Scale Manually

```bash
kubectl scale deployment/idp-core --replicas=5 -n idp
```

### View Logs

```bash
# All pods
kubectl logs -l app=idp-core -n idp --tail=100 -f

# Specific pod
kubectl logs idp-core-xxx-xxx -n idp -f
```

### Debug

```bash
# Get pod details
kubectl describe pod idp-core-xxx-xxx -n idp

# Execute shell
kubectl exec -it idp-core-xxx-xxx -n idp -- /bin/sh

# Port forward for local access
kubectl port-forward svc/idp-core 8080:80 -n idp
```

---

## Next Steps

- **[Configuration](configuration.md)** - Environment configuration
- **[Observability](observability.md)** - Monitoring and alerting
- **[Docker](docker.md)** - Local development setup
