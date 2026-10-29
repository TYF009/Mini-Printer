#include "em_queue.h"
#include "print_protocol.h"
static foli::RowBuffer<MAX_LINE> queue;
static SemaphoreHandle_t mutex = NULL;
void init_queue() { mutex=xSemaphoreCreateMutex();configASSERT(mutex); }
static bool lock() { return mutex && xSemaphoreTake(mutex,portMAX_DELAY)==pdPASS; }
bool write_to_printbuffer(uint8_t* data,size_t length) {
    if(!lock())return false;
    bool accepted=queue.push(data,length);xSemaphoreGive(mutex);return accepted;
}
bool read_to_printer(uint8_t* destination) {
    if(!lock())return false;
    bool read=queue.pop(destination);xSemaphoreGive(mutex);return read;
}
void clean_printbuffer() {
    if(!lock())return;
    queue.clear();xSemaphoreGive(mutex);
}
uint32_t get_ble_rx_leftline() {
    if(!lock())return 0;
    uint32_t n=queue.size();xSemaphoreGive(mutex);return n;
}
bool printbuffer_complete() {
    if(!lock())return false;
    bool complete=queue.complete();xSemaphoreGive(mutex);return complete;
}
