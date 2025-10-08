# Copilot Instructions for EVCS Manager

This is a multi-tenant electric vehicle charging station management platform built with Spring Boot 3.2.2 and Java 21. The system is designed around a sophisticated four-layer multi-tenant data isolation architecture.

## Core Architecture

### Multi-Tenant Data Isolation System
The platform implements a **four-layer isolation approach** - this is the most critical pattern to understand:

1. **Database Layer**: PostgreSQL with `tenant_id` columns in all tenant-specific tables
2. **SQL Layer**: MyBatis Plus with `CustomTenantLineHandler` automatically adds `WHERE tenant_id = ?` to queries  
3. **Service Layer**: `@DataScope` annotations with AOP aspects for method-level permission control
4. **API Layer**: Spring Security with tenant context extraction from JWT tokens

**Key Components:**
- `TenantContext`: ThreadLocal storage for current tenant ID - ALWAYS check this is set before business operations
- `CustomTenantLineHandler`: Automatically filters SQL queries by tenant - tables in `IGNORE_TABLES` are exempt
- `@DataScope` annotation: Controls data access permissions with types like `TENANT`, `TENANT_HIERARCHY`, `ALL`
- `DataScopeAspect`: AOP aspect that intercepts methods to enforce data permissions

### Microservices Structure
```
evcs-mgr/
├── evcs-common/          # Shared utilities, tenant framework, annotations
├── evcs-station/         # Charging station & charger management (IMPLEMENTED)
├── evcs-tenant/          # Tenant hierarchy management  
├── evcs-auth/           # JWT authentication & authorization
├── evcs-order/          # Charging session orders (TODO)
├── evcs-payment/        # Payment gateway integrations (TODO) 
├── evcs-protocol/       # OCPP & CloudCharge protocol handlers (TODO)
├── evcs-gateway/        # API Gateway with routing
└── evcs-monitoring/     # System monitoring & alerts (TODO)
```

## Development Patterns

### Multi-Tenant Service Implementation
**ALWAYS** use this pattern when creating service methods:

```java
@Service
public class YourServiceImpl {
    
    @Override
    @DataScope  // Automatically applies tenant filtering
    public List<Entity> findAll() {
        // MyBatis Plus automatically adds: WHERE tenant_id = ?
        return baseMapper.selectList(null);
    }
    
    @Override
    @Transactional
    public boolean save(Entity entity) {
        // ALWAYS set tenant context for new entities
        entity.setTenantId(TenantContext.getCurrentTenantId());
        entity.setCreateBy(TenantContext.getCurrentUserId());
        return super.save(entity);
    }
}
```

### Controller Security Patterns
Controllers use declarative security with specific annotations:

```java
@RestController
@RequestMapping("/api/resource")
public class ResourceController {
    
    @GetMapping
    @PreAuthorize("hasPermission('resource:query')")
    @DataScope(value = DataScope.DataScopeType.TENANT_HIERARCHY)
    public Result<List<Resource>> list() {
        // Automatically enforces tenant hierarchy permissions
    }
}
```

### Database Entity Conventions
All tenant-specific entities extend `BaseEntity` and include:
- `tenant_id BIGINT NOT NULL` - auto-filtered by MyBatis Plus
- Timestamp fields (`create_time`, `update_time`) with triggers
- Logical deletion with `deleted` field
- JSONB fields for flexible data (e.g., `facilities`, `supported_protocols`)

## Build & Development Workflow

### Build Commands
```bash
# Full build (handles Gradle wrapper issues automatically)
./gradlew build

# Run specific service
./gradlew :evcs-station:bootRun

# Run with Docker services
docker-compose up -d postgres redis rabbitmq
./gradlew :evcs-gateway:bootRun
```

### Testing Multi-Tenant Isolation
Use `TenantIsolationTest` as reference - sets tenant context before operations:
```java
TenantContext.setCurrentTenantId(tenantId);
try {
    // Your business operations here
} finally {
    TenantContext.clear(); // ALWAYS clear context
}
```

## Protocol Extensions

The system supports OCPP and CloudCharge protocols through extension interfaces:

- `IOCPPProtocolService`: OCPP 1.6/2.0.1 WebSocket communication
- `ICloudChargeProtocolService`: HTTP REST API for Chinese market charging stations

Protocol support is stored in charger entity `supported_protocols` JSONB field.

## Critical Implementation Notes

### Data Scope Types
- `TENANT`: Current tenant only
- `TENANT_HIERARCHY`: Current tenant + sub-tenants  
- `ALL`: System admin access (requires special permissions)
- `USER`: User-level data isolation

### Tables Exempt from Tenant Filtering
`CustomTenantLineHandler.IGNORE_TABLES` includes system tables like `sys_tenant`, `sys_dict_type` - these contain global data.

### Charging Station Business Logic
- Stations have hierarchical relationships via `parent_id`
- Chargers belong to stations with `station_id` foreign key
- Real-time data updates use separate mapper methods for performance
- Status tracking includes offline detection based on `last_heartbeat`

### PostgreSQL Specific Features
- Uses arrays for payment methods: `payment_methods INTEGER[]`
- JSONB for flexible configuration: `facilities`, `supported_protocols`
- Spatial data with `latitude`/`longitude` for location searches
- Triggers for automatic timestamp updates

When implementing new features, follow the established multi-tenant patterns and always test tenant isolation thoroughly.