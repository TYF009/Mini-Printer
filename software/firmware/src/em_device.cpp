#include "em_device.h"

device_state_t g_device_state;

device_state_t * get_device_state(){
    return &g_device_state;
}

void set_device_paper_status(paper_state_e status){
    g_device_state.paper_state = status;
}

void set_read_ble_finish(bool finish){
    g_device_state.read_ble_finish = finish;
}

void init_device_state(){
    memset(&g_device_state,0,sizeof(device_state_t));
    g_device_state.printer_state = PRINTER_STATUS_INIT;
    g_device_state.battery = 100;
    g_device_state.temperature = 30;
    g_device_state.paper_state = PAPER_STATUS_NORMAL;
    g_device_state.read_ble_finish = false;
}