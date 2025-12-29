# 智能充电 (Smart Charging) 功能规划

> **状态**: 待办
> **优先级**: P1

## 目标
实现基于 OCPP 的智能充电特性，优化电力分配，降低运营成本。

## 核心特性
1.  **充电配置 (Charging Profiles)**: 下发 `SetChargingProfile`，限制最大功率。
2.  **负载均衡 (Load Balancing)**: 站点内多桩功率动态分配。
3.  **有序充电**: 根据电网负荷或分时电价，自动调整充电计划。
