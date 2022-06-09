#include "./include/platform.h"
#include "./common.h"
#include "./include/serial.h"

int main(void) {
	REG32(uart, UART_REG_TXCTRL) = UART_TXEN;
    REG32(uart, UART_REG_RXCTRL) = UART_RXEN;

    kputs("BOOT INIT");
    while(1) {
        int num1, num2;
        char buf[4];

        kread(buf, 4);
        num1 = *((int*) buf);

        kread(buf, 4);
        num2 = *((int*) buf);

        int res = num1 + num2;
        kwrite(((char*) (&res)), 4);
    }
	return 0;
}