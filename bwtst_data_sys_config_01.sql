SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

INSERT INTO `sys_config` (`id`, `config_key`, `config_value`, `remark`) VALUES (1, 'erp.upload.interval.minutes', '3', '要货单ERP上传定时任务间隔（分钟）');
INSERT INTO `sys_config` (`id`, `config_key`, `config_value`, `remark`) VALUES (2, 'erp.upload.enabled', 'false', '要货单ERP上传定时任务开关（true-启用/false-停用）');
