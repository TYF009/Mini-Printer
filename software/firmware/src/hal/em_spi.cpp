#include "em_spi.h"

static const int spiClk = 1000000; // 1 MHz

SPIClass hspi  = SPIClass(HSPI);
SPISettings printerSPISettings = SPISettings(spiClk, SPI_MSBFIRST, SPI_MODE0);

void spiCommand(uint8_t *data_buffer, uint8_t data_len)
{
    // use it as you would the regular arduino SPI API
    hspi.beginTransaction(printerSPISettings);
    hspi.transfer(data_buffer, data_len);
    hspi.endTransaction();
}

void init_spi()
{
    // hspi = SPIClass(HSPI);
    // alternatively route through GPIO pins
    hspi.begin(PIN_SCK, 16, PIN_SDA, -1); // SCLK, MISO, MOSI, SS
    hspi.setFrequency(spiClk);
}

