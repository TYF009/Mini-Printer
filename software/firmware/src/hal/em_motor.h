#ifndef _EM_MOTOR_H_
#define _EM_MOTOR_H_

#include "em_config.h"

void motor_run_step(uint32_t steps);
void motor_start();
void motor_stop();
void init_motor();
void motor_run();

#endif
