---
applyTo: "evcs-common/**/*.java"
---

# Common Module Specific Instructions

This module contains shared utilities, multi-tenant framework, and common annotations. Changes here affect all other modules.

## Critical Guidelines

1. **Backward Compatibility**: Changes must maintain backward compatibility
   - Don't modify existing public APIs without migration plan
   - Deprecate before removing functionality

2. **Multi-Tenant Framework**: This is where tenant isolation is implemented
   - `TenantContext` - ThreadLocal storage, must be properly managed
   - `CustomTenantLineHandler` - SQL filtering, test thoroughly
   - `@DataScope` - Core annotation, changes require extensive testing

3. **Zero Business Logic**: Keep this module free of business logic
   - Only utilities, frameworks, and cross-cutting concerns
   - Business logic belongs in service modules

## Testing Requirements

- Test with multiple concurrent tenant contexts
- Test SQL filtering with complex queries
- Test AOP aspects with different annotation combinations
- Performance test any changes to tenant filtering logic

## Common Patterns in This Module

```java
// Tenant context management
public class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    
    public static void setCurrentTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }
    
    public static void clear() {
        TENANT_ID.remove(); // CRITICAL: Always call in finally
    }
}

// Custom exception handling
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TenantAccessException extends RuntimeException {
    public TenantAccessException(String message) {
        super(message);
    }
}
```

## Important Notes

- Changes to `IGNORE_TABLES` list must be reviewed carefully
- Tenant context must be cleared properly to prevent leaks
- All AOP aspects should have proper ordering
- Utility classes should be stateless
