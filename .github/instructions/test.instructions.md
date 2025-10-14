---
applyTo: "**/src/test/**/*.java"
---

# Test Code Specific Instructions

Guidelines for writing tests across all modules.

## Test Structure

Follow the AAA pattern consistently:
- **Arrange**: Set up test data and context
- **Act**: Execute the code under test
- **Assert**: Verify the results

## Naming Conventions

```java
@Test
@DisplayName("Operation - expected behavior under specific condition")
void testOperationExpectedBehavior() {
    // Test implementation
}
```

Examples:
- `testSaveStation_shouldSucceed_whenValidData()`
- `testFindStation_shouldThrowException_whenNotFound()`
- `testListStations_shouldReturnOnlyTenantData_whenMultipleTenants()`

## Base Test Classes

Always extend appropriate base test class:
- `BaseServiceTest` - For service layer tests (no web context)
- `BaseControllerTest` - For controller tests (with MockMvc)
- `BaseTenantTest` - For multi-tenant isolation tests

## Tenant Context Management

```java
@Test
@DisplayName("Test with tenant context")
void testWithTenant() {
    // Arrange
    TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID);
    try {
        // Act & Assert
        // Test code here
    } finally {
        TenantContext.clear(); // CRITICAL: Always clear
    }
}
```

## Test Data Generation

Use `TestDataFactory` for unique test data:
```java
// Good - generates unique codes
String code = TestDataFactory.generateCode("PREFIX");

// Bad - hardcoded, causes conflicts
String code = "TEST001";
```

## Testing Multi-Tenant Isolation

Every entity operation test should verify tenant isolation:
```java
@Test
@DisplayName("List - should only return current tenant data")
void testListOnlyReturnsTenantData() {
    // Arrange - create data for two tenants
    TenantContext.setCurrentTenantId(TENANT_1_ID);
    Entity entity1 = createTestEntity("Tenant1");
    
    TenantContext.setCurrentTenantId(TENANT_2_ID);
    Entity entity2 = createTestEntity("Tenant2");
    
    // Act - query as tenant 1
    TenantContext.setCurrentTenantId(TENANT_1_ID);
    List<Entity> results = service.list();
    
    // Assert - only tenant 1's data returned
    assertEquals(1, results.size());
    assertEquals(entity1.getId(), results.get(0).getId());
    
    TenantContext.clear();
}
```

## Performance Tests

For performance-critical code, add basic benchmarks:
```java
@Test
@DisplayName("Bulk operation - should complete within acceptable time")
void testBulkOperationPerformance() {
    long start = System.currentTimeMillis();
    
    // Perform bulk operation
    service.bulkSave(generateTestEntities(1000));
    
    long duration = System.currentTimeMillis() - start;
    assertTrue(duration < 5000, "Bulk save should complete in < 5s");
}
```

## Common Test Assertions

- Always add assertion messages for clarity
- Test both success and failure cases
- Verify tenant_id is set correctly on created entities
- Test boundary conditions (null, empty, large datasets)

## What NOT to Test

- Don't test framework functionality (Spring, MyBatis)
- Don't test getters/setters
- Don't test obvious delegations
- Focus on business logic and tenant isolation
