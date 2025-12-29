# 固件管理 (OTA) 功能设计

> **功能版本**: v1.0
> **状态**: 规划中
> **维护者**: 平台研发组

## 1. 背景与目标
为了降低运维成本，系统需要支持远程对充电桩进行固件升级（Over-The-Air, OTA）。
支持 OCPP 协议中的 `UpdateFirmware` 指令和 `FirmwareStatusNotification` 状态上报。

## 2. 核心功能
1.  **固件版本管理**：上传、存储、版本号管理、适用型号标记。
2.  **升级任务管理**：创建升级任务（单桩/批量），监控升级进度。
3.  **协议交互**：下发升级指令，监听升级状态。

## 3. 数据模型

### 3.1 Firmware (固件包)
- `id`: 主键
- `version`: 版本号 (e.g., "v1.2.0")
- `model`: 适用充电桩型号
- `url`: 固件下载地址 (HTTP/HTTPS)
- `md5`: 文件校验和
- `description`: 版本描述
- `create_time`: 上传时间

### 3.2 FirmwareUpgradeTask (升级任务)
- `id`: 主键
- `firmware_id`: 关联固件ID
- `charger_id`: 目标充电桩ID
- `status`: 状态 (PENDING, DOWNLOADING, INSTALLING, INSTALLED, FAILED)
- `retry_count`: 重试次数
- `start_time`: 开始时间
- `finish_time`: 结束时间
- `error_message`: 失败原因

## 4. 接口设计

### 4.1 管理后台 (evcs-station)
- `POST /firmware`: 上传新固件
- `GET /firmware`: 查询固件列表
- `POST /firmware/upgrade`: 下发升级任务
- `GET /firmware/tasks`: 查询任务进度

### 4.2 协议服务 (evcs-protocol)
- `POST /protocol/command/firmware/update`: 下发 UpdateFirmware 指令
- 监听 `FirmwareStatusNotification` 消息，通过 MQ 更新任务状态。

## 5. 流程图
1.  运营人员上传固件到文件服务器（或对象存储），获取 URL。
2.  运营人员在管理后台创建升级任务。
3.  `evcs-station` 调用 `evcs-protocol` 下发 `UpdateFirmware(location=URL, retrieveDate=now)`。
4.  充电桩收到指令，开始下载，并上报 `FirmwareStatusNotification(status=Downloading)`。
5.  `evcs-protocol` 收到通知，更新任务状态。
6.  充电桩安装完成，上报 `Installed`，重启。
7.  充电桩重新连网，上报 `BootNotification`，携带新固件版本号。
8.  系统更新 `Charger` 表的 `firmware_version` 字段。
