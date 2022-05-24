package chipyard.fpga.vc707

import chipsalliance.rocketchip.config.Config
import freechips.rocketchip.diplomacy.DTSTimebase
import freechips.rocketchip.subsystem.ExtMem
import sifive.blocks.devices.uart.{PeripheryUARTKey, UARTParams}
import sifive.fpgashells.shell.xilinx.VC7071GDDRSize
import testchipip.SerialTLKey
import sifive.fpgashells.shell.DesignKey
import chipyard.ChipTop
import freechips.rocketchip.config.Parameters
import sifive.blocks.devices.spi.{PeripherySPIKey, SPIParams}

class WithDefaultPeripherals extends Config((site, here, up) => {
  case PeripheryUARTKey => List(UARTParams(address = BigInt(0x64000000L)))
  case PeripherySPIKey => List(SPIParams(rAddress = BigInt(0x64001000L)))
})

class WithSystemModifications extends Config((site, here, up) => {
  case DTSTimebase => BigInt((1e6).toLong)
  case ExtMem => up(ExtMem, site).map(x => x.copy(master = x.master.copy(size = site(VC7071GDDRSize)))) // set extmem to DDR size
  case SerialTLKey => None // remove serialized tl port
})

class WithVCU707ChipTop extends Config((site, here, up) => {
  case DesignKey => (p: Parameters) => new ChipTop()(p).suggestName("chiptop")
})

// DOC include start: AbstractVC707 and Rocket
class WithVC707Tweaks extends Config(
  // harness binders
    new WithUART ++
    new WithDDRMem ++
    new WithSPISDCard ++
    // io binders
    new WithUARTIOPassthrough ++
    new WithTLIOPassthrough ++
    new WithSPIIOPassthrough ++
    // other configuration
    new WithDefaultPeripherals ++
    new chipyard.config.WithTLBackingMemory ++ // use TL backing memory
    new WithSystemModifications ++ // setup busses, use sdboot bootrom, setup ext. mem. size
    new chipyard.config.WithNoDebug ++ // remove debug module
    new freechips.rocketchip.subsystem.WithoutTLMonitors ++
    new freechips.rocketchip.subsystem.WithNMemoryChannels(1) ++
    new WithVCU707ChipTop ++
    new WithFPGAFrequency(100) // default 100MHz freq
)

class RocketVC707Config extends Config(
  new WithVC707Tweaks ++
    new chipyard.RocketConfig)

class WithFPGAFrequency(fMHz: Double) extends Config(
  new chipyard.config.WithPeripheryBusFrequency(fMHz) ++ // assumes using PBUS as default freq.
    new chipyard.config.WithMemoryBusFrequency(fMHz)
)

class WithFPGAFreq25MHz extends WithFPGAFrequency(25)
class WithFPGAFreq50MHz extends WithFPGAFrequency(50)
class WithFPGAFreq75MHz extends WithFPGAFrequency(75)
class WithFPGAFreq100MHz extends WithFPGAFrequency(100)

