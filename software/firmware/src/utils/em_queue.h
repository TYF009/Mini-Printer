#ifndef _EM_QUEUE_H_
#define _EM_QUEUE_H_

#include "em_config.h"

//一行最大byte
#define MAX_ONELINE_BYTE 48
//最大行数
#define MAX_LINE 1000

typedef struct{
    uint8_t buffer[MAX_ONELINE_BYTE];
}ble_rx_buffer_t;

typedef struct{
    ble_rx_buffer_t printer_buffer[MAX_LINE];
    uint32_t r_index;
    uint32_t w_index;
    uint32_t left_line;
}ble_rx_t;

void write_to_printbuffer(uint8_t *pdata,size_t length);

uint32_t get_ble_rx_leftline();

void clean_printbuffer();

uint8_t * read_to_printer();

void init_queue();

#endif