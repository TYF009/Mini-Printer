#ifndef _EM_BLE_H_
#define _EM_BLE_H_

#include <BLEDevice.h>
#include <BLE2902.h>

#include "em_config.h"
#include "em_device.h"
#include "em_hal.h"
#include "utils/em_queue.h"

/**
 * @brief 初始化BLE
 * 
 */
void init_ble();

/**
 * @brief 清空接收包统计
 * 
 */
void clean_blepack_count();

/**
 * 获取接收行数
*/
uint32_t get_blepack_count();

/**
 * @brief Get the ble connect object
 * 
 * @return true 
 * @return false 
 */
bool get_ble_connect();

/**
 * @brief 
 * 
 */
void ble_report();


#endif