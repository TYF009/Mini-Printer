#include "em_printer.h"
#include "em_timer.h"

float addTime[6] = {0};
// 点数-增加时间系数
#define kAddTime 0.001
// 根据打印头实际打印效果修改打印时间偏移值
//  #define STB1_ADDTIME 200
//  #define STB2_ADDTIME 200
//  #define STB3_ADDTIME -200
//  #define STB4_ADDTIME 0
//  #define STB5_ADDTIME 100
//  #define STB6_ADDTIME 500
#define STB1_ADDTIME 0
#define STB2_ADDTIME 0
#define STB3_ADDTIME 0
#define STB4_ADDTIME 0
#define STB5_ADDTIME 0
#define STB6_ADDTIME 0
// 热密度
uint8_t heat_density = 64;

/**
 * @brief 失能所有通道
 *
 */
static void set_stb_idle()
{
    digitalWrite(PIN_STB1, LOW);
    digitalWrite(PIN_STB2, LOW);
    digitalWrite(PIN_STB3, LOW);
    digitalWrite(PIN_STB4, LOW);
    digitalWrite(PIN_STB5, LOW);
    digitalWrite(PIN_STB6, LOW);
}

/**
 * @brief 打印前初始化
 *
 */
static void init_printing()
{
    // 开启打印超时监听
    open_printer_timeout_timer();
    set_stb_idle();
    digitalWrite(PIN_LAT, HIGH);
    // POWER ON
    digitalWrite(PIN_VHEN, HIGH);
}

/**
 * @brief 打印后停止
 *
 */
static void stop_printing()
{
    // 关闭打印超时监听
    close_printer_timeout_timer();
    // POWER OFF
    digitalWrite(PIN_VHEN, LOW);
    set_stb_idle();
    digitalWrite(PIN_LAT, HIGH);
}

/**
 * @brief Set the heat density object密度设置
 *
 * @param density
 */
void set_heat_density(uint8_t density)
{
    Serial.printf("打印密度设置：%d\n", density);
    heat_density = density;
}

void clearAddTime()
{
    addTime[0] = addTime[1] = addTime[2] = addTime[3] = addTime[4] = addTime[5] = 0;
}

/**
 * @brief 发送一行数据
 *
 * @param data
 */
static void send_one_line_data(uint8_t *data)
{
    float tmpAddTime = 0;
    clearAddTime();
    for (uint8_t i = 0; i < 6; ++i)
    {
        for (uint8_t j = 0; j < 8; ++j)
        {
            addTime[i] += data[i * 8 + j];
        }
        tmpAddTime = addTime[i] * addTime[i];
        addTime[i] = kAddTime * tmpAddTime;
    }
    // Serial.printf("data %d\n", data[0]);
    /* Send one dot line data. */
    spiCommand(data, TPH_DI_LEN);
    /* After send one dot line, send LAT signal low pulse.*/
    digitalWrite(PIN_LAT, LOW);
    us_delay(LAT_TIME);
    digitalWrite(PIN_LAT, HIGH);
    }

/**
 * @brief 通道打印运行
 *
 * @param now_stb_num
 */
static void run_stb(uint8_t now_stb_num)
{
    switch (now_stb_num)
    {
    case 0:
        digitalWrite(PIN_STB1, 1);
        us_delay((PRINT_TIME + addTime[0] + STB1_ADDTIME) * ((double)heat_density / 100));
        digitalWrite(PIN_STB1, 0);
        us_delay(PRINT_END_TIME);
        break;
    case 1:
        digitalWrite(PIN_STB2, 1);
        us_delay((PRINT_TIME + addTime[1] + STB2_ADDTIME) * ((double)heat_density / 100));
        digitalWrite(PIN_STB2, 0);
        us_delay(PRINT_END_TIME);
        break;
    case 2:
        digitalWrite(PIN_STB3, 1);
        us_delay((PRINT_TIME + addTime[2] + STB3_ADDTIME) * ((double)heat_density / 100));
        digitalWrite(PIN_STB3, 0);
        us_delay(PRINT_END_TIME);
        break;
    case 3:
        digitalWrite(PIN_STB4, 1);
        us_delay((PRINT_TIME + addTime[3] + STB4_ADDTIME) * ((double)heat_density / 100));
        digitalWrite(PIN_STB4, 0);
        us_delay(PRINT_END_TIME);
        break;
    case 4:
        digitalWrite(PIN_STB5, 1);
        us_delay((PRINT_TIME + addTime[4] + STB5_ADDTIME) * ((double)heat_density / 100));
        digitalWrite(PIN_STB5, 0);
        us_delay(PRINT_END_TIME);
        break;
    case 5:
        digitalWrite(PIN_STB6, 1);
        us_delay((PRINT_TIME + addTime[5] + STB6_ADDTIME) * ((double)heat_density / 100));
        digitalWrite(PIN_STB6, 0);
        us_delay(PRINT_END_TIME);
        break;
    default:
        break;
    }
}

/**
 * @brief 移动电机&开始打印
 *
 * @param need_stop
 * @param stbnum
 */
bool move_and_start_std(bool need_stop, uint8_t stbnum)
{
    if (need_stop == true)
    {
        Serial.print("打印停止\n");
        motor_stop();
        stop_printing();
        return true;
    }
    // 4step一行
    motor_run();
    if (stbnum == ALL_STB_NUM)
    {
        // 所有通道打印
        for (uint8_t index = 0; index < 6; index++)
        {
            run_stb(index);
            // 把电机运行信号插入通道加热中，减少打印卡顿和耗时
            if (index == 1 || index == 3 || index == 5)
            {
                motor_run();
            }
        }
        // motor_run_step(3);

    }
    else
    {
        // 单通道打印
        run_stb(stbnum);
        motor_run_step(3);
    }
    return false;
}

