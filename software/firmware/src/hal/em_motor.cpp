#include "em_motor.h"
#include <Ticker.h>

/*
step  A  B  A- B-
      AN BN AP BP
1     1  0  0  1
2     0  0  0  1
3     0  0  1  1
4     0  0  1  0
5     0  1  1  0
6     0  1  0  0
7     1  1  0  0
8     1  0  0  0
4step 行进一点行
对应PIN
A- A B- B
0  1 1  0
0  0 1  0
1  0 1  0
1  0 0  0
1  0 0  1
0  0 0  1
0  1 0  1
0  1 0  0
*/
uint8_t motor_pos = 0;

uint8_t motor_table[8][4] =
    {
        {0, 1, 1, 0},
        {0, 0, 1, 0},
        {1, 0, 1, 0},
        {1, 0, 0, 0},
        {1, 0, 0, 1},
        {0, 0, 0, 1},
        {0, 1, 0, 1},
        {0, 1, 0, 0}};

Ticker timer_motor;

void timer_motor_callbackfun()
{
    digitalWrite(PIN_MOTOR_AP, motor_table[motor_pos][0]);
    digitalWrite(PIN_MOTOR_AM, motor_table[motor_pos][1]);
    digitalWrite(PIN_MOTOR_BP, motor_table[motor_pos][2]);
    digitalWrite(PIN_MOTOR_BM, motor_table[motor_pos][3]);
    motor_pos++;
    if (motor_pos >= 8)
    {
        motor_pos = 0;
    }
}

void motor_start()
{
    if (timer_motor.active() == false){
        digitalWrite(PIN_VHEN, HIGH);
        timer_motor.attach_ms(2, timer_motor_callbackfun);
    }
}

void motor_stop()
{
    digitalWrite(PIN_MOTOR_AP, 0);
    digitalWrite(PIN_MOTOR_AM, 0);
    digitalWrite(PIN_MOTOR_BP, 0);
    digitalWrite(PIN_MOTOR_BM, 0);
    digitalWrite(PIN_VHEN, LOW);
    if (timer_motor.active())
        timer_motor.detach();
}

void motor_run()
{
    digitalWrite(PIN_MOTOR_AP, motor_table[motor_pos][0]);
    digitalWrite(PIN_MOTOR_AM, motor_table[motor_pos][1]);
    digitalWrite(PIN_MOTOR_BP, motor_table[motor_pos][2]);
    digitalWrite(PIN_MOTOR_BM, motor_table[motor_pos][3]);
    motor_pos++;
    if (motor_pos >= 8)
    {
        motor_pos = 0;
    }
}

void motor_run_step(uint32_t steps)
{
    while (steps)
    {
        digitalWrite(PIN_MOTOR_AP, motor_table[motor_pos][0]);
        digitalWrite(PIN_MOTOR_AM, motor_table[motor_pos][1]);
        digitalWrite(PIN_MOTOR_BP, motor_table[motor_pos][2]);
        digitalWrite(PIN_MOTOR_BM, motor_table[motor_pos][3]);
        motor_pos++;
        if (motor_pos >= 8)
        {
            motor_pos = 0;
        }
        us_delay(MOTOR_WATI_TIME);
        steps--;
    }
}

void init_motor()
{
    pinMode(PIN_MOTOR_AP, OUTPUT);
    pinMode(PIN_MOTOR_AM, OUTPUT);
    pinMode(PIN_MOTOR_BP, OUTPUT);
    pinMode(PIN_MOTOR_BM, OUTPUT);

    digitalWrite(PIN_MOTOR_AP, 0);
    digitalWrite(PIN_MOTOR_AM, 0);
    digitalWrite(PIN_MOTOR_BP, 0);
    digitalWrite(PIN_MOTOR_BM, 0);
}
