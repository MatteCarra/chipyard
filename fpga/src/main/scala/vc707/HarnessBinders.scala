package chipyard.fpga.vc707

import chipyard.{CanHaveMasterTLMemPort, HasHarnessSignalReferences}
import chipyard.fpga.vc707.VC707FPGATestHarness
import chipyard.harness.OverrideHarnessBinder
import chisel3.Wire
import chisel3.experimental.BaseModule
import freechips.rocketchip.tilelink.TLBundle
import freechips.rocketchip.util.HeterogeneousBag
import sifive.blocks.devices.uart.{HasPeripheryUARTModuleImp, UARTPortIO}

class WithUART extends OverrideHarnessBinder({
  (system: HasPeripheryUARTModuleImp, th: BaseModule with HasHarnessSignalReferences, ports: Seq[UARTPortIO]) => {
    th match { case vc707th: VC707FPGATestHarness => {
      vc707th.io_uart_bb.bundle <> ports.head
    } }
  }
})

class WithDDRMem extends OverrideHarnessBinder({
  (system: CanHaveMasterTLMemPort, th: BaseModule with HasHarnessSignalReferences, ports: Seq[HeterogeneousBag[TLBundle]]) => {
    th match { case vcu118th: VC707FPGATestHarness => {
      require(ports.size == 1)

      val bundles = vcu118th.ddrClient.out.map(_._1)
      val ddrClientBundle = Wire(new HeterogeneousBag(bundles.map(_.cloneType)))
      bundles.zip(ddrClientBundle).foreach { case (bundle, io) => bundle <> io }
      ddrClientBundle <> ports.head
    } }
  }
})
