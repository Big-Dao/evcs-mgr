# Copilot Instructions for EVCS Manager

## Project Overview

This is a multi-tenant electric vehicle charging station (EVCS) management platform built with Spring Boot 3.2.2 and Java 21. The system is designed around a sophisticated four-layer multi-tenant data isolation architecture, supporting multiple charging protocols (OCPP, CloudCharge) and payment gateways (Alipay, WeChat Pay).

**Project Status**: ✅ P2 & P3 phases completed  
**Primary Language**: Java 21  
**Build Tool**: Gradle 8.5  
**Database**: PostgreSQL 15 + Redis 7  
**Message Queue**: RabbitMQ  

For detailed information, see:
- [README.md](/README.md) - Project overview
- [docs/DEVELOPER-GUIDE.md](/docs/DEVELOPER-GUIDE.md) - Comprehensive development guide
- [docs/TECHNICAL-DESIGN.md](/docs/TECHNICAL-DESIGN.md) - Architecture design
- [docs/TESTING-QUICKSTART.md](/docs/TESTING-QUICKSTART.md) - Testing guide

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

## Build & Testing Commands

### Build the Project
```bash
# Full build with tests
./gradlew build

# Build without tests (for faster iteration)
./gradlew build -x test

# Build specific module
./gradlew :evcs-station:build
```

### Run Tests
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :evcs-station:test

# Run tests with coverage
./gradlew test jacocoTestReport
```

### Run Services Locally
```bash
# Start infrastructure (PostgreSQL, Redis, RabbitMQ)
docker-compose -f docker-compose.local.yml up -d

# Run gateway service
./gradlew :evcs-gateway:bootRun

# Run specific service
./gradlew :evcs-station:bootRun
```

### Access Services
- Swagger API Documentation: http://localhost:8080/doc.html
- Actuator Health Check: http://localhost:8080/actuator/health

## Code Quality Standards

### Naming Conventions
- **Classes**: PascalCase (e.g., `ChargingStationService`)
- **Methods**: camelCase (e.g., `findStationById`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_TENANT_ID`)
- **Packages**: lowercase (e.g., `com.evcs.station`)

### Code Organization
- **Controllers**: Handle HTTP requests, validate input, call services
- **Services**: Contain business logic, transaction boundaries
- **Mappers**: MyBatis Plus data access interfaces
- **Entities**: Database entities extending `BaseEntity`
- **DTOs**: Data transfer objects for API requests/responses

### Testing Best Practices
- Use `@SpringBootTest` for integration tests
- Extend `BaseServiceTest` or `BaseControllerTest` for test utilities
- Use `@DisplayName` annotations for readable test names
- Follow AAA pattern: Arrange, Act, Assert
- Use `TestDataFactory` to generate unique test data
- Always clear `TenantContext` in test cleanup

Example test structure:
```java
@SpringBootTest
@DisplayName("Charging Station Service Tests")
class ChargingStationServiceTest extends BaseServiceTest {
    
    @Test
    @DisplayName("Create station - should succeed with valid data")
    void testCreateStation() {
        // Arrange
        ChargingStation station = new ChargingStation();
        station.setCode(TestDataFactory.generateCode("STATION"));
        station.setName("Test Station");
        
        // Act
        boolean result = stationService.save(station);
        
        // Assert
        assertTrue(result, "Station should be created successfully");
        assertEquals(DEFAULT_TENANT_ID, station.getTenantId());
    }
}
```

### Documentation
- Add JavaDoc for public APIs and complex logic
- Keep comments concise and explain "why", not "what"
- Update relevant documentation in `/docs` when making architectural changes

## Common Pitfalls to Avoid

1. **Forgetting Tenant Context**: Always set `TenantContext` before business operations in services
2. **Hardcoding Test Data**: Use `TestDataFactory.generateCode()` for unique test data
3. **Missing @Transactional**: Add to service methods that modify data
4. **Ignoring @DataScope**: Apply appropriate data scope annotations to controller methods
5. **Not Clearing Context**: Always clear `TenantContext` in finally blocks or test cleanup
6. **Circular Dependencies**: Keep module dependencies unidirectional

## Git Workflow

### Commit Message Format
Follow Conventional Commits:
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

**Example**:
```
feat(station): add bulk import for charging stations

- Support Excel file import
- Implement data validation
- Add progress tracking

Closes #123
```

### Branch Naming
- Feature: `feature/description`
- Bug fix: `fix/description`
- Documentation: `docs/description`

## Important Notes

When implementing new features, follow the established multi-tenant patterns and always test tenant isolation thoroughly. Ensure all database queries respect tenant boundaries and all service methods properly handle tenant context.