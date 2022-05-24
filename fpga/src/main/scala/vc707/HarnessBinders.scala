package chipyard.fpga.vc707

import chipyard.{CanHaveMasterTLMemPort}
import chipyard.harness.OverrideHarnessBinder
import chisel3.Wire
import freechips.rocketchip.tilelink.TLBundle
import freechips.rocketchip.util.HeterogeneousBag
import sifive.blocks.devices.spi.{HasPeripherySPI, SPIPortIO}
import sifive.blocks.devices.uart.{HasPeripheryUARTModuleImp, UARTPortIO}

class WithUART extends OverrideHarnessBinder({
  (system: HasPeripheryUARTModuleImp, th: VC707FPGATestHarness, ports: Seq[UARTPortIO]) => {
    th.io_uart_bb.bundle <> ports.head
  }
})

class WithSPISDCard extends OverrideHarnessBinder({
  (system: HasPeripherySPI, th: VC707FPGATestHarness, ports: Seq[SPIPortIO]) => {
    th.io_spi_bb.bundle <> ports.head
  }
})

class WithDDRMem extends OverrideHarnessBinder({
  (system: CanHaveMasterTLMemPort, th: VC707FPGATestHarness, ports: Seq[HeterogeneousBag[TLBundle]]) => {
    require(ports.size == 1)

    val bundles = th.ddrClient.out.map(_._1)
    val ddrClientBundle = Wire(new HeterogeneousBag(bundles.map(_.cloneType)))
    bundles.zip(ddrClientBundle).foreach { case (bundle, io) => bundle <> io }
    ddrClientBundle <> ports.head
  }
})
