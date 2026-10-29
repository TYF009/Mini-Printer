#pragma once
#include <stdint.h>
#include <stddef.h>
#include <string.h>
namespace foli {
enum class Command { Data, Density, Finish };
inline Command classify(const uint8_t* p, size_t n) {
    if (!p || n != 5) return Command::Data;
    if (p[0]==0xA5 && p[1]==0xA5 && p[2]==0xA5 && p[3]==0xA5) return Command::Density;
    if (p[0]==0xA6 && p[1]==0xA6 && p[2]==0xA6 && p[3]==0xA6) return Command::Finish;
    return Command::Data;
}
// Callers serialize access. Copy-out decouples a row from later producer writes.
template<size_t Capacity> class RowBuffer {
    uint8_t rows[Capacity][48] = {}, partial[48] = {};
    size_t head=0, tail=0, count=0, used=0;
    bool invalid=false;
public:
    void clear() { head=tail=count=used=0;invalid=false; }
    size_t size() const { return count; }
    bool complete() const { return !invalid && used==0 && count>0; }
    bool push(const uint8_t* p,size_t n) {
        if (!p || !n || invalid) return false;
        if (n > (Capacity-count)*48-used) { invalid=true;return false; }
        while(n) {
            size_t take=48-used;if(take>n)take=n;
            memcpy(partial+used,p,take);used+=take;p+=take;n-=take;
            if(used==48) { memcpy(rows[tail],partial,48);tail=(tail+1)%Capacity;++count;used=0; }
        }
        return true;
    }
    bool pop(uint8_t* destination) {
        if (!count || !destination) return false;
        memcpy(destination,rows[head],48);head=(head+1)%Capacity;--count;return true;
    }
};
}
