---
title: API Reference
description: Complete REST API documentation with interactive Swagger UI
---

IDP-Core exposes a RESTful API for managing your software catalog. This section provides complete API documentation with interactive examples.

## Overview

The API follows REST conventions:

- **Base URL**: `/api/v1`
- **Format**: JSON
- **Authentication**: Can be done by any supported configuration of Spring Boot Security (like OAuth 2.0, Basic Auth)

## Interactive Documentation

Explore the API interactively using Swagger UI:

<style>
.swagger-loader {
  text-align: center;
  padding: 40px;
  color: #666;
}
.swagger-spinner {
  border: 3px solid #f3f3f3;
  border-top: 3px solid #3498db;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.swagger-error {
  background: #fee;
  border: 1px solid #fcc;
  border-radius: 4px;
  padding: 15px;
  margin: 20px 0;
  color: #c33;
}
</style>

<div id="swagger-ui">
  <div class="swagger-loader">
    <div class="swagger-spinner"></div>
    <div>Loading API documentation…</div>
  </div>
</div>

<script>
window.onload = function() {
  try {
    SwaggerUIBundle({
      url: "../static/swagger.yaml",
      dom_id: '#swagger-ui',
      presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
      layout: "BaseLayout"
    });

    setTimeout(() => {
      const loader = document.querySelector('.swagger-loader');
      if (loader) loader.remove();
    }, 1000);
  } catch (error) {
    document.getElementById('swagger-ui').innerHTML =
      '<div class="swagger-error">Failed to load API documentation. Please refresh the page.</div>';
  }
}
</script>

---

## Next Steps

- **[Getting Started](../getting-started/quickstart.md)** - Try your first API calls locally
- **[Data Integration](../features/data-integration.md)** - See how to ingest data
- **[Self-Service Actions](../features/self-service-actions.md)** - Trigger workflows via API
