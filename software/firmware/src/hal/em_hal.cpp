#include "em_hal.h"
#include "em_adc.h"

int freq = 2000;    //设置频率2000kHz
int channel = 0;    //通道号，取值0 ~ 15
int resolution = 8; //分辨率，取值0~20，占空比duty最大取值为2^resolution-1

bool need_report = false;


#define EPISON 1e-7

void read_battery()
{
    get_device_state()->battery = map(get_adc_volts()*2,3300,4200,0,100);
    if(get_device_state()->battery > 100)
        get_device_state()->battery = 100;
    Serial.printf("battery = %d\n",get_device_state()->battery);
}

void read_temperature()
{
    float temperatrue = get_adc_temperatrue();
    Serial.printf("temperature = %f\n",temperatrue);
    if(temperatrue >= EPISON){
        get_device_state()->temperature = temperatrue;
    }else{
        get_device_state()->temperature = 0;
    }
}

void run_beep(beep_type_e type){
#ifdef BEEP_MODE
    switch (type)
    {
    case BEEP_CONNECT:
        ledcWriteNote(channel,NOTE_F,5);
        delay(100);
        ledcWrite(channel, 0);
        break;
    case BEEP_WARN:
        ledcWriteNote(channel,NOTE_D,5);
        delay(100);
        ledcWrite(channel, 0);
        delay(100);
        ledcWriteNote(channel,NOTE_D,5);
        delay(100);
        ledcWrite(channel, 0);
        delay(100);
        ledcWriteNote(channel,NOTE_D,5);
        delay(100);
        ledcWrite(channel, 0);
        break;
    case BEEP_PRINTER_START:
        ledcWriteNote(channel,NOTE_D,5);
        delay(100);
        ledcWrite(channel, 0);
        break;
    default:
        break;
    }
#endif
}

void run_led(led_type_e type){
    switch (type)
    {
    case LED_CONNECT:
        digitalWrite(PIN_LED, 0);
        delay(100);
        digitalWrite(PIN_LED, 1);
        break;
    case LED_WARN:
        digitalWrite(PIN_LED, 0);
        delay(100);
        digitalWrite(PIN_LED, 1);
        delay(100);
        digitalWrite(PIN_LED, 0);
        delay(100);
        digitalWrite(PIN_LED, 1);
        delay(100);
        digitalWrite(PIN_LED, 0);
        delay(100);
        digitalWrite(PIN_LED, 1);
        break;
    case LED_PRINTER_START:
        digitalWrite(PIN_LED, 0);
        delay(100);
        digitalWrite(PIN_LED, 1);
        break;
    default:
        break;
    }
}

void ARDUINO_ISR_ATTR paper_isr()
{
    need_report = true;
    set_device_paper_status(PAPER_STATUS_LACK);
    detachInterrupt(PIN_PAPER);
}

void read_paper_statue()
{
    if(digitalRead(PIN_PAPER) == PAPER_STATUS_NORMAL){
        get_device_state()->paper_state = PAPER_STATUS_NORMAL;
        attachInterrupt(PIN_PAPER, paper_isr, RISING);
    }else{
        get_device_state()->paper_state = PAPER_STATUS_LACK;
    }
    Serial.printf("paper_state = %d  %d\n",get_device_state()->paper_state,digitalRead(PIN_PAPER));
}

bool read_paper_irq_need_report_status()
{
    if (need_report)
    {
        need_report = false;
        return true;
    }
    else
        return false;
}

void read_all_hal()
{
    read_battery();
    read_temperature();
    read_paper_statue();
}

void init_hal()
{
    //缺纸IO初始化
    pinMode(PIN_PAPER, INPUT);
    attachInterrupt(PIN_PAPER, paper_isr, RISING);

#ifdef BEEP_MODE
    //蜂鸣器初始化  
    //设置通道频率和分辨率
    ledcSetup(channel, freq, resolution);
    //将通道0与引脚15连接
    ledcAttachPin(PIN_BEEP, channel);   
#endif

    pinMode(PIN_LED, OUTPUT);
    digitalWrite(PIN_LED, 1);
}