#include "utils/print_protocol.h"
#include "utils/temperature.h"
#include <cassert>
#include <cstdio>
#include <vector>

int main() {
    using namespace foli;
    const uint8_t finish[]={0xA6,0xA6,0xA6,0xA6,1};
    const uint8_t density[]={0xA5,0xA5,0xA5,0xA5,3};
    assert(classify(finish,5)==Command::Finish);
    assert(classify(density,5)==Command::Density);
    assert(classify(finish,4)==Command::Data);
    assert(classify(nullptr,5)==Command::Data);
    uint8_t raster[48];memset(raster,0xA6,sizeof raster);
    assert(classify(raster,sizeof raster)==Command::Data); // Prefix inside raster remains printable.
    // Golden rows survive arbitrary negotiated BLE write fragment boundaries.
    std::vector<uint8_t> input(48*7);
    for(size_t i=0;i<input.size();++i)input[i]=uint8_t(i*17+29);
    for(size_t fragment: {size_t(1),size_t(5),size_t(20),size_t(48),size_t(64),size_t(244)}) {
        RowBuffer<7> queue;assert(!queue.complete());
        for(size_t offset=0;offset<input.size();) {
            const size_t n=(input.size()-offset<fragment)?input.size()-offset:fragment;
            assert(queue.push(input.data()+offset,n));offset+=n;
        }
        assert(queue.complete());assert(queue.size()==7);
        uint8_t output[48];
        for(size_t row=0;row<7;++row) { assert(queue.pop(output));assert(memcmp(output,input.data()+row*48,48)==0); }
        assert(!queue.pop(output));
    }
    RowBuffer<2> queue;assert(queue.push(input.data(),47));assert(!queue.complete());
    assert(queue.push(input.data()+47,1));assert(queue.complete());
    uint8_t output[48];assert(queue.pop(output));assert(memcmp(output,input.data(),48)==0);
    // Sustained wraparound does not return overwritten storage to the consumer.
    for(size_t i=0;i<10000;++i) {
        uint8_t row[48];memset(row,uint8_t(i),48);assert(queue.push(row,48));assert(queue.pop(output));
        assert(memcmp(row,output,48)==0);
    }
    assert(queue.push(input.data(),96));assert(!queue.push(input.data(),1));assert(!queue.complete());
    assert(queue.size()==2); // Overflow is rejected atomically, not truncated or silently padded.
    assert(!queue.push(input.data(),1));queue.clear();assert(queue.size()==0);
    assert(!queue.complete());assert(!queue.push(nullptr,48));assert(!queue.push(input.data(),0));
    assert(queue.push(input.data(),48));assert(queue.pop(output));queue.clear();
    assert(memcmp(output,input.data(),48)==0);
    const float nan=std::numeric_limits<float>::quiet_NaN();
    const float inf=std::numeric_limits<float>::infinity();
    assert(std::fabs(ntc_celsius(2475)-25.5f)<0.01f);
    for(float mv: {0.0f,-1.0f,3300.0f,3400.0f,nan,inf}) {
        assert(!std::isfinite(ntc_celsius(mv)));
        assert(!temperature_allows_heating(ntc_celsius(mv)));
    }
    for(float invalid: {-1.0f,125.1f,256.0f,1000.0f,nan,inf,-inf}) {
        assert(!valid_temperature(invalid));
        assert(!temperature_allows_heating(invalid));
        assert(temperature_byte(invalid)==255); // No wrapping a hot/invalid sample into a cold byte.
    }
    assert(temperature_allows_heating(0));assert(temperature_allows_heating(65));
    assert(!temperature_allows_heating(65.01f));assert(temperature_byte(65.01f)==66);
    assert(temperature_byte(125)==125);assert(temperature_byte(0)==0);
    assert(!temperature_allows_heating(ntc_celsius(.01f)));
    assert(!temperature_allows_heating(ntc_celsius(3299)));
    std::puts("Firmware protocol/row-buffer/temperature regression tests passed");
}
