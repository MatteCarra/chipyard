#include <stdint.h>
#include "./include/platform.h"
#include "./common.h"
#include <unistd.h>

#define DEBUG
#include "kprintf.h"

#define MAX_CORES 8

int main(void)
{
    REG32(uart, UART_REG_TXCTRL) = UART_TXEN;
    while(1) {
	    kputs("Hello from the other side");
	}
	return 0;
}