package chipyard.fpga.vc707

import chipyard.CanHaveMasterTLMemPort
import chipyard.iobinders.{OverrideIOBinder, OverrideLazyIOBinder}
import chisel3.experimental.{DataMirror, IO}
import freechips.rocketchip.devices.debug.HasPeripheryDebugModuleImp
import freechips.rocketchip.diplomacy.{InModuleBody, Resource, ResourceAddress, ResourceBinding}
import freechips.rocketchip.jtag.JTAGIO
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.tilelink.TLBundle
import freechips.rocketchip.util.HeterogeneousBag
import sifive.blocks.devices.spi.{HasPeripherySPI, HasPeripherySPIModuleImp, MMCDevice}
import sifive.blocks.devices.uart.HasPeripheryUARTModuleImp

class WithUARTIOPassthrough extends OverrideIOBinder({
  (system: HasPeripheryUARTModuleImp) => {
    val io_uart_pins_temp = system.uart.zipWithIndex.map { case (dio, i) => IO(dio.cloneType).suggestName(s"uart_$i") }
    (io_uart_pins_temp zip system.uart).foreach { case (io, sysio) =>
      io <> sysio
    }
    (io_uart_pins_temp, Nil)
  }
})

class WithJTAGPassthrough extends OverrideIOBinder({
  (system: HasPeripheryDebugModuleImp) => {
    val io_uart_pins_temp = Seq(IO(new JTAGIO()).suggestName("jtag"))
    (io_uart_pins_temp zip system.debug.toSeq).foreach { case (io, sysio) =>
      io <> sysio
    }
    (io_uart_pins_temp, Nil)
  }
})

class WithTLIOPassthrough extends OverrideIOBinder({
  (system: CanHaveMasterTLMemPort) => {
    val io_tl_mem_pins_temp = IO(DataMirror.internal.chiselTypeClone[HeterogeneousBag[TLBundle]](system.mem_tl)).suggestName("tl_slave")
    io_tl_mem_pins_temp <> system.mem_tl
    (Seq(io_tl_mem_pins_temp), Nil)
  }
})
