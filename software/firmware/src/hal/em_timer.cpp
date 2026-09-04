#include <Ticker.h>

#include "em_config.h"
#include "em_timer.h"

Ticker read_state_timer;
Ticker printer_timer;

bool read_state_timeout = true;
bool printer_timeout = false;

static void read_state_timer_callbackfun(){
  Serial.println("read_state now...");
  read_state_timeout = true;
}

void init_timer(){
  read_state_timer.attach_ms(10000,read_state_timer_callbackfun);
}

bool get_state_timeout(){
    return read_state_timeout;
}

void clean_state_timeout(){
    read_state_timeout = false;
}

static void printer_timeout_callbackfun(){
  Serial.println("触发打印超时错误...");
  printer_timeout = true;
}

bool get_printer_timeout_status(){
  return printer_timeout;
}

void open_printer_timeout_timer(){
  printer_timeout = false;
  printer_timer.once_ms(20000, printer_timeout_callbackfun);     
}

void close_printer_timeout_timer(){
  if(printer_timer.active())
    printer_timer.detach();
}