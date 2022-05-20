package chipyard.fpga.vc707

import chipsalliance.rocketchip.config.Config
import chipyard.DefaultClockFrequencyKey
import chipyard.fpga.arty.ArtyFPGATestHarness
import chipyard.fpga.vc707.{WithDDRMem, WithSPIIOPassthrough, WithSPISDCard, WithTLIOPassthrough, WithUART, WithUARTIOPassthrough}
import chipyard.harness.OverrideHarnessBinder
import chisel3.Data
import freechips.rocketchip.devices.debug.HasPeripheryDebug
import freechips.rocketchip.devices.tilelink.BootROMLocated
import freechips.rocketchip.diplomacy.DTSTimebase
import freechips.rocketchip.subsystem.ExtMem
import sifive.blocks.devices.spi.{PeripherySPIKey, SPIParams}
import sifive.blocks.devices.uart.{PeripheryUARTKey, UARTParams}
import sifive.fpgashells.shell.xilinx.{VC7071GDDRSize}
import testchipip.SerialTLKey

class WithDefaultPeripherals extends Config((site, here, up) => {
  case PeripheryUARTKey => List(UARTParams(address = BigInt(0x64000000L)))
  case PeripherySPIKey => List(SPIParams(rAddress = BigInt(0x64001000L)))
})

class WithSystemModifications extends Config((site, here, up) => {
  case DTSTimebase => BigInt((1e6).toLong)
  case ExtMem => up(ExtMem, site).map(x => x.copy(master = x.master.copy(size = site(VC7071GDDRSize)))) // set extmem to DDR size
  case SerialTLKey => None // remove serialized tl port
})

// DOC include start: Abstractvc707 and Rocket
class Withvc707Tweaks extends Config(
  // harness binders
    new WithUART ++
    new WithDDRMem ++
    // io binders
    new WithUARTIOPassthrough ++
    new WithSPIIOPassthrough ++
    new WithTLIOPassthrough ++
    // other configuration
    new WithDefaultPeripherals ++
    new chipyard.config.WithTLBackingMemory ++ // use TL backing memory
    new WithSystemModifications ++ // setup busses, use sdboot bootrom, setup ext. mem. size
    new chipyard.config.WithNoDebug ++ // remove debug module
    new freechips.rocketchip.subsystem.WithoutTLMonitors ++
    new freechips.rocketchip.subsystem.WithNMemoryChannels(1) ++
    new WithFPGAFrequency(100) // default 100MHz freq
)

class Rocketvc707Config extends Config(
  new Withvc707Tweaks ++
    new chipyard.RocketConfig)

class WithFPGAFrequency(fMHz: Double) extends Config(
  new chipyard.config.WithPeripheryBusFrequency(fMHz) ++ // assumes using PBUS as default freq.
    new chipyard.config.WithMemoryBusFrequency(fMHz)
)

class WithFPGAFreq25MHz extends WithFPGAFrequency(25)
class WithFPGAFreq50MHz extends WithFPGAFrequency(50)
class WithFPGAFreq75MHz extends WithFPGAFrequency(75)
class WithFPGAFreq100MHz extends WithFPGAFrequency(100)

