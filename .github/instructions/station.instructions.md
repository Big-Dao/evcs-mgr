---
applyTo: "evcs-station/**/*.java"
---

# Station Module Specific Instructions

This module handles charging station and charger management. It's the most mature module and serves as a reference implementation for multi-tenant patterns.

## Critical Requirements

1. **Multi-Tenant Isolation**: Every query MUST respect tenant boundaries
   - All service methods should use `@DataScope` annotations
   - Always set `TenantContext` in new entity creation

2. **Station Hierarchy**: Stations can have parent-child relationships
   - Validate parent station exists and belongs to same tenant
   - Use `TENANT_HIERARCHY` scope for queries that need to traverse hierarchy

3. **Real-Time Updates**: Station and charger status updates are frequent
   - Use dedicated mapper methods for status updates to avoid full entity updates
   - Consider caching for frequently accessed station data

4. **Offline Detection**: Track station heartbeat for connectivity monitoring
   - Update `last_heartbeat` on every communication
   - Use scheduled jobs to detect offline stations (>5 minutes without heartbeat)

## Testing Guidelines

- Always test with multiple tenants to verify isolation
- Test station hierarchy queries (parent-child relationships)
- Test offline detection scenarios
- Verify status update performance

## Common Patterns in This Module

```java
// Service layer - with proper tenant isolation
@Override
@DataScope(value = DataScopeType.TENANT_HIERARCHY)
public List<ChargingStation> findStationsByParentId(Long parentId) {
    LambdaQueryWrapper<ChargingStation> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ChargingStation::getParentId, parentId);
    return baseMapper.selectList(wrapper);
}

// Controller layer - with security
@GetMapping("/list")
@PreAuthorize("hasPermission('station:query')")
@DataScope(value = DataScopeType.TENANT_HIERARCHY)
public Result<List<ChargingStation>> list() {
    return Result.success(stationService.list());
}
```

## Database Schema Notes

- `charging_station` table has `tenant_id`, `parent_id` for hierarchy
- `charger` table has `station_id` foreign key
- Use JSONB fields: `facilities`, `supported_protocols`, `operating_hours`
- PostgreSQL arrays: `payment_methods`