/**
 * @brief 打印错误检查
 * 
 * @param need_report 是否需BLE上报
 * @return true 打印出错
 * @return false 打印正常
 */
bool printing_error_check(bool need_report)
{
    if (get_printer_timeout_status())
    {
        Serial.print("打印超时\n");
        return true;
    }
    if (get_device_state()->paper_state == PAPER_STATUS_LACK)
    {
        if(need_report){
            // 停止打印
            clean_printbuffer();
            ble_report();
        }
        // 停止打印
        Serial.print("缺纸\n");
        run_beep(BEEP_WARN);
        run_led(LED_WARN);
        return true;
    }
    if (get_device_state()->temperature > 65)
    {
        if(need_report){
            // 停止打印
            clean_printbuffer();
            ble_report();
        }
        // 停止打印
        Serial.print("温度异常\n");
        run_beep(BEEP_WARN);
        run_led(LED_WARN);
        return true;
    }
    return false;
}

/**
 * @brief 数组打印
 *
 * @param data
 * @param length 数据长度必须是整行 48*n
 */
void start_printing(uint8_t *data, uint32_t len)
{
    uint32_t offset = 0;
    uint8_t *ptr = data;
    bool need_stop = false;
    init_printing();
    while (1)
    {
        if (len > offset)
        {
            // 发送一行数据 48byte*8=384bit
            send_one_line_data(ptr);
            offset += TPH_DI_LEN;
            ptr += TPH_DI_LEN;
        }
        else
            need_stop = true;
        if (move_and_start_std(need_stop, ALL_STB_NUM))
            break;
        if(printing_error_check(false))
            break;
    }
    motor_run_step(40);
    motor_stop();
    Serial.printf("打印完成\n");
}

/**
 * @brief 可变队列打印
 *
 */
void start_printing_by_queuebuf()
{
    uint8_t *pdata = NULL;
    bool need_stop = false;
    uint32_t printer_count = 0;
    init_printing();
    while (1)
    {
        if (get_ble_rx_leftline() > 0)
        {
            // Serial.printf("printing...\n");
            pdata = read_to_printer();
            if (pdata != NULL)
            {
                printer_count ++;
                send_one_line_data(pdata);
                if (move_and_start_std(false, ALL_STB_NUM))
                    break;
            }
        }
        else
        {
            if (move_and_start_std(true, ALL_STB_NUM))
                break;
        }
        if (get_printer_timeout_status())
            break;
        if(printing_error_check(true))
            break;
    }
    motor_run_step(230);
    motor_stop();
    Serial.printf("打印完成 接收总行数:%d 打印总行数:%d\n",get_blepack_count(),printer_count);
    clean_blepack_count();
}

/**
 * @brief 单通道数组打印
 *
 * @param stbnum
 * @param data
 * @param len
 */
void start_printing_by_onestb(uint8_t stbnum, uint8_t *data, uint32_t len)
{
    uint32_t offset = 0;
    uint8_t *ptr = data;
    bool need_stop = false;
    init_printing();
    while (1)
    {
        Serial.printf("printer %d\n", offset);
        if (len > offset)
        {
            // 发送一行数据 48byte*8=384bit
            send_one_line_data(ptr);
            offset += TPH_DI_LEN;
            ptr += TPH_DI_LEN;
        }
        else
            need_stop = true;
        if (move_and_start_std(need_stop, stbnum))
            break;
        if (get_printer_timeout_status())
            break;
        if(printing_error_check(false))
            break;
    }
    motor_run_step(40);
    motor_stop();
}

static void setDebugData(uint8_t *print_data)
{
    for (uint32_t cleardata = 0; cleardata < 48 * 5; ++cleardata)
    {
        print_data[cleardata] = 0x55;
    }
}

void testSTB()
{
    uint8_t print_data[48 * 10];
    uint32_t print_len;
    Serial.println("开始打印打印头选通引脚测试\n顺序: 1  2  3  4  5  6");
    print_len = 48 * 5;
    setDebugData(print_data);
    start_printing_by_onestb(0, print_data, print_len);
    setDebugData(print_data);
    start_printing_by_onestb(1, print_data, print_len);
    setDebugData(print_data);
    start_printing_by_onestb(2, print_data, print_len);
    setDebugData(print_data);
    start_printing_by_onestb(3, print_data, print_len);
    setDebugData(print_data);
    start_printing_by_onestb(4, print_data, print_len);
    setDebugData(print_data);
    start_printing_by_onestb(5, print_data, print_len);
    Serial.println("测试完成");
    motor_run_step(230);
}

void init_printer()
{
    init_motor();

    pinMode(PIN_LAT, OUTPUT);
    pinMode(PIN_SCK, OUTPUT);
    pinMode(PIN_SDA, OUTPUT);

    pinMode(PIN_STB1, OUTPUT);
    pinMode(PIN_STB2, OUTPUT);
    pinMode(PIN_STB3, OUTPUT);
    pinMode(PIN_STB4, OUTPUT);
    pinMode(PIN_STB5, OUTPUT);
    pinMode(PIN_STB6, OUTPUT);
    set_stb_idle();
    digitalWrite(PIN_LAT, HIGH);


    pinMode(PIN_VHEN, OUTPUT);
    digitalWrite(PIN_VHEN, LOW);

   init_spi();
}
