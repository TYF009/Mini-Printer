#ifndef _EM_TASK_H_
#define _EM_TASK_H_

#include "em_config.h"
#include "em_device.h"
#include "hal/em_timer.h"
#include "hal/em_ble.h"
#include "hal/em_hal.h"
#include "utils/em_queue.h"

void init_task();
void printer_run();
void key_click_handle(void);
void key_long_click_handle(void);
void key_long_click_free_handle(void);

#endif