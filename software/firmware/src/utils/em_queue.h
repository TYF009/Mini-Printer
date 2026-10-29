#pragma once
#include "em_config.h"
#define MAX_ONELINE_BYTE 48
#define MAX_LINE 1000
void init_queue();
bool write_to_printbuffer(uint8_t* data,size_t length);
bool read_to_printer(uint8_t* destination);
uint32_t get_ble_rx_leftline();
bool printbuffer_complete();
void clean_printbuffer();
