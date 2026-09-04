#ifndef _EM_TIMER_H_
#define _EM_TIMER_H_

/**
 * @brief 初始化Timer
 * 
 */
void init_timer();

/**
 * @brief Get the state timeout objectd
 * 
 * @return true 
 * @return false 
 */
bool get_state_timeout();

/**
 * @brief 
 * 
 */
void clean_state_timeout();

/**
 * @brief 开启打印超时计时器
 * 
 */
void open_printer_timeout_timer();

/**
 * @brief 关闭打印超时计时器
 * 
 */
void close_printer_timeout_timer();

/**
 * @brief Get the printer timeout status object
 * 
 * @return true 
 * @return false 
 */
bool get_printer_timeout_status();

#endif