# Stage 1: Build frontend
# NOTE: Kaniko running inside the cluster cannot reliably access public registries.
# Use base images mirrored into the in-cluster registry.
ARG BASE_REGISTRY=192.168.20.235:5000
FROM ${BASE_REGISTRY}/base/node:20-alpine AS build
ARG BUILD_NUMBER=0
ENV BUILD_NUMBER=${BUILD_NUMBER}
WORKDIR /app

# Copy package files for better caching
COPY package.json package-lock.json* ./

# Set npm mirror
RUN npm config set registry https://registry.npmmirror.com/

# Install dependencies
RUN npm ci || npm install

# Copy source files
COPY . .
# Build the application (includes writing version.json)
RUN npm run build

# Stage 2: Runtime with Nginx
FROM ${BASE_REGISTRY}/base/nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static assets and config
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/default.conf

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf.template

# Render template at startup (no envsubst/gettext required)
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Copy built assets from build stage (including version.json)
COPY --from=build /app/dist .

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
        CMD wget --no-verbose --tries=1 --spider http://127.0.0.1/ || exit 1

# Start Nginx
ENTRYPOINT ["/docker-entrypoint.sh"]
