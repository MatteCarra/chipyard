package chipyard.fpga.vc707

import chipyard.harness.ApplyHarnessBinders
import chipyard.iobinders.HasIOBinders
import chipyard.{BuildTop, HasHarnessSignalReferences}
import chisel3._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy.LazyModule
import sifive.fpgashells.shell.xilinx.VC707Shell
import sifive.fpgashells.shell.xilinx.artyshell.ArtyShell

class VC707FPGATestHarness(override implicit val p: Parameters) extends VC707Shell with HasHarnessSignalReferences {

  val lazyDut = LazyModule(p(BuildTop)(p)).suggestName("chiptop")

  // Convert harness resets from Bool to Reset type.
  val hReset = Wire(Reset())
  hReset := reset

  val dReset = Wire(AsyncReset())
  dReset := reset_core.asAsyncReset

  // default to 32MHz clock
  withClockAndReset(clock_32MHz, hReset) {
    val dut = Module(lazyDut.module)
  }

  val buildtopClock = clock_32MHz
  val buildtopReset = hReset
  val success = false.B

  val dutReset = dReset

  // must be after HasHarnessSignalReferences assignments
  lazyDut match { case d: HasIOBinders =>
    ApplyHarnessBinders(this, d.lazySystem, d.portMap)
  }
}

