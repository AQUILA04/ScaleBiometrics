# Custom Keycloak with ScaleBiometrics theme and configuration

FROM quay.io/keycloak/keycloak:23.0 AS builder

# Enable health and metrics
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Configure database
ENV KC_DB=postgres

# Build optimized Keycloak
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:23.0

LABEL maintainer="ScaleBiometrics Team"
LABEL description="Custom Keycloak for ScaleBiometrics with automated realm configuration"

COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Copy custom theme
COPY themes/scalebiometrics /opt/keycloak/themes/scalebiometrics

# Copy realm configuration
COPY config/realm-export.json /opt/keycloak/data/import/realm.json

# Set environment variables
ENV KC_DB=postgres
ENV KC_HOSTNAME_STRICT=false
ENV KC_HOSTNAME_STRICT_HTTPS=false
ENV KC_HTTP_ENABLED=true
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Expose ports
EXPOSE 8080 8443

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=5 \
  CMD curl -f http://localhost:8080/health/ready || exit 1

# Start Keycloak with import
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--import-realm", "--optimized"]
