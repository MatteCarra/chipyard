package chipyard.fpga.vc707

import barstools.iocell.chisel.IOCell
import chipyard.CanHaveMasterTLMemPort
import chipyard.iobinders.{IOCellKey, OverrideIOBinder, OverrideLazyIOBinder}
import chisel3.experimental.{DataMirror, IO}
import freechips.rocketchip.diplomacy.{InModuleBody, Resource, ResourceAddress, ResourceBinding}
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.tilelink.TLBundle
import freechips.rocketchip.util.HeterogeneousBag
import sifive.blocks.devices.spi.{HasPeripherySPI, HasPeripherySPIModuleImp, MMCDevice}
import sifive.blocks.devices.uart.{HasPeripheryUARTModuleImp, UARTPortIO}

class WithUARTIOPassthrough extends OverrideIOBinder({
  (system: HasPeripheryUARTModuleImp) => {
    val (ports: Seq[UARTPortIO], cells2d) = system.uart.zipWithIndex.map({ case (u, i) =>
      val (port, ios) = IOCell.generateIOFromSignal(u, s"uart_${i}", system.p(IOCellKey), abstractResetAsAsync = true)
      (port, ios)
    }).unzip
    (ports, cells2d.flatten)
  }
})

class WithSPIIOPassthrough  extends OverrideLazyIOBinder({
  (system: HasPeripherySPI) => {
    // attach resource to 1st SPI
    ResourceBinding {
      Resource(new MMCDevice(system.tlSpiNodes.head.device, 1), "reg").bind(ResourceAddress(0))
    }

    InModuleBody {
      system.asInstanceOf[BaseSubsystem].module match { case system: HasPeripherySPIModuleImp => {
        val io_spi_pins_temp = system.spi.zipWithIndex.map { case (dio, i) => IO(dio.cloneType).suggestName(s"spi_$i") }
        (io_spi_pins_temp zip system.spi).map { case (io, sysio) =>
          io <> sysio
        }
        (io_spi_pins_temp, Nil)
      } }
    }
  }
})

class WithTLIOPassthrough extends OverrideIOBinder({
  (system: CanHaveMasterTLMemPort) => {
    val io_tl_mem_pins_temp = IO(DataMirror.internal.chiselTypeClone[HeterogeneousBag[TLBundle]](system.mem_tl)).suggestName("tl_slave")
    io_tl_mem_pins_temp <> system.mem_tl
    (Seq(io_tl_mem_pins_temp), Nil)
  }
})
