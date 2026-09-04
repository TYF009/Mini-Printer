#include "em_ble.h"
#include "em_printer.h"

BLECharacteristic *pCharacteristic = NULL;
bool bleConnected = false;
uint32_t packcount = 0;

void clean_blepack_count(){
    packcount = 0;
}

uint32_t get_blepack_count(){
    return packcount;
}

bool get_ble_connect(){
    return bleConnected;
}

void ble_report(){
    if (get_ble_connect()){
        device_state_t *pdevice = get_device_state();
        uint8_t status[4];
        status[0] = pdevice->battery;
        status[1] = pdevice->temperature;
        status[2] = pdevice->paper_state;
        status[3] = pdevice->printer_state;
        pCharacteristic->setValue((uint8_t*)&status,sizeof(status));
        pCharacteristic->notify();
    }
}

// Server回调函数声明
class bleServerCallbacks : public BLEServerCallbacks
{
    void onConnect(BLEServer *pServer)
    {
        bleConnected = true;
        packcount = 0;
        Serial.println("现在有设备接入~");
        run_beep(BEEP_CONNECT);
        run_led(LED_CONNECT);
    }

    void onDisconnect(BLEServer *pServer)
    {
        bleConnected = false;
        Serial.println("现在有设备断开连接~");
        // 在有设备接入后Advertising广播会被停止，所以要在设备断开连接时重新开启广播
        // 不然的话只有重启ESP32后才能重新搜索到
        pServer->startAdvertising(); // 该行效果同 BLEDevice::startAdvertising();
    }
};

class bleCharacteristicCallbacks : public BLECharacteristicCallbacks
{
    void onRead(BLECharacteristic *pCharacteristic)
    { // 客户端读取事件回调函数
        Serial.println("触发读取事件");
    }

    void onWrite(BLECharacteristic *pCharacteristic)
    { // 客户端写入事件回调函数
        size_t length = pCharacteristic->getLength();
        uint8_t *pdata = pCharacteristic->getData();
        if(length == 5){
            if(pdata[0] == 0xA5 && pdata[1] == 0xA5 && pdata[2] == 0xA5 && pdata[3] == 0xA5){
                if(pdata[4] == 1){
                    set_heat_density(30);
                }else if(pdata[4] == 2){
                    set_heat_density(60); 
                }else{
                    set_heat_density(100);
                }
                return;
            }
            if(pdata[0] == 0xA6 && pdata[1] == 0xA6 && pdata[2] == 0xA6 && pdata[3] == 0xA6){
                set_read_ble_finish(true);
                Serial.printf("接收打印数据完成,总行数=%d\n",packcount);
            }
        }
        packcount++;
        write_to_printbuffer(pdata,length);
        Serial.printf("触发写入事件 length=%d count=%d ", length, packcount);
        for (int index = 0; index < length; index++)
        {
            Serial.printf(" %x", pdata[index]);
        }
        Serial.printf("\n");
    }
};

void init_ble()
{
    BLEDevice::init(BLE_NAME); // 填写自身对外显示的蓝牙设备名称，并初始化蓝牙功能
    BLEDevice::startAdvertising();   // 开启Advertising广播

    BLEServer *pServer = BLEDevice::createServer();  // 创建服务器
    pServer->setCallbacks(new bleServerCallbacks()); // 绑定回调函数

    BLEService *pService = pServer->createService(SERVICE_UUID); // 创建服务
    pCharacteristic = pService->createCharacteristic(            // 创建特征
        CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ |
            BLECharacteristic::PROPERTY_NOTIFY |
            BLECharacteristic::PROPERTY_WRITE |
            BLECharacteristic::PROPERTY_WRITE_NR);
    // 如果客户端连上设备后没有任何写入的情况下第一次读取到的数据应该是这里设置的值
    pCharacteristic->setCallbacks(new bleCharacteristicCallbacks());
    pCharacteristic->addDescriptor(new BLE2902()); // 添加描述 
    pService->start(); // 启动服务
    BLEDevice::startAdvertising();
}

