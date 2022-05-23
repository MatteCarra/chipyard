package chipyard.fpga.vc707

import chipsalliance.rocketchip.config.Config
import chipyard.{BuildTop, CanHaveMasterTLMemPort, ChipTop, DefaultClockFrequencyKey, ExtTLMem}
import chisel3.experimental.Analog
import chisel3.{Flipped, fromIntToWidth}
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy.{BundleBridgeSource, LazyModule}
import freechips.rocketchip.jtag.JTAGIO
import freechips.rocketchip.tilelink.TLClientNode
import sifive.blocks.devices.jtag.JTAGPins
import sifive.blocks.devices.uart.{PeripheryUARTKey, UARTPortIO}
import sifive.fpgashells.clocks.{ClockGroup, ClockSinkNode, PLLFactoryKey, ResetWrangler}
import sifive.fpgashells.shell.xilinx.VC707BaseShell
import sifive.fpgashells.shell.{ClockInputDesignInput, ClockInputOverlayKey, DDRDesignInput, DDROverlayKey, DesignKey, JTAGDebugDesignInput, JTAGDebugOverlayKey, UARTDesignInput, UARTOverlayKey}

class WithVCU707ChipTop extends Config((site, here, up) => {
  case DesignKey => (p: Parameters) => new ChipTop()(p).suggestName("chiptop")
})

class VC707FPGATestHarness(override implicit val p: Parameters) extends VC707BaseShell with WithVCU707ChipTop {
  def dp = designParameters

  // DOC include start: ClockOverlay
  // place all clocks in the shell
  require(dp(ClockInputOverlayKey).size >= 1)
  val sysClkNode = dp(ClockInputOverlayKey)(0).place(ClockInputDesignInput()).overlayOutput.node

  /*** Connect/Generate clocks ***/

  // connect to the PLL that will generate multiple clocks
  val harnessSysPLL = dp(PLLFactoryKey)()
  harnessSysPLL := sysClkNode

  // create and connect to the dutClock
  println(s"VCU118 FPGA Base Clock Freq: ${dp(DefaultClockFrequencyKey)} MHz")
  val dutClock = ClockSinkNode(freqMHz = dp(DefaultClockFrequencyKey))
  val dutWrangler = LazyModule(new ResetWrangler)
  val dutGroup = ClockGroup()
  dutClock := dutWrangler.node := dutGroup := harnessSysPLL

  /*** UART ***/

  // DOC include start: UartOverlay
  // 1st UART goes to the VC707 dedicated UART

  /*** DDR ***/
  val io_uart_bb = BundleBridgeSource(() => (new UARTPortIO(dp(PeripheryUARTKey).head)))
  dp(UARTOverlayKey).head.place(UARTDesignInput(io_uart_bb))
  // DOC include end: UartOverlay

  val ddrNode = dp(DDROverlayKey).head.place(DDRDesignInput(dp(ExtTLMem).get.master.base, dutWrangler.node, harnessSysPLL)).overlayOutput.ddr
  // connect 1 mem. channel to the FPGA DDR
  val inParams = topDesign match { case td: ChipTop =>
    td.lazySystem match { case lsys: CanHaveMasterTLMemPort =>
      lsys.memTLNode.edges.in(0)
    }
  }
  val ddrClient = TLClientNode(Seq(inParams.master))
  ddrNode := ddrClient

  val jtagNode = dp(JTAGDebugOverlayKey).head.place(JTAGDebugDesignInput()).overlayOutput.jtag.getWrappedValue
  /*** DDR ***/

}