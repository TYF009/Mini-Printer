#ifndef _EM_SPI_H_
#define _EM_SPI_H_

#include "em_config.h"
#include <SPI.h>

void spiCommand(byte data);

void spiCommand(uint8_t *data_buffer, uint8_t data_len);

void init_spi();

#endif