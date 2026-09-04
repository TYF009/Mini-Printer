#ifndef _EM_DEVICE_H_
#define _EM_DEVICE_H_

#include "em_config.h"

typedef enum{
    PRINTER_STATUS_INIT = 0,
    PRINTER_STATUS_START,
    PRINTER_STATUS_WORKING,
    PRINTER_STATUS_FINISH,
}printer_state_e;

typedef enum{
    PAPER_STATUS_NORMAL = 0,
    PAPER_STATUS_LACK,
}paper_state_e;

typedef struct
{
    uint8_t battery;
    uint8_t temperature;
    paper_state_e paper_state;
    printer_state_e printer_state; 
    bool read_ble_finish;
}device_state_t;

void init_device_state();

device_state_t * get_device_state();

void set_read_ble_finish(bool finish);

void set_device_paper_status(paper_state_e status);

#endif