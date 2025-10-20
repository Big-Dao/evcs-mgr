# EVCS Manager Performance Tests

## Week 4 Day 5: API Performance Testing

This directory contains JMeter test scripts for performance testing of the EVCS Manager APIs.

## Test Scenarios

### 1. Order Creation Test
- **Endpoint**: POST /api/orders
- **Target TPS**: 500 transactions per second
- **Duration**: 5 minutes
- **Script**: jmeter/order-creation-test.jmx

### 2. Order Query Test
- **Endpoint**: GET /api/orders
- **Target TPS**: 1000 transactions per second
- **Duration**: 5 minutes
- **Script**: jmeter/order-query-test.jmx

### 3. Charger Status Update Test
- **Endpoint**: PUT /api/chargers/{id}/status
- **Target TPS**: 2000 transactions per second
- **Duration**: 5 minutes
- **Script**: jmeter/charger-status-update-test.jmx

## Performance Targets

| Metric | Target |
|--------|--------|
| Order Creation TPS | >= 500 |
| Order Query TPS | >= 1000 |
| Charger Status Update TPS | >= 2000 |
| P99 Response Time | < 200ms |
| Error Rate | < 0.1% |

## Running Tests

```bash
# Run in non-GUI mode
jmeter -n -t order-creation-test.jmx -l results/order-creation.jtl -e -o results/order-creation-report
```

For detailed instructions, see the full documentation in this directory.

