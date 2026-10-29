#pragma once
#include <cmath>
#include <stdint.h>
#include <limits>
namespace foli {
// Retain the legacy 10k divider / 30k NTC / B3950 model; calibration is separate.
inline float ntc_celsius(float millivolts) {
    const float invalid=std::numeric_limits<float>::quiet_NaN();
    if(!std::isfinite(millivolts) || millivolts<=0 || millivolts>=3300)return invalid;
    const float voltage=millivolts/1000.0f;
    const float resistance=voltage*10000.0f/(3.3f-voltage);
    if(!std::isfinite(resistance) || resistance<=0)return invalid;
    const float inverse=std::log(resistance/30000.0f)/3950.0f+1.0f/298.15f;
    if(!std::isfinite(inverse) || inverse<=0)return invalid;
    const float celsius=1.0f/inverse-273.15f+0.5f;
    return std::isfinite(celsius)?celsius:invalid;
}
inline bool valid_temperature(float celsius) {
    return std::isfinite(celsius) && celsius>=0 && celsius<=125;
}
inline uint8_t temperature_byte(float celsius) {
    return valid_temperature(celsius)?uint8_t(std::ceil(celsius)):uint8_t(255);
}
inline bool temperature_allows_heating(float celsius) {
    return valid_temperature(celsius) && celsius<=65;
}
}
