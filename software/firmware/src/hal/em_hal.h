#ifndef _EM_HAL_H_
#define _EM_HAL_H_

#include "em_config.h"
#include "em_device.h"


typedef enum{
    BEEP_CONNECT = 0,
    BEEP_WARN,
    BEEP_PRINTER_START,
}beep_type_e;

typedef enum{
    LED_CONNECT = 0,
    LED_WARN,
    LED_PRINTER_START,
}led_type_e;


void run_beep(beep_type_e type);

void run_led(led_type_e type);

void read_all_hal();

void init_hal();

bool read_paper_irq_need_report_status();


#endif