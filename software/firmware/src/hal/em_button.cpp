#include "em_button.h"
#include "../em_task.h"

//短按时间为 < 1s
#define SHORT_PRESS_TIME 1000

bool keyIsPress = false;
unsigned long clicktime = 0;
bool longPressIsStart = false;

void key_check_run(){
  if(keyIsPress == false){
    if(digitalRead(PIN_KEY) == LOW){
      vTaskDelay(10);
      if(digitalRead(PIN_KEY) == LOW){
        keyIsPress = true;
        clicktime = millis();
      }
    }
  }
  if(keyIsPress == true){
    if(digitalRead(PIN_KEY) == HIGH){
      if(millis() - clicktime > SHORT_PRESS_TIME){
        // Serial.printf("long click free\n");
        key_long_click_free_handle();
        longPressIsStart = false;
      }else{
        // Serial.printf("short click\n");
        key_click_handle();
      }
      keyIsPress = false;
    }else{
      if(millis() - clicktime > SHORT_PRESS_TIME){
        if(longPressIsStart == false){
        //   Serial.printf("long click\n");
          key_long_click_handle();
          longPressIsStart = true;
        }
      }
    }
  }
}

void init_key()
{
    pinMode(PIN_KEY,INPUT);
}