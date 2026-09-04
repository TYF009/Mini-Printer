#ifndef _EM_PRINTER_H_
#define _EM_PRINTER_H_

#include "em_config.h"
#include "em_motor.h"
#include "em_spi.h"
#include "em_ble.h"
#include "utils/em_queue.h"

//每行总点数
#define TPH_DOTS_PER_LINE 384
//每行字节长度 384/8
#define TPH_DI_LEN 48
//所有通道打印
#define ALL_STB_NUM 0xFF

/**
 * @brief 打印密度设置
 * 
 */
void set_heat_density(uint8_t density);
/**
 * @brief 数组打印
 *
 * @param data
 * @param length 数据长度必须是整行 48*n
 */
void start_printing(uint8_t *data, uint32_t len);

/**
 * @brief 可变队列打印
 * 
 */
void start_printing_by_queuebuf();

/**
 * @brief 单通道打印
 *
 * @param stbnum
 * @param data
 * @param len
 */
void start_printing_by_onestb(uint8_t stbnum, uint8_t *data, uint32_t len);

/**
 * @brief 
 * 
 */
void init_printer();

void testSTB();

#endif